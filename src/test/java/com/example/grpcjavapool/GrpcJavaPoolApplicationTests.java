package com.example.grpcjavapool;

import com.example.grpcjavapool.server.zookeeper.CustomWatcher;
import com.example.grpcjavapool.server.zookeeper.ZkUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class GrpcJavaPoolApplicationTests {
    @Resource
    ZkUtil zkUtil;


    @Test
    public void test(){
        String data = zkUtil.getData("/test", new CustomWatcher());
        System.out.println(data);
    }

}
