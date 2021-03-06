package com.xiaojie.netty;

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
        //配置服务端的NIO线程组 bossGroup用于服务端接收客户端的连接,workerGroup用于SocketChannel的网络读写.
        //NioEventLoopGroup相当于一个事件循环组，含有多个事件循环
        //NioEventLoop是一个事件循环，一个不断循环的执行处理任务的线程，
        //每个NioEventLoop都有一个selector，用于监听绑定在其上的socket的网络通讯
        //所以NioEventLoopGroup是有多个线程的,

        //bossGroup的NioEventLoop循环执行的步骤：
        // 1.轮询accept事件，
        // 2.处理accept事件，与client建立连接，生成NioServerSocketChannel，并将其注册到workerGroup的NioEventLoop上的selector
        // 3.处理任务队列的任务，即runAllTasks
        //workerGroup的NioEventLoop循环执行的步骤：
        // 1.轮询read，write事件，
        // 2.处理NioServerSocketChannel上的io事件，即read，write事件，
        // 3.处理任务队列的任务，即runAllTasks
        //workerGroup的NioEventLoop处理业务时，会使用pipeline（管道）
        // pipeline中包含了channel，可以通过pipeline获取到对应通道，pipeline还维护了处理器
        // bossGroup与workerGroup含有的子线程(NioEventLoop)个数默认为cpu核数*2
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
                            //新增两个解码器,否则会发生粘包和拆包问题
                            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                private int count;

                                @Override
                                public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
                                    //异步处理,不会阻塞
                                    ctx.channel().eventLoop().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(10000);
                                                ctx.writeAndFlush(Unpooled.copiedBuffer(("操作了十秒" + System.getProperty("line.separator")).getBytes()));
                                            } catch (Exception e) {
                                                e.printStackTrace();

                                            }

                                        }
                                    });


                                    System.out.println("server:" + ctx);
                                    System.out.println("服务器线程:" + Thread.currentThread().getName());
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

                                    ctx.writeAndFlush(resp);
                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    //将消息发送队列中的消息写入socketChannel中发送给对方
                                    //为了防止频繁的唤醒Selector进行消息发送,Netty的write方法并不是直接将消息写入SocketChannel中
                                    //调用write方法只是将待发送的消息放到发送缓存数组中,在通过flush方法,
                                    // 将发送缓冲区的消息全部写到SocketChannel
                                    //如果没有在channelRead方法执行ctx.write,就需要执行writeAndFlush()方法
                                    //ctx.writeAndFlush(Unpooled.copiedBuffer("writeAndFlush()".getBytes()));

                                    ctx.flush();
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
