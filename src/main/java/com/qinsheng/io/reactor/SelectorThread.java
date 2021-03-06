package com.qinsheng.io.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: qinsheng
 * @Date: 2020/6/26 16:34
 * 每个线程对应一个selector
 * 多线程情况下，该主机，该程序的并发客户端被分配到多个selector上
 * 每个客户端，只会绑定到其中一个selector
 * 不会有交互问题
 */
public class SelectorThread implements Runnable{

    Selector selector = null;
    SelectorThreadGroup group = null;

    LinkedBlockingQueue<Channel> queue = new LinkedBlockingQueue();

    SelectorThread(SelectorThreadGroup group) {
        try {
            this.group = group;
            selector = Selector.open();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        // 上来就是死循环，Loop
        while (true) {

            try {
                // 如果不传参数，selector.select()是阻塞，那么往下执行的时候，nums必然大于0
                // 1,select()
//                System.out.println(Thread.currentThread().getName() + " - before select, select.length: " + selector.keys().size());
                int nums = selector.select();
//                System.out.println(Thread.currentThread().getName() + " - after select, select.length: " + selector.keys().size());
                // 2,如果nums大于0，处理selectKeys
                if (nums > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        }
                    }
                }
                // 3,处理一些task
                if(!queue.isEmpty()) {
                    Channel c = queue.take();
                    if(c instanceof ServerSocketChannel) {
                        ((ServerSocketChannel) c).register(selector, SelectionKey.OP_ACCEPT);
                    } else if (c instanceof SocketChannel) {
                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        ((SocketChannel) c).register(selector, SelectionKey.OP_READ, buffer);
                    }

                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }
    }



    public void acceptHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + "   accept handler.......");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);

            group.nextSelector(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + "   read handler.......");
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

    public void setWorker(SelectorThreadGroup group) {
        this.group = group;
    }
}
