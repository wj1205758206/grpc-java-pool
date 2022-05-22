package com.example.grpcjavapool.server.zookeeper;

import com.sun.xml.internal.ws.util.StringUtils;
import io.netty.util.internal.StringUtil;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.imps.CuratorFrameworkImpl;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class ZkUtil {
    private static final Logger logger = LoggerFactory.getLogger(ZkUtil.class);

    private CuratorFramework zkClient;

    @Resource
    ZkConfig zkConfig;

    @PostConstruct
    public void init() {
        try {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry
                    (zkConfig.getBaseSleepTimeMs(), zkConfig.getMaxRetries());
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
            builder.connectString(zkConfig.getServer())
                    .retryPolicy(retryPolicy)
                    .sessionTimeoutMs(zkConfig.getSessionTimeoutMs())
                    .connectionTimeoutMs(zkConfig.getConnectionTimeoutMs())
                    .namespace(zkConfig.getNamespace());
//            if (StringUtil.isNullOrEmpty(zkConfig.getDigest())) {
//                builder.authorization("digest", zkConfig.getDigest().getBytes(StandardCharsets.UTF_8));
//                builder.aclProvider(new ACLProvider() {
//                    @Override
//                    public List<ACL> getDefaultAcl() {
//                        return ZooDefs.Ids.CREATOR_ALL_ACL;
//                    }
//
//                    @Override
//                    public List<ACL> getAclForPath(String s) {
//                        return ZooDefs.Ids.CREATOR_ALL_ACL;
//                    }
//                });
//            }
            zkClient = builder.build();
            zkClient.start();

            zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                    if (connectionState == ConnectionState.LOST) {
                        logger.info("[zkClient]: lost session with zookeeper...");
                    } else if (connectionState == ConnectionState.CONNECTED) {
                        logger.info("[zkClient]: connected with zookeeper...");
                    } else if (connectionState == ConnectionState.RECONNECTED) {
                        logger.info("[zkClient]: reconnected with zookeeper...");
                    }
                }
            });
        } catch (Exception e) {
            logger.error("[zkClient]: init exception:{}", e);
        }
    }

    public CuratorFramework getZkClient() {
        return zkClient;
    }

    public void close() {
        if (zkClient != null) {
            zkClient.close();
        }
    }

    /**
     * 创建持久化节点
     *
     * @param path
     * @param data
     * @return
     */
    public boolean createPersistentNode(String path, String data) {
        try {
            zkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path, data.getBytes(StandardCharsets.UTF_8));
            logger.info("create persistent node, path:{}, data:{}", path, data);
//            zkClient.create(path, // 创建节点的路径
//                    data.getBytes(StandardCharsets.UTF_8), // 节点数据，UTF-8编码
//                    ZooDefs.Ids.OPEN_ACL_UNSAFE, // 节点权限 完全open
//                    CreateMode.PERSISTENT); // 客户端断开连接后，节点数据持久化在磁盘上，不会被删除
            return true;
        } catch (Exception e) {
            logger.error("create persistent node exception, path:{}, data:{}, exception:{}",
                    path, data, e.getMessage());
            return false;
        }
    }

    /**
     * 创建临时节点
     *
     * @param path
     * @param data
     * @return
     */
    public boolean createEphemeralNode(String path, String data) {
        try {
            zkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(path, data.getBytes(StandardCharsets.UTF_8));
//            zkClient.create(path, // 创建节点的路径
//                    data.getBytes(StandardCharsets.UTF_8), // 节点数据，UTF-8编码
//                    ZooDefs.Ids.OPEN_ACL_UNSAFE, // 节点权限 完全open
//                    CreateMode.EPHEMERAL); // 客户端断开连接后，节点将被删除
            logger.info("create ephemeral node, path:{}, data:{}", path, data);
            return true;
        } catch (Exception e) {
            logger.error("create ephemeral node exception, path:{}, data:{}, exception:{}",
                    path, data, e.getMessage());
            return false;
        }
    }

    /**
     * 创建自定义节点
     *
     * @param path
     * @param data
     * @param acl
     * @param createMode
     * @return
     */
    public boolean createCustomNode(String path, String data, List<ACL> acl, CreateMode createMode) {
        try {
            zkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .withACL(acl)
                    .forPath(path, data.getBytes(StandardCharsets.UTF_8));
            logger.info("create custom node, path:{}, data:{}", path, data);
//            zkClient.create(path, data.getBytes(StandardCharsets.UTF_8), acl, createMode);
            return true;
        } catch (Exception e) {
            logger.error("create custom node exception, path:{}, data:{}, exception:{}",
                    path, data, e.getMessage());
            return false;
        }
    }

    /**
     * 修改节点
     *
     * @param path
     * @param data
     * @return
     */
    public boolean updateNode(String path, String data) {
        try {
            Stat stat = zkClient.checkExists().forPath(path);
            if (null == stat) {
                logger.error("{} node is not exists", path);
                return false;
            }
            String value = getData(path, new CustomWatcher());
            zkClient.setData().withVersion(stat.getAversion()).forPath(path, value.getBytes(StandardCharsets.UTF_8));
            logger.info("{} node data is updated. old data:{}, new data:{}", path, value, data);
            // version:-1 表示zk服务器基于最新的数据进行更新
//            zkClient.setData(path, data.getBytes(StandardCharsets.UTF_8), -1);
            return true;
        } catch (Exception e) {
            logger.error("update node exception, path:{}, data:{}, exception:{}",
                    path, data, e.getMessage());
            return false;
        }
    }

    /**
     * 删除节点
     *
     * @param path
     * @return
     */
    public boolean deleteNode(String path) {
        try {
            zkClient.delete().deletingChildrenIfNeeded().forPath(path);
            logger.info("{} node is deleted", path);
            // version:-1 表示忽略版本检查
//            zkClient.delete(path, -1);
            return true;
        } catch (Exception e) {
            logger.error("delete node exception, path:{}, exception:{}", path, e.getMessage());
            return false;
        }
    }

    /**
     * 判断节点是否存在
     *
     * @param path
     * @return
     */
    public Stat exists(String path) {
        try {
            return zkClient.checkExists().forPath(path);
        } catch (Exception e) {
            logger.error("exists exception, path:{}, exception:{}", path, e.getMessage());
            return null;
        }
    }

