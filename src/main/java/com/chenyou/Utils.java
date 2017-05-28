package com.chenyou;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.io.UnsupportedEncodingException;

/**
 * Created by ChenYou on 2017/5/23.
 */
public class Utils {
    public static String parseAddr(byte[] rawAddr) throws IllegalArgumentException, UnsupportedEncodingException {
        if(rawAddr.length == 0) {
            throw new IllegalArgumentException("Empty Raw Address");
        }
        String addr;
        switch(rawAddr[0]) {
            case 0x01:
                addr = new String(rawAddr, 1, 4, "UTF-8");
                break;
            case 0x03:
                addr = new String(rawAddr, 2, (int)rawAddr[1], "UTF-8");
                break;
            case 0x04:
                addr = new String(rawAddr, 1, 16, "UTF-8");
                break;
            default:
                throw new IllegalArgumentException("Not Raw Address");
        }
        return addr;
    }
    public static int parsePort(byte[] rawAddr) throws IllegalArgumentException {
        int rawAddrSize = rawAddr.length;
        if(rawAddrSize == 0) {
            throw new IllegalArgumentException("Empty Raw Address");
        }
        if((rawAddr[0] != 0x01 && rawAddr[0] != 0x03 && rawAddr[0] != 0x04) || rawAddrSize < 2) {
            throw new IllegalArgumentException("Wrong Raw Address Format");
        }
        int portHigh = rawAddr[rawAddrSize-2] & 0xFF, portLow = (int)rawAddr[rawAddrSize-1] & 0xFF;
        return ((portHigh << 8) | portLow);
    }
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
