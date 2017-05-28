package com.chenyou;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;
/**
 * Created by ChenYou on 2017/5/16.
 */
public class LiamCipher {
    private String cipherName;
    private byte[] nonce;
    private Cipher cipher;
    //private byte[] key;
    private int overHead;
    private int mode;
    private int keySize;
    private final int nonceSize = 12;
    private final int saltSize = 12;
    private final int tagSize = 16;
    //private final int keySize = 32;
    public LiamCipher(String cName, int keySize, boolean isEncrypt) throws Exception {
        this.cipherName = cName;
        //this.key = key;
        this.keySize = keySize;
        this.nonce = new byte[this.nonceSize];
        if(isEncrypt) this.mode = Cipher.ENCRYPT_MODE;
        else this.mode = Cipher.DECRYPT_MODE;
        if(cipherName.equals("AEAD_AES_256_GCM")) {
            this.cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        }
    }
    public byte[] encrypt(byte[] plainText) throws Exception {
        if(this.cipher == null) {
            throw new RuntimeException("Uninitialized Cipher");
        }
        return this.cipher.doFinal(plainText);
    }
    public byte[] decrypt(byte[] cipherText) throws Exception {
        if(this.cipher == null) {
            throw new RuntimeException("Uninitialized Cipher");
        }
        return this.cipher.doFinal(cipherText);
    }

    public int getOverHead() throws Exception {
        if(this.cipher == null) {
            throw new RuntimeException("Uninitialized Cipher");
        }
        return this.overHead;
    }

    public void init(byte[] key) throws Exception {
        if(this.keySize != key.length) {
            throw new RuntimeException("Key size doesn't match");
        }

        GCMParameterSpec spec = new GCMParameterSpec(this.tagSize * 8, this.nonce);
        SecretKey secretKey = new SecretKeySpec(key, 0, this.keySize, "AES");
        this.cipher.init(this.mode, secretKey, spec);
        if(this.overHead == 0) {
            if (this.mode == Cipher.ENCRYPT_MODE) {
                this.overHead = this.cipher.getOutputSize(0);
            } else {
                int tmpOverhead = 0;
                while (this.cipher.getOutputSize(tmpOverhead) <= 0) {
                    tmpOverhead++;
                }
                this.overHead = tmpOverhead - 1;
            }
        }
    }

    public void increaseNonce() {
        for(int i = 0; i < this.nonce.length; i++) {
            this.nonce[i]++;
            if((this.nonce[i] & 0xFF) != 0) {
                break;
            }
        }
    }
    /*
    private static byte[] encrypt(String s, byte[] k) throws Exception {
        SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
        // Generate 128 bit IV for Encryption
        byte[] iv = new byte[12]; r.nextBytes(iv);

        SecretKeySpec eks = new SecretKeySpec(k, "AES");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");

        // Generated Authentication Tag should be 128 bits
        c.init(Cipher.ENCRYPT_MODE, eks, new GCMParameterSpec(128, iv));
        //byte[] es = c.doFinal(s.getBytes(StandardCharsets.UTF_8));
        byte[] es = c.doFinal(s.getBytes());

        // Return a Base64 Encoded String
        return es;
    }*/
    public static final int AES_KEY_SIZE = 256; // in bits
    public static final int GCM_NONCE_LENGTH = 12; // in bytes
    public static final int GCM_TAG_LENGTH = 16; // in bytes

    public static void shoot() throws Exception {
        int testNum = 2; // pass

        /*if (args.length > 0) {
            testNum = Integer.parseInt(args[0]);
            if (testNum <0 || testNum > 3) {
                System.out.println("Usage: java AESGCMUpdateAAD2 [X]");
                System.out.println("X can be 0, 1, 2, 3");
                System.exit(1);
            }
        }*/
        byte[] input = "Hello AES-GCM World!".getBytes();

        // Initialise random and generate key
        SecureRandom random = SecureRandom.getInstanceStrong();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, random);
        SecretKey key = keyGen.generateKey();

        // Encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        final byte[] nonce = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(nonce);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] aad = "Whatever I like".getBytes();;
        cipher.updateAAD(aad);

        byte[] cipherText = cipher.doFinal(input);

        // Decrypt; nonce is shared implicitly
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        // EXPECTED: Uncommenting this will cause an AEADBadTagException when decrypting
        // because AAD value is altered
        if (testNum == 1) aad[1]++;

        cipher.updateAAD(aad);

        // EXPECTED: Uncommenting this will cause an AEADBadTagException when decrypting
        // because the encrypted data has been altered
        if (testNum == 2) cipherText[10]++;

        // EXPECTED: Uncommenting this will cause an AEADBadTagException when decrypting
        // because the tag has been altered
        if (testNum == 3) cipherText[cipherText.length - 2]++;

        try {
            byte[] plainText = cipher.doFinal(cipherText);
            if (testNum != 0) {
                System.out.println("Test Failed: expected AEADBadTagException not thrown");
            } else {
                // check if the decryption result matches
                if (Arrays.equals(input, plainText)) {
                    System.out.println("Test Passed: match!");
                } else {
                    System.out.println("Test Failed: result mismatch!");
                    System.out.println(new String(plainText));
                }
            }
        } catch(Exception ex) {
            if (testNum == 0) {
                System.out.println("Test Failed: unexpected ex " + ex);
                ex.printStackTrace();
            } else {
                System.out.println("Test Passed: expected ex " + ex);
            }
        }
    }

}
