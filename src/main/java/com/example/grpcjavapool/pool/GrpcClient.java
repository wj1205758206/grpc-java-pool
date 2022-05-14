package com.example.grpcjavapool.pool;

import com.example.grpcjavapool.gen.GreeterGrpc;
import com.example.grpcjavapool.gen.HelloReply;
import com.example.grpcjavapool.gen.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

/**
 * gRPC客户端,每个客户端都封装了channel和stub,实现复用
 */
public class GrpcClient {

    // 在gRPC官网中《最佳性能实践》章节建议复用channel和stub
    private final ManagedChannel channel; // 定义一个channel
    private GreeterGrpc.GreeterBlockingStub blockingStub; // 定义一个阻塞式同步存根

    public GrpcClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel).withCompression("gzip");
    }

    // 定义客户端方法
    public void greet(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            System.out.println("gRPC call sayHello fail: " + e.getMessage());
            return;
        }
        System.out.println("gRPC call sayHello success, response: " + response.getMessage());
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("channel shutdown exception: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "GrpcClient{" +
                "channel=" + channel +
                ", blockingStub=" + blockingStub +
                '}';
    }
}
