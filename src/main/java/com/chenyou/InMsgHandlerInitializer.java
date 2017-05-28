package com.chenyou;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by ChenYou on 2017/5/15.
 */
public class InMsgHandlerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new InMsgDecoder())
                .addLast(new InMsgHandler());
    }
}
