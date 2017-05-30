package com.chenyou;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ChannelOption;
import io.netty.buffer.Unpooled;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;


/**
 * Created by ChenYou on 2017/5/15.
 */
public class InMsgHandler extends ChannelInboundHandlerAdapter {
    private Channel outMsgChannel;
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        final Channel inMsgChannel = ctx.channel();
        byte[] rawAddrMsg = (byte[])msg;
        try {
            String addr = Utils.parseAddr(rawAddrMsg);
            int port = Utils.parsePort(rawAddrMsg);

            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new OutMsgHandlerInitializer(inMsgChannel));

            ChannelFuture connectFuture = b.connect(addr, port);
            this.outMsgChannel = connectFuture.awaitUninterruptibly().channel();
        } catch (IllegalArgumentException ie) {
            if(this.outMsgChannel.isActive()) {
                this.outMsgChannel.writeAndFlush(Unpooled.copiedBuffer(rawAddrMsg)).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            ctx.channel().read();
                        } else {
                            System.out.println(future.cause());
                            future.channel().close();
                        }
                    }
                });
            } else {

            }
        } catch (Exception ue) {
            System.out.println(ue.getMessage());
        }
        return;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        Utils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Utils.closeOnFlush(this.outMsgChannel);

    }
}
