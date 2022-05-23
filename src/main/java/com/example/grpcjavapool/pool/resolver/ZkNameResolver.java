package com.example.grpcjavapool.pool.resolver;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZkNameResolver extends NameResolver implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(ZkNameResolver.class);
    private static final String ZK_PATH = "/grpc-zk/grpc_services";
    private static final int ZK_CONN_TIMEOUT = 3000;

    private Listener listener;
    private URI zkUri;
    private ZooKeeper zk;

    public ZkNameResolver(URI targetUri) {
        this.zkUri = targetUri;
    }

    @Override
    public String getServiceAuthority() {
        return zkUri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        // 并发计数器，用来保证多线程中只有一个线程成功连接zookeeper server
        final CountDownLatch latch = new CountDownLatch(1);
        String zkAddress = zkUri.getHost() + ":" + zkUri.getPort();
        logger.info("connect to zookeeper server {}", zkAddress);
        try {
            this.zk = new ZooKeeper(zkAddress, ZK_CONN_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    // watch监听到连接成功，计数器减1
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
        } catch (Exception e) {
            logger.error("connect to zookeeper server fail:{}", e.getMessage());
            System.exit(1); // JVM退出，为了保证client从zk中获取可用的grpcServer服务
        }
        try {
            // 等待并发计数器减为0，意味着有一个线程成功连接上zk
            latch.await();
        } catch (Exception e) {
            logger.error("CountDownLatch exception:{}", e.getMessage());
            System.exit(1);
        }
        try {
            Stat stat = zk.exists(ZK_PATH, true);
            if (stat == null) {
                logger.error("{} node is not exists", ZK_PATH);
                return;
            }
            logger.info("{} node is exists", ZK_PATH);
        } catch (Exception e) {
            logger.error("exists method exception:{}", e.getMessage());
        }
        try {
            List<String> children = zk.getChildren(ZK_PATH, this);
            addServersToListener(children);
        } catch (Exception e) {

        }


    }

    /**
     * 把 ZK_PATH 的所有子节点作为gRPC的节点地址，客户端会从所有节点中选择进行负载均衡
     *
     * @param servers
     */
    private void addServersToListener(List<String> servers) {
        logger.info("grpc all server node:{}", servers);
        ArrayList<EquivalentAddressGroup> addressGroups = new ArrayList<>();
        for (String server : servers) {
            logger.info("server info:{}", server);
            ArrayList<SocketAddress> socketAddresses = new ArrayList<>();
            String[] address = server.split(":");
            String grpcServerIp = address[0];
            int grpcServerPort = Integer.parseInt(address[1]);
            socketAddresses.add(new InetSocketAddress(grpcServerIp, grpcServerPort));
            addressGroups.add(new EquivalentAddressGroup(socketAddresses));
        }
        if (addressGroups.size() > 0) {
            // 把grpc server服务地址注册到gRPC负载均衡上
            listener.onAddresses(addressGroups, Attributes.EMPTY);
        } else {
            logger.error("not found available grpc server, listener...");
        }
    }

    @Override
    public void shutdown() {
        try {
            zk.close();
        } catch (Exception e) {
            logger.error("zookeeper shutdown exception:{}", e.getMessage());
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.None) {
            logger.info("zookeeper connection expired");
        } else {
            try {
                //事件监听器监听 ZK_PATH 的创建 删除 修改等事件
                //动态的更新zk上的节点信息
                List<String> children = zk.getChildren(ZK_PATH, false);
                addServersToListener(children);
                zk.getChildren(ZK_PATH, true);
            } catch (Exception e) {
                logger.error("watchedEvent add servers to listener exception:{}", e.getMessage());
            }
        }
    }
}
