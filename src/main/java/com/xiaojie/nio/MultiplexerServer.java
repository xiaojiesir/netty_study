package com.xiaojie.nio;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerServer implements Runnable {
    private Selector selector;
    private ServerSocketChannel channel;
    private volatile boolean stop;

    public MultiplexerServer(int port) {
        try {
            //创建多路复用器
            selector = Selector.open();
            //创建通道
            channel = ServerSocketChannel.open();
            //将ServerSocketChannel设置为异步非阻塞模式
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(port), 1024);
            //将通道注册到selector,监听SelectionKey.OP_ACCEPT操作位
            channel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The NIO_Server is start in port:" + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void stop() {
        this.stop = true;
    }

    public void run() {
        while (!stop) {
            try {
                //每隔1s唤醒一次selector,
                //也有无参函数:当有处于就绪状态的channel时,selector将返回该channel的selectionKeys集合
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //多路复用关闭,注册在上面的channel和pipe都会自动注册并关闭
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            //处理新接入的请求消息
            if (key.isAcceptable()) {
                //accept the new connection
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                //接收客户端的连接请求并创建SocketChannel实例
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ);
                //到这里,相当于完成TCP的三次握手,TCP物理链路正式建立
            }
            if (key.isReadable()) {
                //accept the new connection
                SocketChannel sc = (SocketChannel) key.channel();
                //开辟1MB的缓冲区
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                //SocketChannel已设置为非阻塞模式,read操作是非阻塞的
                int readBytes = sc.read(readBuffer);
                //返回值大于0:读到字节,对字节进行编解码
                if (readBytes > 0) {
                    //将缓冲区当前的limit设置为position,position设置为0,用于后续对缓冲区的读取操作
                    readBuffer.flip();
                    //根据缓冲区可读字节个数创建字节数组
                    byte[] bytes = new byte[readBuffer.remaining()];
                    //将缓冲区可读的字节数组复制到新创建的字节数组中
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("the server accept:" + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                            new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(sc, currentTime);
                }
                //返回值小于0,链路已经关闭,需要关闭SocketChannel,释放资源
                else if (readBytes < 0) {
                    key.cancel();
                    sc.close();
                } else {

                }
            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
            //由于SocketChannel是异步非阻塞,它不能保证一次能够吧需要发送的字节数组发送完,会出现"写半包"问题,
            // 我们需要注册写操作,不断轮询selector将没有发送完的ByteBuffer发送完毕,
            // 然后通过ByteBuffer的hasRemain方法判断消息是否发送完成
        }
    }
}
