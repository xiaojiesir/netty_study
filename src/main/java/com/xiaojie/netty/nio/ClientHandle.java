package com.xiaojie.netty.nio;

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

public class ClientHandle implements Runnable {

    private String host;
    private int port;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;

    public ClientHandle(String host, int port) {
        this.host = host == null ? "127.0.0.1" : host;
        this.port = port;
        try {
            //创建多路复用器
            selector = Selector.open();
            //创建通道
            socketChannel = SocketChannel.open();
            //将ServerSocketChannel设置为异步非阻塞模式
            socketChannel.configureBlocking(false);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() {

        try {
            //发送连接请求
            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

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
            SocketChannel sc = (SocketChannel) key.channel();
            //是否处在连接状态
            if (key.isConnectable()) {
                if (sc.finishConnect()) {
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                    doWrite(sc);
                } else {
                    System.exit(1);
                }
            }

            if (key.isReadable()) {
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
                    System.out.println("the client accept:" + body);
                    this.stop = true;
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

    private void doConnect() throws IOException {

        //如果连接成功,则注册到多路复用器上,发送请求消息,读应答
        if (socketChannel.connect(new InetSocketAddress(host, port))) {
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
        }
        //没有连接成功,服务端没有返回TCP握手应答消息,注册连接操作
        else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel socketChannel) throws IOException {
        String message = "QUERY TIME ORDER";
        byte[] bytes = message.getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
        writeBuffer.put(bytes);
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            System.out.println("send message success:" + message);
        }
    }
}
