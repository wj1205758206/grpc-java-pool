package com.example.grpcjavapool;

import com.example.grpcjavapool.pool.GrpcClient;
import com.example.grpcjavapool.pool.GrpcClientPool;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;

import javax.annotation.Resource;

import static com.example.grpcjavapool.pool.GrpcClientPool.getGrpcClient;
import static com.example.grpcjavapool.pool.GrpcClientPool.getGrpcClientPool;

@SpringBootTest
class GrpcJavaPoolApplicationTests {
    @Resource
    GrpcClientPool grpcClientPool;

    @Test
    public void TestGrpcClientPool() {

        // 创建10个线程，每个线程发送10次请求
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 10; i++) {
                            String key = "127.0.0.1";
                            // 获取key为本机地址对应的客户端对象
                            GrpcClient grpcClient = getGrpcClient(key);
                            grpcClient.greet("hello, " + key
                                    + "! I'm [" + Thread.currentThread().getName() + "]-"
                                    + "[client-" + i + "]");
                            System.out.println("[grpcClient]:" + grpcClient.toString());
                            System.out.println("[pool info] "
                                    + "getNumIdle:" + getGrpcClientPool().getNumIdle(key)
                                    + "getNumActive" + getGrpcClientPool().getNumActive(key));
                            GrpcClientPool.returnGrpcClient(key, grpcClient);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }
}
