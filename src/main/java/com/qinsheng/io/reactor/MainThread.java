package com.qinsheng.io.reactor;

/**
 * @Author: qinsheng
 * @Date: 2020/6/26 16:33
 */
public class MainThread {

    public static void main(String[] args) {

        // 创建几个selector thread
        SelectorThreadGroup group = new SelectorThreadGroup(3);
        group.bind(9999);

    }

}
