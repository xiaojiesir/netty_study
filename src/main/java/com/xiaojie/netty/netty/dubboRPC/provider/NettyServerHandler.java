package com.xiaojie.netty.netty.dubboRPC.provider;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        //获取客户端消息,并调用本地服务
        System.out.println("msg:" + msg);
        String message = (String) msg;
        //客户端在调用远程服务,需要指定协议,例如(消息必须以某个字符串开头"HelloService#msg")
        if (message.startsWith("HelloService#")) {
            String resp = new HelloServiceImpl().hello(message.substring(message.lastIndexOf("#")));
            System.out.println("resp:" + resp);
            ctx.writeAndFlush(resp);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
        ctx.close();
    }
}
