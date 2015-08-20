package com.northpoint.basics.encryption.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Hash {
    protected static final int SALTYNESS = 599; // 829th prime number
    	
    public static synchronized String SHA1(String text, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.reset();
        md.update(salt.getBytes("UTF-8"));
        byte[] digested = md.digest(text.getBytes("UTF-8"));
        for (int i = 0; i < SALTYNESS; i++ ) {
            md.reset();
            digested = md.digest(digested);
        }
        return new String(Base64Coder.encode(digested));
    }

//    public static void main(String[] args) {
//    	try {
//    		String randomSalt = generateRandomSaltString();
//			System.out.println("Random Salt = " + randomSalt);
//			System.out.println("Salted hash of foobar = " + SHA1("foobar", randomSalt));
//		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//    }

    private static synchronized byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        return salt;
    }

    public static synchronized String generateRandomSaltString() throws NoSuchAlgorithmException {
        byte[] salt = generateSalt();
        return new String(Base64Coder.encode(salt));
    }
}
