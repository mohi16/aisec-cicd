package com.thesis.securitystudy.util;

/**
 * Utility class for note encryption/decryption.
 * Implement actual crypto in these methods (key management, IVs, encoding, etc.).
 */
public final class EncryptionUtil {

    private EncryptionUtil() { /* utility */ }

    /**
     * Encrypt plaintext with provided key and return ciphertext (e.g. base64).
     * Implement proper authenticated encryption (AEAD) in your implementation.
     */
    public static String encrypt(String plaintext, String key) {
        // TODO: implement encryption
        throw new UnsupportedOperationException("TODO implement encrypt");
    }

    /**
     * Decrypt ciphertext with provided key and return plaintext.
     */
    public static String decrypt(String ciphertext, String key) {
        // TODO: implement decryption
        throw new UnsupportedOperationException("TODO implement decrypt");
    }
}
