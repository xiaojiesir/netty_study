package com.xiaojie.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class GroupChatServer implements Runnable {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final int PORT = 6666;

    public GroupChatServer() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        GroupChatServer groupChatServer = new GroupChatServer();
        new Thread(groupChatServer).start();
    }

    @Override
    public void run() {
        System.out.println("监听接收的线程：" + Thread.currentThread().getName());
        while (true) {
            try {
                int count = selector.select();
                if (count > 0) {
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        if (key.isAcceptable()) {
                            System.out.println("监听Acceptable的线程：" + Thread.currentThread().getName());
                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            SocketChannel socketChannel = serverChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println(socketChannel.getRemoteAddress() + "上线了");
                        } else if (key.isReadable()) {
                            System.out.println("监听Readable的线程：" + Thread.currentThread().getName());
                            accept(key);
                        }
                        keys.remove();
                    }

                }
            } catch (IOException e) {
                System.out.println();
                e.printStackTrace();
            }

        }
    }

    private void accept(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            socketChannel.read(byteBuffer);
            String msg = new String(byteBuffer.array());
            System.out.println("收到消息:" + msg);
            send(msg, socketChannel);
        } catch (IOException e) {
            try {
                System.out.println(socketChannel.getRemoteAddress() + "下线了");
                key.cancel();
                socketChannel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    private void send(String msg, SocketChannel socketChannel) throws IOException {
        for (SelectionKey selectionKey : selector.keys()) {
            ByteBuffer msgBuffer = ByteBuffer.wrap(msg.getBytes());
            SelectableChannel channel = selectionKey.channel();
            if (channel instanceof SocketChannel && channel != socketChannel) {
                SocketChannel send = (SocketChannel) channel;
                send.write(msgBuffer);
                System.out.println("发送给" + send.getRemoteAddress() + ":" + msg);
            }
        }
    }
}
