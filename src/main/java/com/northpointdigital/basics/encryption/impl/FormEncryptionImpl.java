package com.northpoint.basics.encryption.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import com.northpoint.basics.dataimport.impl.DataImporterFactoryImpl;
import com.northpoint.basics.encryption.FormEncryption;
import com.northpoint.basics.exception.NorthpointException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@Service(value = FormEncryption.class)
@Properties({
	@Property(name = "service.pid", value = "com.northpoint.basics.encryption.FormEncryption", propertyPrivate = false),
	@Property(name = "service.description", value = "Northpoint Form Encryption Service", propertyPrivate = false),
	@Property(name = "service.vendor", value = "Northpoint", propertyPrivate = false) })
public class FormEncryptionImpl implements FormEncryption {
	private static Logger log = LoggerFactory
		    .getLogger(DataImporterFactoryImpl.class);
	public static final char SEPARATOR = (char)30;
	public String encrypt(String plainText) throws NorthpointException {
		try {
            AESCipher aes =AESCipher.getInstance();
            log.error("===========form encrypting===========");
            Secret secret = aes.encrypt(plainText);
            return secret.toString();
            
        } catch (Exception e) {
        	log.error("Encryption Exception");
        	log.error(e.toString());
            throw new NorthpointException(e,"Encryption Exception");
        }
	}

	public String decrypt(String secret) throws NorthpointException {
		try {
            AESCipher aes =AESCipher.getInstance();
            log.error("===========form decrypting===========");
            String[] tmpStrings=secret.split(String.valueOf(SEPARATOR));
            Secret crypt=new Secret(tmpStrings[0], tmpStrings[1], Secret.ITERATIONS);
            aes.decrypt(crypt);
            return crypt.getValue();
        } catch (Exception e) {
        	log.error("Decryption Exception");
        	log.error(e.toString());
            throw new NorthpointException(e,"Decryption Exception");
        }
	}



}
