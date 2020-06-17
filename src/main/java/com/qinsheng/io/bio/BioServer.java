package com.qinsheng.io.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class BioServer {

    private static final int BACK_LOG = 2;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(9090), BACK_LOG);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                System.in.read(); // 阻塞
                Socket client = serverSocket.accept();

                new Thread(() -> {
                    try {
                        while(true){
                            InputStream in = client.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                            char[] data = new char[1024];
                            int num = reader.read(data);

                            if(num > 0) {
                                System.out.println("client read some data is :" + num + ", val :" + new String(data, 0, num));
                            } else if (num == 0) {
                                System.out.println("client read nothing!");
                            } else {
                                System.out.println("client read -1....");
                                client.close();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
