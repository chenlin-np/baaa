package com.northpointdigital.com.encryption.impl;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESCipher {
	private static AESCipher INSTANCE = null;
	public static synchronized AESCipher getInstance() throws Exception{
        if (INSTANCE == null) {
            INSTANCE = new AESCipher();
        }
        return INSTANCE;
    }
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String CIPHER_AUTH = "SunJCE";

    private SecretKey key;

    private AESCipher() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        this.key = generateKey(Secret.key);
    };

    private AESCipher(String key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        this.key = generateKey(key);
    }
    
    public synchronized Secret encrypt(String plainText) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, CIPHER_AUTH);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        Secret returnSecret = new Secret(plainText, Hash.generateRandomSaltString(), 0);
        for (int i = 0; i < Secret.ITERATIONS; i++ ) {
            byte[] sec = (returnSecret.getSalt() + returnSecret.getValue()).getBytes("UTF-8");
            returnSecret.encryptIt(cipher, sec);
        }
        return returnSecret;
    }

    public synchronized Secret decrypt(Secret secret) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, CIPHER_AUTH);
        cipher.init(Cipher.DECRYPT_MODE, key);
        while (secret.getEncrypted() > 0) {
            secret.decryptIt(cipher);
        }
        return secret;
    }

    public static synchronized SecretKey generateKey(String secretString) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // PrintStream out = new PrintStream(System.out, true, "UTF-8");
        // out.println("generating secret key : " + secretString);
        SecretKey secret = null;
        if (secretString == null) {
            // generate new secret
            KeyGenerator kgen = KeyGenerator.getInstance(KEY_ALGORITHM);
            kgen.init(256);
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            secret = new SecretKeySpec(raw, "AES");
        } else {
            // use existing secret
            secret = new SecretKeySpec(secretString.getBytes("UTF-8"), "AES");
        }
        return secret;
    }
}
