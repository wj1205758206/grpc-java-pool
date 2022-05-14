package com.example.grpcjavapool.server;

import com.example.grpcjavapool.server.services.GreeterServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcServer {

    private int port = 9099;
    private Server server;

    /**
     * 启动gRPC服务端，监听客户端发来的消息
     *
     * @throws IOException
     */
    private void start() throws IOException {
        System.out.println("GrpcServer start...");
        server = ServerBuilder.forPort(port)
                .addService(new GreeterServiceImpl())
                .build()
                .start();
        System.out.println("GrpcServer start success...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("GrpcServer shutdown...");
            this.stop();
            System.out.println("GrpcServer shutdown success");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUtilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final GrpcServer grpcServer = new GrpcServer();
        grpcServer.start();
        grpcServer.blockUtilShutdown();
    }
}
