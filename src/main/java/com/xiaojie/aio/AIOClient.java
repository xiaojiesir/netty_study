package com.xiaojie.aio;

import java.io.IOException;

public class AIOClient {
    public static String ip = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        AsyncClientHandler client = new AsyncClientHandler(ip, port);
        new Thread(client, "AIO-AsyncClientHandler-001").start();
    }
}

