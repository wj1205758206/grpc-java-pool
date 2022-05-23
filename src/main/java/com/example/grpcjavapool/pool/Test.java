package com.example.grpcjavapool.pool;


import com.example.grpcjavapool.server.zookeeper.ZkUtil;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {
    private static final Logger logger = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) throws InterruptedException {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setMaxIdlePerKey(5);
        poolConfig.setMinIdlePerKey(0);
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxTotalPerKey(10);
        poolConfig.setLifo(true);
        poolConfig.setBlockWhenExhausted(true); // 池满策略设置，true:池满则阻塞
        poolConfig.setMaxWaitMillis(3000); // 阻塞时最大等待时间
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestOnCreate(true);
        poolConfig.setTestWhileIdle(true); // 检查空闲连接是否超时
        poolConfig.setTimeBetweenEvictionRunsMillis(120000L); // 设置驱逐线程执行间隔时间为2min
        poolConfig.setNumTestsPerEvictionRun(2); // 每次驱逐线程清除2个空闲连接
        poolConfig.setMinEvictableIdleTimeMillis(1000L); // 空闲连接被驱逐前能够保留的时间

        PooledGrpcClientFactory pooledGrpcClientFactory = new PooledGrpcClientFactory("127.0.0.1", 9099);
        GrpcClientPool clientPool = new GrpcClientPool(poolConfig, pooledGrpcClientFactory);
        long t1 = System.currentTimeMillis();
        // 创建10个线程，每个线程发送10次请求
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 10; i++) {
                            String key = "127.0.0.1";

                            // 获取key为本机地址对应的客户端对象
                            GrpcClient grpcClient = clientPool.getGrpcClient(key);
                            grpcClient.greet("hello, " + key
                                    + "! I'm [" + Thread.currentThread().getName() + "]-"
                                    + "[client-" + i + "]");
                            logger.info("[grpcClient]:" + grpcClient.toString());
                            logger.info("[pool info] "
                                    + "getNumIdle:" + clientPool.getGrpcClientPool().getNumActive(key)
                                    + "  getNumActive" + clientPool.getGrpcClientPool().getNumActive(key));
                            clientPool.returnGrpcClient(key, grpcClient);

                        }
                        // Thread.sleep(5000L);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
            });
            t.start();


//            if(i%2==0) {
//                Thread.sleep(5000L);
//            }
        }
        long t2 = System.currentTimeMillis();

        logger.info("~~~~~~~~~~~~~ " + (t2 - t1));
    }

}
