package com.chenyou;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by ChenYou on 2017/5/17.
 */
public class Hkdf {

    private String hmacName;
    private SecretKeySpec prk;
    private int size;
    private byte[] info;
    private byte counter;
    private byte[] prev;
    private byte[] cache;


    public static Hkdf getInstance(String hmacName, byte[] secret, byte[] salt, byte[] info) throws Exception {
        Mac extractor = Mac.getInstance(hmacName);
        if(salt.length == 0) {
            salt = new byte[extractor.getMacLength()];
        }
        extractor.init(new SecretKeySpec(salt, hmacName));
        byte[] prk = extractor.doFinal(secret);
        return new Hkdf(hmacName, new SecretKeySpec(prk, hmacName), extractor.getMacLength(), info, (byte)1, null, null);
    }

    public byte[] getKey(final int keySize) throws Exception {
        byte[] res = new byte[keySize];
        getKeyHelper(this.info, keySize, res, 0);
        return res;
    }

    private void getKeyHelper(final byte[] info, final int keySize, final byte[] output, final int offset) throws Exception {
        if(keySize < 0) {
            throw new IllegalArgumentException("Key size must be non-negative");
        }
        if(output.length < offset + keySize) {
            throw new IllegalArgumentException("Buffer too short");
        }
        Mac mac = getMac();
        if(keySize > 255 * mac.getMacLength()) {
            throw new IllegalArgumentException("Key size too long");
        }
        byte[] t = new byte[0];
        try {
            int pos = 0;
            byte i = 1;
            while(pos < keySize) {
                mac.reset();
                mac.update(t);
                mac.update(info);
                mac.update(i);
                t = mac.doFinal();

                for (int x = 0; x < t.length && pos < keySize; x++, pos++) {
                    output[pos] = t[x];
                }
                i++;
            }
        } finally {
            Arrays.fill(t, (byte)0);
        }
    }

    private Hkdf(String hmacName, SecretKeySpec prk, int size, byte[] info, byte counter, byte[] prev, byte[] cache) {
        this.hmacName = hmacName;
        this.prk = prk;
        this.size = size;
        this.info = info;
        this.counter = counter;
        this.prev = prev;
        this.cache = cache;
    }

    private Mac getMac() throws Exception {
        Mac mac = Mac.getInstance(this.hmacName);
        mac.init(this.prk);
        return mac;
    }

    public static byte[] kdf(String password, int keySize) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] res = new byte[keySize];
        byte[] prev = new byte[0];
        int sPos = 0;
        while(sPos < keySize) {
            md.update(prev);
            md.update(password.getBytes());
            byte[] tmp = md.digest();
            for(int i = sPos, j = 0; j < tmp.length && i < res.length; i++, j++) {
                res[i] = tmp[j];
            }
            prev = new byte[md.getDigestLength()];
            for(int i = tmp.length-md.getDigestLength(), j = 0; i < tmp.length; i++, j++) {
                prev[j] = tmp[i];
            }
            md.reset();
            sPos += md.getDigestLength();
        }
        return res;
    }
}
