package com.xiaojie.netty.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServerThreadPool {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("The Server is start,port:" + port);
            Socket socket = null;
            ServerHandlerExecutePool pool = new ServerHandlerExecutePool(50, 1000);
            while (true) {
                socket = serverSocket.accept();
                pool.execute(new TimeServerHandler(socket));
            }
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
                System.out.println("The Server is close");
                serverSocket = null;
            }
        }
    }
}
