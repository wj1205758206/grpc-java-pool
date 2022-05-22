package com.example.grpcjavapool;

import com.example.grpcjavapool.server.GrpcServer;
import com.example.grpcjavapool.server.zookeeper.CustomWatcher;
import com.example.grpcjavapool.server.zookeeper.SpringUtil;
import com.example.grpcjavapool.server.zookeeper.ZkUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

import java.io.IOException;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class GrpcJavaPoolApplication {

    public static void main(String[] args) throws InterruptedException, IOException {
        SpringApplication.run(GrpcJavaPoolApplication.class, args);

        ApplicationContext context = SpringUtil.getApplicationContext();
        final GrpcServer grpcServer = context.getBean(GrpcServer.class);
        grpcServer.start();
        grpcServer.blockUtilShutdown();
//        ZkUtil zkUtil = context.getBean(ZkUtil.class);

//        bean.createPersistentNode("/test", "111");
//        System.out.println("<<<<<<" + bean.getData("/test", new CustomWatcher()));
    }
}
