package com.example.grpcjavapool;

import com.example.grpcjavapool.server.GrpcServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GrpcJavaPoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcJavaPoolApplication.class, args);
    }

}
