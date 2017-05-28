package com.chenyou;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

import io.netty.channel.socket.SocketChannel;

/**
 * Created by ChenYou on 2017/5/24.
 */
public class OutMsgHandlerInitializer extends ChannelInitializer<SocketChannel> {
    private final Channel inMsgChannel;
    public OutMsgHandlerInitializer(Channel inMsgChannel) {
        this.inMsgChannel = inMsgChannel;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new OutMsgHandler(this.inMsgChannel));
    }
}
