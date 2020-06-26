package com.qinsheng.io.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: qinsheng
 * @Date: 2020/6/26 18:59
 */
public class SelectorThreadGroup {
    SelectorThread[] selectorThreads;
    ServerSocketChannel server = null;
    AtomicInteger xid = new AtomicInteger(0);

    public SelectorThreadGroup(int num) {
        selectorThreads = new SelectorThread[num];
        for(int i=0; i<selectorThreads.length; i++) {
            selectorThreads[i] = new SelectorThread(this);
            new Thread(selectorThreads[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            nextSelector(server);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void nextSelector(Channel server) {
        int index = xid.incrementAndGet() % selectorThreads.length;
        SelectorThread selectorThread = selectorThreads[index];

        selectorThread.queue.add(server);
        selectorThread.selector.wakeup();

    }


}
