package com.xiaojie.netty.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.io.IOException;
import java.util.Date;

public class NettyServer {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        new NettyServer().bind(port);
    }

    private void bind(int port) {
        //配置服务端的NIO线程组 一个用于服务端接收客户端的连接,一个用于SocketChannel的网络读写.
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            //用于启动NIO服务端的辅助启动类
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //新增两个解码器,否则会发生粘包和拆包问题
                            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter() {
                                private int count;

                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    /*ByteBuf buf = (ByteBuf) msg;
                                    byte[] bytes = new byte[buf.readableBytes()];
                                    //将缓冲区可读的字节数组复制到新创建的字节数组中
                                    buf.readBytes(bytes);
                                    String body = new String(bytes, "UTF-8")
                                            .substring(0, bytes.length - System.getProperty("line.separator").length());*/

                                    //增加解码器后,不需要额外对请求消息进行编码
                                    String body = (String) msg;
                                    System.out.println("the server accept:" + body + "; the count is :" + ++count);
                                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                                            new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                                    currentTime = currentTime + System.getProperty("line.separator");
                                    ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());

                                    ctx.write(resp);
                                }

                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    //将消息发送队列中的消息写入socketChannel中发送给对方
                                    //为了防止频繁的唤醒Selector进行消息发送,Netty的write方法并不是直接将消息写入SocketChannel中
                                    //调用write方法只是将待发送的消息放到发送缓存数组中,在通过flush方法,
                                    // 将发送缓冲区的消息全部写到SocketChannel
                                    ctx.flush();
                                }

                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                }
                            });
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
