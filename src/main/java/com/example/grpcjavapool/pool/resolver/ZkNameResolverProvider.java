package com.example.grpcjavapool.pool.resolver;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;

public class ZkNameResolverProvider extends NameResolverProvider {
    @Override
    protected boolean isAvailable() {
        return true; // 这个Provider是否可用
    }

    @Override
    protected int priority() {
        return 5; // 默认优先级为5(0~10)
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return new ZkNameResolver(targetUri);
    }

    @Override
    public String getDefaultScheme() {
        return "zk";
    }
}
