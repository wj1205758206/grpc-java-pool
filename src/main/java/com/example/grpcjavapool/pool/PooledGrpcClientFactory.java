package com.example.grpcjavapool.pool;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * 工厂类，用于 创建/销毁/激活 池化后的客户端对象
 */
public class PooledGrpcClientFactory extends BaseKeyedPooledObjectFactory<String, GrpcClient> {
    private String host;
    private int port;


    public PooledGrpcClientFactory(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public PooledGrpcClientFactory() {
    }

    /**
     * 创建原始的客户端对象GrpcClient
     *
     * @param key
     * @return
     * @throws Exception
     */
    @Override
    public GrpcClient create(String key) throws Exception {
        return new GrpcClient(key, port);
    }

    /**
     * 包装池化create方法创建出来的原始客户端对象
     * pool中是一个个池化后的对象，即 DefaultPooledObject
     * 池化后的GrpcClient对象除了具有channel和stub属性，还有了与连接池相关的一些属性
     * 实际上底层的实现 makeObject = create + warp
     *
     * @param grpcClient
     * @return
     */
    @Override
    public PooledObject<GrpcClient> wrap(GrpcClient grpcClient) {
        return new DefaultPooledObject<>(grpcClient);
    }

    @Override
    public void destroyObject(String key, PooledObject<GrpcClient> p) throws Exception {
        super.destroyObject(key, p);
    }

    @Override
    public boolean validateObject(String key, PooledObject<GrpcClient> p) {
        return super.validateObject(key, p);
    }

    @Override
    public void activateObject(String key, PooledObject<GrpcClient> p) throws Exception {
        super.activateObject(key, p);
    }

    @Override
    public void passivateObject(String key, PooledObject<GrpcClient> p) throws Exception {
        super.passivateObject(key, p);
    }
}
