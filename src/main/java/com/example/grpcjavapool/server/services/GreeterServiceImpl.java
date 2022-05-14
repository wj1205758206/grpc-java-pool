package com.example.grpcjavapool.server.services;

import com.example.grpcjavapool.gen.GreeterGrpc;
import com.example.grpcjavapool.gen.HelloReply;
import com.example.grpcjavapool.gen.HelloRequest;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import io.grpc.stub.StreamObserver;

public class GreeterServiceImpl extends GreeterGrpc.GreeterImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(GreeterServiceImpl.class);

    public GreeterServiceImpl() {
    }

    // 服务端实现客户端定义的方法
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> replyStreamObserver){
        System.out.println("server receive client info: " + request.getName());
        // 可根据具体业务进行处理...

        // 返回给客户端响应结果
        HelloReply response = HelloReply.newBuilder()
                .setMessage("I'm Server, hello! " + request.getName()).build();
        replyStreamObserver.onNext(response);
        replyStreamObserver.onCompleted();
    }
}
