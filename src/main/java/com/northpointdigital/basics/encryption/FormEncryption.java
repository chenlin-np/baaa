package com.northpointdigital.basics.encryption;

import com.northpointdigital.basics.exception.NorthpointException;

public interface FormEncryption {

    public String encrypt(String plainText) throws NorthpointException;
    public String decrypt(String secret) throws NorthpointException;
    
}
