package com.xiaojie.netty.netty.dubboRPC.publicinterface;

/**
 * 公共接口,服务消费方和服务提供方共同使用
 */
public interface HelloService {
    String hello(String msg);
}
