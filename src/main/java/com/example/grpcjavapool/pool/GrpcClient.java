package com.example.grpcjavapool.pool;

import com.example.grpcjavapool.gen.*;
import io.grpc.*;

import java.util.concurrent.TimeUnit;

/**
 * gRPC客户端,每个客户端都封装了channel和stub,实现复用
 */
public class GrpcClient {

    // 在gRPC官网中《最佳性能实践》章节建议复用channel和stub
    private ManagedChannel channel; // 定义一个channel
    private GreeterGrpc.GreeterBlockingStub blockingStub; // 定义一个阻塞式同步存根

    public GrpcClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .keepAliveTime(10, TimeUnit.SECONDS) // 设置channel保活
                .usePlaintext()
                .build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel).withCompression("gzip");
        // this.channel = null;
        // this.blockingStub = null;
    }

    // 定义客户端方法
    public void greet(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            // channel = ManagedChannelBuilder.forAddress("127.0.0.1", 9099).usePlaintext().build();
            // blockingStub = GreeterGrpc.newBlockingStub(channel).withCompression("gzip");
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
            if (channel.isShutdown() || channel.isTerminated()) {
                System.out.println("the channel has shutdown or terminated: " + channel.toString());
            }
        } catch (InterruptedException e) {
            System.out.println("channel shutdown exception: " + e.getMessage());
        }
    }

    public boolean ping() {
        System.out.println("grpcClient try to ping...");
        Ping pingRequest = Ping.newBuilder().setPing("PING").build();
        Pong pongResponse = null;
        try {
            // 设置ping请求的超时时间，要求客户端在1s之内收到PONG
            pongResponse = blockingStub
                    .withDeadlineAfter(1, TimeUnit.SECONDS).pingPong(pingRequest);
        } catch (Exception e) {
            System.out.println("grpcClient call ping exception");
        }
        // 如果客户端收到PONG，并且channel状态是ready，说明当前客户端能ping通
        return "PONG".equals(pongResponse.getPong())
                && this.channel.getState(true) == ConnectivityState.READY;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public GreeterGrpc.GreeterBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public void setBlockingStub(GreeterGrpc.GreeterBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    @Override
    public String toString() {
        return "GrpcClient{" +
                "channel=" + channel +
                ", blockingStub=" + blockingStub +
                '}';
    }


}
