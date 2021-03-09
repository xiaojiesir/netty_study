package com.xiaojie.netty.netty.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GroupChatServerHandler extends SimpleChannelInboundHandler {

    //定义一个channel组,管理所有的channel(GlobalEventExecutor.INSTANCE 全局事件处理器,是一个单例)
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //handlerAdded表示连接建立,一旦连接,第一个执行
    //将当前channel加入到channelGroup
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        //将该客户加入聊天的信息推送给其他在线客户端
        channelGroup.writeAndFlush("[客户端]" + ctx.channel().remoteAddress() + "在" + sdf.format(new Date()) + "加入聊天室");
        //将该channel加入channelGroup
        channelGroup.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        //将该客户加入聊天的信息推送给其他在线客户端
        channelGroup.writeAndFlush("[客户端]" + ctx.channel().remoteAddress() + "在" + sdf.format(new Date()) + "离开聊天室");
        System.out.println("channelGroup大小:" + channelGroup.size());
        //不需要执行以下方法,执行handlerRemoved会自动remove
        //channelGroup.remove(ctx.channel());
    }

    //表示channel处于活动状态.提示xx上线
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "上线了");
    }

    //表示channel处于非活动状态.提示xx下线
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "下线了");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object o) throws Exception {
        Channel channel = ctx.channel();
        for (Channel ch : channelGroup) {
            if (channel != ch) {
                ch.writeAndFlush("[客户端]" + channel.remoteAddress() + "在" + sdf.format(new Date()) + "发送了消息" + o);
            } else {
                ch.writeAndFlush("自己" + "在" + sdf.format(new Date()) + "发送了消息" + o);
            }
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
