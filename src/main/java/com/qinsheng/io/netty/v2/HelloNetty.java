package com.qinsheng.io.netty.v2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.nio.ByteBuffer;

/**
 * @Author: qinsheng
 * @Date: 2020/6/27 01:52
 * Netty入门级代码
 */
public class HelloNetty {

    public static void main(String[] args) {
        new NettyServer(8888).serverStart();
    }

}

class NettyServer {
    int port = 9999;

    public NettyServer(int port) {
        this.port = port;
    }

    public void serverStart() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // 负责客户端连接
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // 负责连接后的处理

        ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                // 每个客户端连接后，怎么进行处理，就是new Channel...里的方法了
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    // 通道一旦初始化
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // 添加对这个通道的处理器
                        socketChannel.pipeline().addLast(new Handler());
                    }
                });

        try {
            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}

class Handler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("server: channel read");
        ByteBuf buf = (ByteBuf)msg;
        System.out.println(buf.toString(CharsetUtil.UTF_8));

        ctx.writeAndFlush(msg);
        ctx.close();
    }
}