package com.xiaojie.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            //测试pipeline是否会随着请求变化
            // (HttpVersion.HTTP_1_0如果不设置长连接,每次都会变化,HttpVersion.HTTP_1_1默认长连接,不会变化)
            System.out.println("pipeline hashcode: " + ctx.pipeline().hashCode() + "HttpServerHandler hash: " + this.hashCode());
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer("hello, 我是HTTP服务器".getBytes("utf-8")));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(response.content().readableBytes()));
            //response.headers().set(HttpHeaderNames.CONNECTION, KEEP_ALIVE);
            ctx.write(response);
            ctx.flush();
        }

    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

}
