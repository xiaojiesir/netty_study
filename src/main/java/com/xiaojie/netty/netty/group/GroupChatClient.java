package com.xiaojie.netty.netty.group;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Scanner;

public class GroupChatClient {

    private final int PORT = 6666;
    private final String HOST = "127.0.0.1";
    private String clientName;


    public static void main(String[] args) {
        GroupChatClient groupChatClient = new GroupChatClient();
        groupChatClient.run();

    }


    public void run() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //向pipeline增加解码器
                            socketChannel.pipeline().addLast("decoder", new StringDecoder());
                            //向pipeline增加编码器
                            socketChannel.pipeline().addLast("encoder", new StringEncoder());
                            socketChannel.pipeline().addLast(new GroupChatClientHandler());
                        }
                    });
            //发起异步连接操作
            ChannelFuture f = b.connect(HOST, PORT).sync();
            Channel channel = f.channel();

            //channel.closeFuture().sync();

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String s = scanner.next();
                channel.writeAndFlush(s);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //释放NIO线程组
            group.shutdownGracefully();
        }
    }
}
