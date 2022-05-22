package com.example.grpcjavapool.server;

import com.example.grpcjavapool.server.services.GreeterServiceImpl;
import com.example.grpcjavapool.server.zookeeper.ZkUtil;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
public class GrpcServer {
    private static final Logger logger = LoggerFactory.getLogger(GrpcServer.class);

    @Resource
    ZkUtil zkUtil;

    private int port = 9099;
    private Server server;

    /**
     * 启动gRPC服务端，监听客户端发来的消息
     *
     * @throws IOException
     */
    public void start() throws IOException {
        logger.info("GrpcServer start...");
        try {
            server = ServerBuilder.forPort(port)
                    .addService(new GreeterServiceImpl())
                    .build()
                    .start();
            logger.info("GrpcServer is started");
        } catch (Exception e) {
            logger.error("GrpcServer start fail:{}", e.getMessage());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("GrpcServer shutdown...");
            this.stop();
            logger.info("GrpcServer is shutdown");
        }));
        // grpc server启动成功之后，将服务注册到zk
        register();
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUtilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * 服务注册，zk尝试连接GrpcServer
     */
    public void register() {
        String grpcServerIp = "127.0.0.1";
        int grpcServerPort = 9099;
        boolean connection = zkUtil.connection(grpcServerIp, grpcServerPort);
        if (!connection) {
            logger.error("GrpcServer register fail, grpcServerIp:{}, grpcServerPort:{}",
                    grpcServerIp, grpcServerPort);
            return;
        }
        logger.info("GrpcServer register success, grpcServerIp:{}, grpcServerPort:{}",
                grpcServerIp, grpcServerPort);
    }
}
