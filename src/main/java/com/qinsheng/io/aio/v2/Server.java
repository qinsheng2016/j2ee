package com.qinsheng.io.aio.v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @Author: qinsheng
 * @Date: 2020/6/27 01:32
 * AIO，异步处理
 * 为什么有了AIO还要有NIO，Aio和Nio在Linux下都是epoll实现的
 * Netty封装了NIO，API更像是AIO
 * Windows可以支持AIO，但是Windows服务器少，所以Netty也不关心。
 */
public class Server {

    public static void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(9999));

        // 异步处理，将后面的代码交由操作系统，有连接进来时，使用后面的一段代码处理
        // 观察者模式
        server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel client, Object attachment) {
                server.accept(null, this);

                try {
                    System.out.println(client.getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(2048);
                    // 这里也是一个异步方法
                    client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            attachment.flip();
                            System.out.println(new String(attachment.array(), 0, result));
                            client.write(ByteBuffer.wrap("IamAIO".getBytes()));
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {

                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        });

        while (true) {
            Thread.sleep(1000);
        }

    }

}
