package com.example.grpcjavapool.pool;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;


public class Test {
    public static GenericKeyedObjectPool<String, GrpcClient> getClientPool() {
        PooledGrpcClientFactory pooledGrpcClientFactory = new PooledGrpcClientFactory("127.0.0.1", 9099);
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setMaxIdlePerKey(10);
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxTotalPerKey(10);
        poolConfig.setLifo(true);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestOnCreate(true);
        GenericKeyedObjectPool<String, GrpcClient> pool = new GenericKeyedObjectPool<>(pooledGrpcClientFactory, poolConfig);
        return pool;
    }

    public static void main(String[] args) throws InterruptedException {
        GenericKeyedObjectPool<String, GrpcClient> clientPool = getClientPool();
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
                            GrpcClient grpcClient = clientPool.borrowObject(key);
                            grpcClient.greet("hello, " + key
                                    + "! I'm [" + Thread.currentThread().getName() + "]-"
                                    + "[client-" + i + "]");
                            System.out.println("[grpcClient]:" + grpcClient.toString());
                            System.out.println("[pool info] "
                                    + "getNumIdle:" + clientPool.getNumIdle(key)
                                    + "  getNumActive" + clientPool.getNumActive(key));
                            clientPool.returnObject(key, grpcClient);

                        }
                        // Thread.sleep(5000L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
//            if(i%2==0) {
//                Thread.sleep(5000L);
//            }
        }
        long t2 = System.currentTimeMillis();

        System.out.println("~~~~~~~~~~~~~ " + (t2 - t1));
    }

}
