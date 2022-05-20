package com.example.grpcjavapool.pool;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;

/**
 * gRPC连接池
 */
public class GrpcClientPool {

    // 基于 GenericKeyedObjectPool类 实现带key的gRPC连接池
    // 连接池 = 对象工厂 + 连接池配置
    private GenericKeyedObjectPool<String, GrpcClient> grpcClientPool = null;
    private GenericKeyedObjectPoolConfig poolConfig = null;
    private PooledGrpcClientFactory pooledGrpcClientFactory = null;

    public GrpcClientPool(GenericKeyedObjectPoolConfig poolConfig,
                          PooledGrpcClientFactory pooledGrpcClientFactory) {
        this.poolConfig = poolConfig;
        this.pooledGrpcClientFactory = pooledGrpcClientFactory;
        this.grpcClientPool = new GenericKeyedObjectPool<>(pooledGrpcClientFactory, poolConfig);
    }


    /**
     * 获取对象，基于key获取对象，相当于每一个key对应了一个小型池子
     *
     * @param key
     * @return
     * @throws Exception
     */
    public GrpcClient getGrpcClient(String key) throws Exception {
        if (grpcClientPool == null) {
            initPool();
        }
        GrpcClient grpcClient = null;
        try {
            grpcClient = grpcClientPool.borrowObject(key);
        } catch (Exception e) {
            System.out.println("getGrpcClient exception: " + e.getMessage());
        }
        return grpcClient;
    }

    /**
     * 归还对象
     *
     * @param key
     * @param grpcClient
     */
    public void returnGrpcClient(String key, GrpcClient grpcClient) {
        if (grpcClientPool == null) {
            initPool();
        }
        try {
            grpcClientPool.returnObject(key, grpcClient);
        } catch (Exception e) {
            System.out.println("returnGrpcClient exception: " + e.getMessage());
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        if (grpcClientPool != null && !grpcClientPool.isClosed()) {
            grpcClientPool.close();
            grpcClientPool = null; // 置空,方便GC
        }
    }

    /**
     * 初始化连接池
     */
    public synchronized void initPool() {
        if (grpcClientPool != null) {
            return;
        }
        grpcClientPool = new GenericKeyedObjectPool<String, GrpcClient>(new PooledGrpcClientFactory(), poolConfig);
    }

    public GenericKeyedObjectPool<String, GrpcClient> getGrpcClientPool() {
        return grpcClientPool;
    }

}
