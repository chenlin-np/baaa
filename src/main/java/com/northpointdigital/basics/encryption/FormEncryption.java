package com.northpointdigital.com.encryption;

import com.northpointdigital.com.exception.NorthpointException;

public interface FormEncryption {

    public String encrypt(String plainText) throws NorthpointException;
    public String decrypt(String secret) throws NorthpointException;
    
}
