package com.xiaojie.netty.netty.websocket;

import com.xiaojie.netty.netty.groupchat.GroupChatServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.time.LocalDateTime;

public class WebSocketServer {
    private final int PORT = 7000;


    public static void main(String[] args) {
        new WebSocketServer().run();
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
                            //基于http协议,使用http的编码和解码器
                            channel.pipeline().addLast(new HttpServerCodec());
                            //是以块方式写,添加ChunkedWriteHandler处理器
                            channel.pipeline().addLast(new ChunkedWriteHandler());
                            //http数据在传输过程中是分段,HttpObjectAggregator,就是可以将多个段聚合
                            channel.pipeline().addLast(new HttpObjectAggregator(8192));
                            //websocket数据是以帧(frame)形式传递
                            //浏览器请求时 ws://localhost:7000/hello 表示请求的uri
                            //WebSocketServerProtocolHandler 核心功能将http协议升级为ws协议,保持长连接
                            channel.pipeline().addLast(new WebSocketServerProtocolHandler("/hello"));
                            //处理业务的handler
                            channel.pipeline().addLast(new SimpleChannelInboundHandler<TextWebSocketFrame>() {

                                /**
                                 *
                                 * @param ctx
                                 * @param msg 文本帧
                                 * @throws Exception
                                 */
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
                                    System.out.println("服务器收到的信息:" + msg.text());
                                    ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器当前时间" + LocalDateTime.now() + msg.text()));
                                }

                                //当web客户端连接后,触发该方法
                                public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                                    //id表示唯一值,LongText是唯一的,ShortText不是唯一的
                                    System.out.println("handlerAdded 被调用" + ctx.channel().id().asLongText());
                                    System.out.println("handlerAdded 被调用" + ctx.channel().id().asShortText());
                                }

                                public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("handlerRemoved 被调用" + ctx.channel().id().asLongText());
                                    System.out.println("handlerRemoved 被调用" + ctx.channel().id().asShortText());
                                }

                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    System.out.println("异常信息:" + cause.getMessage());
                                    ctx.close();
                                }
                            });
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
