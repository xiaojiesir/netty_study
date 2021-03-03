package com.xiaojie.netty.nio;

import com.xiaojie.netty.bio.TimeServerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NIOServer {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        MultiplexerServer server = new MultiplexerServer(port);
        new Thread(server, "NIO-MultiplexerServer-001").start();
    }
}
