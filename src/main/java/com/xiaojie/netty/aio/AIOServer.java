package com.xiaojie.netty.aio;

import com.xiaojie.netty.nio.MultiplexerServer;

import java.io.IOException;

public class AIOServer {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        AsyncServerHandler server = new AsyncServerHandler(port);
        new Thread(server, "AIO-AsyncServerHandler-001").start();
    }
}
