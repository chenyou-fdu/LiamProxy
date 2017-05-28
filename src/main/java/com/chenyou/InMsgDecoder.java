package com.chenyou;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

/**
 * Created by ChenYou on 2017/5/16.
 */
public class InMsgDecoder extends ByteToMessageDecoder {
    private byte[] subkey;
    private int payloadLen;
    private LiamCipher cipher;
    // tmp define for easily debug
    private final String password = "1";

    public InMsgDecoder() {
        payloadLen = -1;
    }
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        int readableSize = buf.readableBytes();
        if(this.subkey == null && readableSize >= 32) {
            byte[] salt = new byte[32];
            buf.readBytes(salt);
            byte[] key = Hkdf.kdf(this.password, 32);
            Hkdf h = Hkdf.getInstance("HmacSHA1", key, salt, "ss-subkey".getBytes());
            this.subkey = h.getKey(32);
            this.cipher = new LiamCipher("AEAD_AES_256_GCM", 32, false);
        } else if(this.subkey != null && this.payloadLen == -1 && readableSize >= 2 + this.cipher.getOverHead()) {
            this.cipher.init(this.subkey);
            byte[] encryptedPayloadlen = new byte[2 + this.cipher.getOverHead()];
            buf.readBytes(encryptedPayloadlen);
            try {
                byte[] payloadLenRaw = this.cipher.decrypt(encryptedPayloadlen);
                this.payloadLen = ((((int)payloadLenRaw[0] & 0xFF) << 8) + ((int)payloadLenRaw[1] & 0xFF)) & 0x3FFF;
            } catch (Exception ex) {
                System.out.println("Size Decrypt Wrong");
            } finally {
                this.cipher.increaseNonce();
            }
        } else if(this.payloadLen != -1 && readableSize >= this.payloadLen + this.cipher.getOverHead()) {
            this.cipher.init(this.subkey);
            byte[] encryptedPayload = new byte[this.payloadLen + this.cipher.getOverHead()];
            buf.readBytes(encryptedPayload);
            try {
                byte[] payload = this.cipher.decrypt(encryptedPayload);
                out.add(payload);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            } finally {
                this.payloadLen = -1;
                this.cipher.increaseNonce();
            }
        }
        return;
    }

}
