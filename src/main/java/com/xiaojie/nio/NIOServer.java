package com.xiaojie.nio;

import java.io.IOException;

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