//    /**
//     * 判断节点是否存在，并设置监听事件(创建/删除/更新)
//     *
//     * @param path
//     * @param watcher
//     * @return
//     */
//    public Stat exists(String path, Watcher watcher) {
//        try {
//            return zkClient.newWatcherRemoveCuratorFramework().
////            return zkClient.exists(path, watcher);
//        } catch (Exception e) {
//            logger.error("exists exception, path:{}, exception:{}", path, e.getMessage());
//            return null;
//        }
//    }

    /**
     * 获取当前节点的子节点(不包含孙子节点)
     *
     * @param path
     * @return
     */
    public List<String> getChildren(String path) {
        try {
            return zkClient.getChildren().forPath(path);
//            return zkClient.getChildren(path, false);
        } catch (Exception e) {
            logger.error("get children exception, path:{}, exception:{}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取节点的data
     *
     * @param path
     * @param watcher
     * @return
     */
    public String getData(String path, Watcher watcher) {
        Stat stat = null;
        byte[] data = null;
        try {
            stat = new Stat();
            data = zkClient.getData().usingWatcher(watcher).forPath(path);
//            data = zkClient.getData(path, watcher, stat);
            return new String(data);
        } catch (Exception e) {
            logger.error("get data exception, path:{}, data:{}, exception:{}",
                    path, data, e.getMessage());
            return null;
        }
    }

    public boolean connection(String grpcServerIp, int grpcServerPort) {
        String path = "/grpc_services";
        Stat stat = null;
        String currTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        try {
            stat = exists(path);
            if (stat == null) {
                createPersistentNode(path, currTime);
            }
        } catch (Exception e) {
            logger.error("{} node create exception:{}", path, e.getMessage());
        }
        String grpcServerAddr = path + "/" + grpcServerIp + ":" + grpcServerPort;
        try {
            stat = exists(grpcServerAddr);
            if (stat == null) {
                try {
                    createEphemeralNode(grpcServerAddr, currTime);
                } catch (Exception e) {
                    logger.error("create grpcServerAddr {} node exception:{}", grpcServerAddr, e.getMessage());
                    return false;
                }
            }
            try {
                updateNode(grpcServerAddr, currTime);
            } catch (Exception e) {
                logger.error("update grpcServerAddr {} node exception:{}", grpcServerAddr, e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("add grpcServerAddr {} node exception:{}", grpcServerAddr, e.getMessage());
            return false;
        }
        return true;
    }
}
