package com.xiaojie.netty.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.io.IOException;

public class NettyClient {
    public static String ip = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        new NettyClient().connect(port, "127.0.0.1");
    }

    private void connect(int port, String host) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //新增两个解码器,否则会发生粘包和拆包问题
                            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter() {
                                //发送的消息条数,测试粘包
                                private int count;

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    String response = "QUERY TIME ORDER" + System.getProperty("line.separator");
                                    byte[] bytes = response.getBytes();
                                    ByteBuf message = null;
                                    for (int i = 0; i < 100; i++) {
                                        message = Unpooled.buffer(bytes.length);
                                        message.writeBytes(bytes);
                                        ctx.writeAndFlush(message);
                                    }

                                }

                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                   /* ByteBuf buf = (ByteBuf) msg;
                                    byte[] bytes = new byte[buf.readableBytes()];
                                    //将缓冲区可读的字节数组复制到新创建的字节数组中
                                    buf.readBytes(bytes);
                                    String body = new String(bytes, "UTF-8");*/
                                    //增加解码器后,不需要额外对请求消息进行编码
                                    String body = (String) msg;
                                    System.out.println("the client accept:" + body + "; the count is :" + ++count);

                                }

                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    System.out.println("exception:" + cause.getMessage());
                                    ctx.close();
                                }
                            });
                        }
                    });
            //发起异步连接操作
            ChannelFuture f = b.connect(host, port).sync();
            //等待客户端链路关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //释放NIO线程组
            group.shutdownGracefully();
        }
    }
}

