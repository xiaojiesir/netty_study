package com.xiaojie.netty.dubboRPC.consumer;

import com.xiaojie.netty.dubboRPC.provider.HelloServiceImpl;
import com.xiaojie.netty.dubboRPC.provider.NettyServer;
import com.xiaojie.netty.dubboRPC.publicinterface.HelloService;

public class ClientBootstrap {
    private static final String provider = "HelloService#";

    public static void main(String[] args) throws InterruptedException {
        HelloService helloService = (HelloService) new NettyClient().getBean(HelloServiceImpl.class, provider);
        for (; ; ) {
            Thread.sleep(2 * 1000);
            //通过代理对象调用服务提供者的方法(服务)
            String res = helloService.hello("你好 dubbo~");
            System.out.println("调用的结果 res= " + res);
        }
    }
}
