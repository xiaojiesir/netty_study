package com.xiaojie.netty.netty.dubboRPC.provider;

import com.xiaojie.netty.netty.dubboRPC.publicinterface.HelloService;

public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String msg) {
        System.out.println("收到客户端消息:" + msg);
        if (msg != null) {
            return "您好客户端,我已收到你的消息[" + msg + "]";
        }
        return "您好服务端,我未收到你的消息";
    }
}
