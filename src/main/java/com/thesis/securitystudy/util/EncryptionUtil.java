package com.thesis.securitystudy.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class EncryptionUtil {

    // In production provide a secure key via application properties / env var.
    // Key must be 16 bytes (128 bit) for this implementation. Default is for dev only.
    @Value("${app.encryption.key:0123456789abcdef}")
    private String key;

    private SecretKeySpec keySpec;
    private static final String AES = "AES";
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits

    @PostConstruct
    public void init() {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // ensure key length is 16 bytes (128 bit). If longer/shorter, adjust by trimming/padding.
        keyBytes = Arrays.copyOf(keyBytes, 16);
        keySpec = new SecretKeySpec(keyBytes, AES);
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom rnd = new SecureRandom();
            rnd.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // store iv + ciphertext together
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String base64IvAndCipher) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64IvAndCipher);
            if (combined.length < IV_LENGTH) {
                throw new RuntimeException("Invalid cipher text");
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
