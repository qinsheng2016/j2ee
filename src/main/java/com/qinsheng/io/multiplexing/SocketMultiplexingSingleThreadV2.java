package com.qinsheng.io.multiplexing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: qinsheng
 * @Date: 2020/6/26 13:30
 */
public class SocketMultiplexingSingleThreadV2 {

    private ServerSocketChannel service = null;
    private Selector selector = null;
    private int port = 9090;

    private void initServer() {
        try {
            service = ServerSocketChannel.open();
            service.configureBlocking(false);
            service.bind(new InetSocketAddress(port));

            selector = Selector.open();
            service.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        initServer();
        System.out.println("服务器启动了。。。");

        try {
            while (true) {
                while (selector.select(50) > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();

                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            key.cancel();
                            readHandler(key);
                        } else if (key.isWritable()) {
                            key.cancel();
                            writeHandler(key);
                        }

                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            client.register(selector, SelectionKey.OP_READ, buffer);

            System.out.println("-------------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void readHandler(SelectionKey key) {
        new Thread(()->{
            System.out.println("read handler");
            SocketChannel client = (SocketChannel) key.channel();

            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            int read = 0;
            try {
                while (true) {
                    read = client.read(buffer);
                    System.out.println(Thread.currentThread().getName() + " " + read);

                    if (read > 0) {
                        client.register(key.selector(), SelectionKey.OP_WRITE, buffer);
                    } else if (read == 0) {
                        break;
                    } else {
                        client.close();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void writeHandler(SelectionKey key) {
        new Thread(()->{
            System.out.println("write handler");
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            buffer.flip();
            while (buffer.hasRemaining()) {
                try {
                    client.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.clear();
            key.cancel();
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadV2 service = new SocketMultiplexingSingleThreadV2();
        service.startServer();
    }

}
