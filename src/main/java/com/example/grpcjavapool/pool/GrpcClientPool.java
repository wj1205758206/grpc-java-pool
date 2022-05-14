package com.example.grpcjavapool.pool;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.springframework.stereotype.Component;

/**
 * gRPC连接池
 */
public class GrpcClientPool {

    // 基于 GenericKeyedObjectPool类 实现带key的gRPC连接池
    // 连接池 = 对象工厂 + 连接池配置
    private static GenericKeyedObjectPool<String, GrpcClient> grpcClientPool = null;
    private static GenericKeyedObjectPoolConfig poolConfig;

    // 连接池相关属性配置
    static {
        poolConfig.setMaxTotalPerKey(8);
        poolConfig.setMaxIdlePerKey(8);
        poolConfig.setMinIdlePerKey(0);
        poolConfig.setMaxWaitMillis(-1);
        poolConfig.setLifo(true);
        poolConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 30L);
        poolConfig.setBlockWhenExhausted(true);
        grpcClientPool = new GenericKeyedObjectPool<>(new PooledGrpcClientFactory(), poolConfig);
    }

    /**
     * 获取对象，基于key获取对象，相当于每一个key对应了一个小型池子
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static GrpcClient getGrpcClient(String key) throws Exception {
        if (grpcClientPool == null) {
            initPool();
        }
        return grpcClientPool.borrowObject(key);
    }

    /**
     * 归还对象
     *
     * @param key
     * @param grpcClient
     */
    public static void returnGrpcClient(String key, GrpcClient grpcClient) {
        if (grpcClientPool == null) {
            initPool();
        }
        grpcClientPool.returnObject(key, grpcClient);
    }

    /**
     * 关闭连接池
     */
    public static void close() {
        if (grpcClientPool != null && !grpcClientPool.isClosed()) {
            grpcClientPool.close();
            grpcClientPool = null; // 置空,方便GC
        }
    }

    /**
     * 初始化连接池
     */
    public static synchronized void initPool() {
        if (grpcClientPool != null) {
            return;
        }
        grpcClientPool = new GenericKeyedObjectPool<String, GrpcClient>(new PooledGrpcClientFactory(), poolConfig);
    }

    public static GenericKeyedObjectPool<String, GrpcClient> getGrpcClientPool() {
        return grpcClientPool;
    }
}
