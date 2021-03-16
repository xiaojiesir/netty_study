package com.xiaojie.netty.handler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;
import java.util.List;

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

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);

        try {
            //用于启动NIO服务端的辅助启动类，可配置参数
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //对应TCP/IP协议listen函数中的backlog参数,用来初始化服务器可连接队列大小.服务端处理请求是顺序处理的,
                    // 同一时间只能处理一个客户端连接,多个客户端来的时候,服务端将不能处理的连接放在队列中等待处理,
                    //SO_BACKLOG指定队列大小
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)//设置保持活动连接状态
                    //.handler(null)//给bossGroup添加handler,childHandler为workerGroup的handler
                    .childHandler(new ChannelInitializer<SocketChannel>() {//创建一个通道测试对象

                        //给pipeline设置处理器
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            socketChannel.pipeline().addLast(new MessageToByteEncoder<Long>() {
                                @Override
                                protected void encode(ChannelHandlerContext channelHandlerContext, Long msg, ByteBuf byteBuf) throws Exception {
                                    System.out.println("服务端的MessageToByteEncoder.encode方法被调用");
                                    System.out.println("服务端发送的msg=" + msg);
                                    byteBuf.writeLong(msg);
                                }
                            });
                            socketChannel.pipeline().addLast(new ByteToMessageDecoder() {
                                @Override
                                protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
                                    if (byteBuf.readableBytes() >= 8) {
                                        list.add(byteBuf.readLong());
                                    }
                                    System.out.println("服务端的ByteToMessageDecoder.decode方法被调用");
                                }
                            });


                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                                @Override
                                public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
                                    System.out.println("服务端收到消息:" + msg);

                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    //
                                    ctx.writeAndFlush(123456L);

                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                }
                            });
                        }
                    });
            //启动服务器并绑定一个端口,生成一个ChannelFuture对象
            ChannelFuture f = b.bind(port).sync();
            //对关闭通道进行监听
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
