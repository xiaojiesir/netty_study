package com.xiaojie.netty.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.Random;

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
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                //发送的消息条数,测试粘包
                                private int count;

                                //当通道就绪会触发该方法
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    /*StudentPOJO.Student student = StudentPOJO.Student.newBuilder().setId(4)
                                            .setName("小王").build();
                                    ctx.writeAndFlush(student);*/

                                    //随机发送Student对象或者Worker对象
                                    int random = new Random().nextInt(3);
                                    DataInfo.DataMessage message = null;
                                    if (0 == random) {
                                        message = DataInfo.DataMessage.newBuilder().setDataType(DataInfo.DataMessage.DataType.StudentType)
                                                .setStudent(DataInfo.Student.newBuilder().setId(5).setName("小李").build()).build();
                                    } else {
                                        message = DataInfo.DataMessage.newBuilder().setDataType(DataInfo.DataMessage.DataType.WorkerType)
                                                .setWorker(DataInfo.Worker.newBuilder().setAge(5).setName("小赵")).build();
                                    }
                                    ctx.writeAndFlush(message);
                                }

                                //当通道有读取事件时,触发该方法
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    ByteBuf byteBuf = (ByteBuf) msg;
                                    System.out.println("msg:" + byteBuf.toString(CharsetUtil.UTF_8));
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

