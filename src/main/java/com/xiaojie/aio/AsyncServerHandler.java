package com.xiaojie.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class AsyncServerHandler implements Runnable {

    private int port;
    CountDownLatch latch;
    AsynchronousServerSocketChannel serverSocketChannel;

    public AsyncServerHandler(int port) {
        this.port = port;
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            System.out.println("The AIO_Server is start in port:" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        //完成一组正在执行的操作之前,允许当前的线程一直阻塞,让线程在此阻塞,防止服务端执行完成退出
        latch = new CountDownLatch(1);
        doAccept();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doAccept() {
        serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, AsyncServerHandler>() {

            @Override
            public void completed(AsynchronousSocketChannel result, AsyncServerHandler attachment) {
                //调用accept方法后,如果有新的客户端连接接入,系统将回调我们传入的CompletionHandler实例的completed方法表示新的客户端已经接入成功.
                // 一个AsynchronousServerSocketChannel可以接收成千上万个客户端,所以需要继续调用他的accept方法,接收其他的客户端连接,最终形成一个循环.
                // 每当接收一个客户端连接成功之后,在异步接收新的客户端连接.
                attachment.serverSocketChannel.accept(attachment, this);
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                //第一个byteBuffer:接收缓冲区,用于从异步channel中读取数据包
                //第二个byteBuffer:异步channel携带的附件,通知回调的时候作为入参使用
                //ReadCompletionHandler:接收通知回调的业务handler
                result.read(byteBuffer, byteBuffer, new ReadCompletionHandler(result));
            }

            @Override
            public void failed(Throwable t, AsyncServerHandler attachment) {
                t.printStackTrace();
                attachment.latch.countDown();

            }
        });
    }
}
