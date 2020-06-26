package com.qinsheng.io.multiplexing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: qinsheng
 * @Date: 2020/6/25 23:43
 */
public class SocketMultiplexingSingleThreadV1 {

    private ServerSocketChannel server = null;
    private Selector selector = null;
    int port = 9090;

    public void initServer() {
        try {
            // 三步曲
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port)); // bind & listen

            // 如果是在epoll模型下，这里的open就会在内核中调用epoll_create，产生文件描述符fd3
            // 在java中，对三种多路复用器，都是使用的Selection.open()打开，默认是Epoll，可以-D修改.
            selector = Selector.open();

            // server约等于listen状态的fd4，这里哪来的fd4，需要再看
            // 如果是select, poll，这里会在jvm中开辟一个数组，把fd4放进去，这里是native方法实现的
            // 如果是epoll，这里就调用的是内核中的epoll_ctl(fd3, ADD, fd4)
            server.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。");
        try {
            while (true) {
                Set<SelectionKey> keys = selector.keys();
                System.out.println("size: " + keys.size());

                // 调用多路复用器（select，poll or epoll epoll_wait）
                // 如果是select，poll，其实是内核中的select(fd4),poll(fd4);
                // 如果是epoll，这里调用的就是内核中的epoll_wait方法
                while (selector.select(500) > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();  // 返回所有的fd集合

                    // 取得所有的fd集合后，一个一个去处理，所以是同步的。
                    // 如果是NIO，这里需要对每个fd进行系统调用，在使用多路复用器后，只需要一次select方法就能得到具体的哪些需要R/W了。
                    Iterator<SelectionKey> iterable = selectionKeys.iterator();
                    while (iterable.hasNext()) {
                        SelectionKey key = iterable.next();
                        iterable.remove();

                        if(key.isAcceptable()) {
                            // 这里很重要，这里是去接受一个新的连接了，accept 应该接收连接，并返回一个新连接的FD
                            // 在select，poll中，因为在内核中没有空间，所以会在jvm中，把新的fd和之前的fd4的那个listen放在一起
                            // 在epoll中，则是通过epoll_ctl把新的客户端连接fd注册到内核空间，就是红黑树fd3中。
                            acceptHandler(key);
                        } else if(key.isReadable()) {
                            // 在当前线程，这个方法可能是阻塞的，引入IO Thread
                            // redis是不是用了epoll，是不是有个io threads的概念，是不是单线程的
                            // tomcat 8，9 异步的处理方式，实现IO和处理上的解耦
                            readHandler(key);
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();   // 目的是调用accept客户端，fd7
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);

            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer)key.attachment();
        buffer.clear();
        int read = 0;

        try {
            while (true) {
                read = client.read(buffer);
                if(read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    // 对方close断开了
                    // close和不做close 在关闭后可以通过netstat -natp查看不同的连接
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadV1 service = new SocketMultiplexingSingleThreadV1();
        service.start();
    }

}
