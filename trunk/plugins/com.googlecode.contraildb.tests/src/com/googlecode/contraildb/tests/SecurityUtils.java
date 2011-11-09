package com.googlecode.contraildb.tests;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class SecurityUtils {
	private static final String DES = "DES";
	private static final String HMAC_SHA1 = "HmacSHA1";
	
	public static byte[] calculateRFC2104HMAC(byte[] data, String signingKey)
	{
		try {
			SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(), HMAC_SHA1);
			Mac mac = Mac.getInstance(HMAC_SHA1);
			mac.init(keySpec);
			return mac.doFinal(data);
		}
		catch (GeneralSecurityException x) {
			throw new RuntimeException("Error creating signature", x);
		}
	}

	public byte[] encrypt(byte[] data, String signingKey) {

		try {
			SecretKeySpec key = new SecretKeySpec(signingKey.getBytes(), DES);
			Cipher cipher = Cipher.getInstance(DES);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(data);
		} 
		catch (GeneralSecurityException  e) {
			throw new RuntimeException("Error encrypting data", e);
		} 
	}

	public byte[] decrypt(byte[] data, String signingKey) {

		try {
			SecretKeySpec key = new SecretKeySpec(signingKey.getBytes(), DES);
			Cipher cipher = Cipher.getInstance(DES);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(data);
		} 
		catch (GeneralSecurityException  e) {
			throw new RuntimeException("Error decrypting data", e);
		} 
	}
}

