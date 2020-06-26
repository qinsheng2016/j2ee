package com.qinsheng.io.bio.v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Author: qinsheng
 * @Date: 2020/6/27 00:37
 * 应用版，从应用层面直接撸代码
 * BIO，效率低，并发性也不好，多数的线程都是停着的，线程在CPU里不停切换
 * 应用场景：如果确定了连接数很少，使用BIO也没有问题
 */
public class Server {

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress("127.0.0.1", 9999));
        while (true) {
            // s 代表的就是客户端和服务端的一个连接
            Socket s = ss.accept(); // 阻塞方法，一直等客户端连接


            new Thread(() -> {
                handle(s);
            }).start();
        }
    }

    public static void handle(Socket s) {
        System.out.println("new client : " + s.getPort());
        try {
            byte[] bytes = new byte[1024];
            int len = s.getInputStream().read(bytes);   // 阻塞
            System.out.println(new String(bytes, 0, len));

            s.getOutputStream().write(bytes, 0, len);   // 阻塞
            s.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
