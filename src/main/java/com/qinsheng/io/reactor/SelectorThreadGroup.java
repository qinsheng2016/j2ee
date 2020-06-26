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

    SelectorThreadGroup stg = this;

    public SelectorThreadGroup(int num) {
        selectorThreads = new SelectorThread[num];
        for(int i=0; i<selectorThreads.length; i++) {
            selectorThreads[i] = new SelectorThread(this);
            new Thread(selectorThreads[i]).start();
        }
    }

    public void setWorker(SelectorThreadGroup stg) {
        this.stg = stg;
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

//            nextSelector(server);
            nextSelector2(server);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void nextSelector2(Channel c) {
        try {
            if (c instanceof ServerSocketChannel) {
                int index = xid.incrementAndGet() % selectorThreads.length;
                SelectorThread selectorThread = selectorThreads[index];
                selectorThread.queue.put(c);
                selectorThread.setWorker(stg);
                selectorThread.selector.wakeup();
            } else {
                int index = xid.incrementAndGet() % stg.selectorThreads.length;
                SelectorThread selectorThread = stg.selectorThreads[index];
                selectorThread.queue.add(c);
                selectorThread.selector.wakeup();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void nextSelector(Channel c) {
        try {
            int index = xid.incrementAndGet() % selectorThreads.length;
            SelectorThread selectorThread = selectorThreads[index];
            selectorThread.queue.put(c);
            selectorThread.selector.wakeup();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
