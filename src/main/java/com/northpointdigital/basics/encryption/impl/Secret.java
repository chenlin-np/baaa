package com.northpoint.basics.encryption.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class Secret {
    // 32 byte, 256 bit secret key
    // secret must be exactly 32bytes in UTF-8 character encoding
    //protected static final String testkey = "11111111111111111111111111111111";
    protected static final String key = "\u0008\u0008\u00a9\u00ae\u20ac\u0009\u00a5!\u01FC\u000CK\u263a/JNQcd7g/7O\u20a9";

    // number of times a secret is salt-encrypted before writing to persistent store
    public static final int ITERATIONS = 2;

    private String value; // secret value -- supports UTF-8 encoding
    private String salt; // prepended salt -- should be in ASCII
    private int encrypted; // number of times secret has been been salt-encrypted

    protected Secret() {}; // a secret is not useful unless value, salt, and encrypted fields are set

    public Secret(String value, String salt, int encrypted) {
        this.salt = salt;
        this.value = value;
        this.encrypted = encrypted;
    }

    public String getValue() {
        return value;
    }

    public int getEncrypted() {
        return encrypted;
    }

    public void setValue(String encodedString) {
        this.value = encodedString;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public int encryptIt(Cipher cipher, byte[] encoded) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        byte[] crypt = cipher.doFinal(encoded);
        this.value = new String(Base64Coder.encode(crypt));
        this.encrypted++ ;
        return this.encrypted;
    }

    public int decryptIt(Cipher cipher) throws IllegalBlockSizeException, BadPaddingException, IOException {
        byte[] decodedValue = Base64Coder.decode(this.value);
        byte[] decValue = cipher.doFinal(decodedValue);
        this.value = new String(decValue,"UTF-8").substring(this.salt.length());
        this.encrypted-- ;
        return this.encrypted;
    }
    
    public String toString() {
		return value+String.valueOf(FormEncryptionImpl.SEPARATOR)+salt;
	}
}
