package com.xiaojie.nio;

import java.io.IOException;

public class NIOClient {
    public static String ip = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        ClientHandle client = new ClientHandle(ip, port);
        new Thread(client, "NIO-ClientHandle-001").start();
    }
}

