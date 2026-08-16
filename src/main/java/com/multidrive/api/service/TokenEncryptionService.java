package com.multidrive.api.service;

public interface TokenEncryptionService {

	String encrypt(String plainText);

	String decrypt(String encryptedText);

}