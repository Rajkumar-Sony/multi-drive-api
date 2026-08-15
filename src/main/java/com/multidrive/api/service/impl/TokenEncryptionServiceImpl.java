package com.multidrive.api.service.impl;

import com.multidrive.api.service.TokenEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionServiceImpl implements TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public TokenEncryptionServiceImpl(
            @Value("${app.security.token-encryption-key}")
            String encryptionKey
    ) {

        byte[] decodedKey =
                Base64.getDecoder().decode(encryptionKey);

        if (decodedKey.length != 32) {
            throw new IllegalArgumentException(
                    "TOKEN_ENCRYPTION_KEY must be a 256-bit Base64 encoded key"
            );
        }

        this.secretKey =
                new SecretKeySpec(decodedKey, "AES");

        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String plainText) {

        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        try {

            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(TAG_LENGTH, iv);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] encryptedBytes =
                    cipher.doFinal(
                            plainText.getBytes(StandardCharsets.UTF_8)
                    );

            byte[] combined =
                    new byte[iv.length + encryptedBytes.length];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encryptedBytes,
                    0,
                    combined,
                    iv.length,
                    encryptedBytes.length
            );

            return Base64.getEncoder()
                    .encodeToString(combined);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to encrypt token",
                    ex
            );
        }
    }

    @Override
    public String decrypt(String encryptedText) {

        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }

        try {

            byte[] combined =
                    Base64.getDecoder()
                            .decode(encryptedText);

            byte[] iv =
                    new byte[IV_LENGTH];

            byte[] encryptedBytes =
                    new byte[combined.length - IV_LENGTH];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    encryptedBytes,
                    0,
                    encryptedBytes.length
            );

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(TAG_LENGTH, iv);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] decryptedBytes =
                    cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to decrypt token",
                    ex
            );
        }
    }
}