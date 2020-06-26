package com.qinsheng.io.reactor;

/**
 * @Author: qinsheng
 * @Date: 2020/6/26 16:33
 */
public class MainThread {

    public static void main(String[] args) {

        // 创建几个selector thread
        SelectorThreadGroup boss = new SelectorThreadGroup(3);
        SelectorThreadGroup group = new SelectorThreadGroup(3);

        boss.setWorker(group);

        boss.bind(9999);
        boss.bind(8888);
        boss.bind(7777);
        boss.bind(6666);

    }

}
