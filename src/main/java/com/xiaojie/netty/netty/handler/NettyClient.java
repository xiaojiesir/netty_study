package com.xiaojie.netty.netty.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.List;

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
                            socketChannel.pipeline().addLast(new MessageToByteEncoder<Long>() {
                                @Override
                                protected void encode(ChannelHandlerContext channelHandlerContext, Long msg, ByteBuf byteBuf) throws Exception {
                                    System.out.println("客户端的MessageToByteEncoder.encode方法被调用");
                                    System.out.println("客户端发送的msg=" + msg);
                                    byteBuf.writeLong(msg);
                                }
                            });
                            socketChannel.pipeline().addLast(new ByteToMessageDecoder() {
                                @Override
                                protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

                                    if (byteBuf.readableBytes() >= 8) {
                                        list.add(byteBuf.readLong());
                                    }
                                    System.out.println("客户端的ByteToMessageDecoder.decode方法被调用");
                                }
                            });
                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                                //当通道就绪会触发该方法
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("客户端发送数据");
                                    //ctx.writeAndFlush(1234L);
                                    //ctx.writeAndFlush(Unpooled.copiedBuffer("abcdabcdabcdabcd", CharsetUtil.UTF_8));
                                    //ctx.writeAndFlush(Unpooled.copiedBuffer("1234567812345678", CharsetUtil.UTF_8));
                                    ctx.writeAndFlush(1234567812345678L);
                                }

                                //当通道有读取事件时,触发该方法
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    System.out.println("客户端收到消息:" + msg);

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

