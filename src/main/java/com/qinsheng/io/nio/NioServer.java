package com.qinsheng.io.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * @Author: qinsheng
 * @Date: 2020/6/26 00:17
 */
public class NioServer {

    public static void main(String[] args) throws Exception{

        LinkedList<SocketChannel> clients = new LinkedList<>();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(9090));
        ssc.configureBlocking(false);

        while (true) {
            Thread.sleep(1000);
            // 这里是和BIO的主要区别，BIO会一直阻塞，直到有连接进来，NIO中没有连接，返回-1
            // 如果有连接进来，返回这个客户端的fd5
            SocketChannel client = ssc.accept();    // 这里是非阻塞，如果没有连接进来，内核返回-1，这里返回null
            if (client == null) {
                System.out.println("null......");
            } else {
                client.configureBlocking(false);
                int port = client.socket().getPort();
                System.out.println("client...port...:" + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocate(4096);

            for (SocketChannel c : clients) {
                int num = c.read(buffer);
                if (num > 0) {
                    buffer.flip();
                    byte[] read = new byte[buffer.limit()];
                    buffer.get(read);

                    String string = new String(read);
                    System.out.println(c.socket().getPort() + " : " + string);
                    buffer.clear();
                }
            }
        }

    }

}
