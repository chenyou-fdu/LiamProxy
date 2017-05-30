package com.chenyou;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelFuture;
import java.security.SecureRandom;

/**
 * Created by ChenYou on 2017/5/15.
 */
public class OutMsgHandler extends ChannelInboundHandlerAdapter {
    private final Channel inMsgChannel;
    private byte[] subkey;
    private int payloadLen;
    private final byte[] salt;
    private LiamCipher cipher;

    // tmp define for easily debug
    private final String password = "1";
    public OutMsgHandler(Channel inMsgChannel) {
        this.inMsgChannel = inMsgChannel;
        this.payloadLen = -1;
        this.salt = new byte[32];
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            random.nextBytes(this.salt);
            this.cipher = new LiamCipher("AEAD_AES_256_GCM", 32, true);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(this.subkey == null) {
            byte[] key = Hkdf.kdf(this.password, 32);
            Hkdf h = Hkdf.getInstance("HmacSHA1", key, this.salt, "ss-subkey".getBytes());
            this.subkey = h.getKey(32);
        }
        this.inMsgChannel.writeAndFlush(Unpooled.copiedBuffer(this.salt));
        return;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
        if(!buf.hasArray()) {
            int payloadLen = buf.readableBytes();
            if(payloadLen > 16*1024 - 1) {
                payloadLen = 16*1024 - 1;
            }
            byte[] payloadLenBytes = new byte[2];
            payloadLenBytes[0] = (byte)(payloadLen >> 8);
            payloadLenBytes[1] = (byte)payloadLen;
            byte[] payLoad = new byte[payloadLen];
            buf.getBytes(buf.readerIndex(), payLoad);
            this.cipher.init(this.subkey);
            byte[] encryptedPayloadlen = this.cipher.encrypt(payloadLenBytes);
            this.cipher.increaseNonce();
            this.cipher.init(this.subkey);
            byte[] encryptedPayload = this.cipher.encrypt(payLoad);
            this.cipher.increaseNonce();
            int payloadLenOffset = encryptedPayloadlen.length, payloadOffset = encryptedPayload.length;
            byte[] encryptedText = new byte[payloadLenOffset + payloadOffset];
            for(int i = 0; i < payloadLenOffset; i++) {
                encryptedText[i] =  encryptedPayloadlen[i];
            }
            for(int i = 0; i < payloadOffset; i++) {
                encryptedText[i + payloadLenOffset] = encryptedPayload[i];
            }
            this.inMsgChannel.writeAndFlush(Unpooled.copiedBuffer(encryptedText)).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        System.out.println(future.cause());
                        future.channel().close();
                    }
                }
            });
        }
        buf.release();
        return;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        Utils.closeOnFlush(ctx.channel());
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Utils.closeOnFlush(this.inMsgChannel);
    }
}
