package com.xiaojie.netty.http;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import org.omg.CORBA.BAD_CONTEXT;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class HttpFileServerHandler extends ChannelInboundHandlerAdapter {
    public HttpFileServerHandler(String url) {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        if (!request.decoderResult().isSuccess()) {
            //TODO
            return;
        }
        if (request.method() != HttpMethod.GET) {
            return;
        }
        final String uri = request.uri();
        final String path = sanitizeUri(uri);
        if (path == null) {
            return;
        }

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            return;
        }
        if (!file.isFile()) {
            return;
        }
        RandomAccessFile randomAccessFile = null;
        //以只读的方式打开文件
        randomAccessFile = new RandomAccessFile(file, "r");

        Long fileLength = randomAccessFile.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        response.headers().set(HttpHeaderNames.CONNECTION, KEEP_ALIVE);
        ctx.write(response);
        ChannelFuture sendFileFuture;
        sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192)
                , ctx.newProgressivePromise());

    }

    private void setContentTypeHeader(HttpResponse response, File file) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/jpeg");
    }

    private void setContentLength(HttpResponse response, Long fileLength) {
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(fileLength));
    }

    private String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException ex) {
                throw new Error();
            }

        }
        uri = uri.replace('/', File.separatorChar);
        if (uri.contains(File.separator + '.')
                || uri.contains('.' + File.separator)
                || uri.startsWith(".") || uri.endsWith(".")) {
            return null;
        }
        return System.getProperty("user.dir") + File.separator + uri;

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
