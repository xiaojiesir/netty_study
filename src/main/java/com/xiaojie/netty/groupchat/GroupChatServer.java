package com.xiaojie.netty.groupchat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


public class GroupChatServer {
    private final int PORT = 6666;


    public static void main(String[] args) {
        new GroupChatServer().run();
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .handler(new LoggingHandler())//在bossGroup增加一个日志处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            //IdleStateHandler:处理空闲状态的处理器,
                            // readerIdleTime:多长时间没有读,就会发送一个心跳检测包检测是否连接
                            // writerIdleTime:多长时间没有写,就会发送一个心跳检测包检测是否连接
                            // allIdleTime:多长时间没有读写,就会发送一个心跳检测包检测是否连接
                            //当IdleStateEvent触发后,传递给管道的下一个handler去处理,
                            // 通过调用下一个handler的userEventTriggered处理IdleStateEvent的读空闲,写空闲,读写空闲
                            channel.pipeline().addLast(new IdleStateHandler(3, 5, 7, TimeUnit.SECONDS));
                            channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    if (evt instanceof IdleStateEvent) {
                                        IdleStateEvent event = (IdleStateEvent) evt;
                                        String eventType = null;
                                        switch (event.state()) {
                                            case READER_IDLE:
                                                eventType = "读空闲";
                                                break;
                                            case WRITER_IDLE:
                                                eventType = "写空闲";
                                                break;
                                            case ALL_IDLE:
                                                eventType = "读写空闲";
                                                break;
                                        }
                                        System.out.println(ctx.channel().remoteAddress() + "超时事件" + eventType);
                                        //如果发生空闲事件,关闭通道
                                        ctx.channel().close();
                                    }
                                }
                            });
                            //向pipeline增加解码器
                            channel.pipeline().addLast("decoder", new StringDecoder());
                            //向pipeline增加编码器
                            channel.pipeline().addLast("encoder", new StringEncoder());
                            channel.pipeline().addLast(new GroupChatServerHandler());

                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind(PORT).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}
