package com.vaccination.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

/**
 * Enterprise cryptographic utility for password hashing and verification.
 * 
 * Uses standard Java Cryptography Architecture (JCA):
 * - Algorithm: PBKDF2WithHmacSHA256
 * - Iterations: 65,536 (computationally resilient against GPU/ASIC attacks)
 * - Key Length: 256 bits (32 bytes)
 * - Salt: 16 cryptographically secure random bytes per user (SecureRandom)
 * - Verification: Constant-time comparison (MessageDigest.isEqual) to prevent timing attacks
 * 
 * Zero external libraries required.
 */
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically secure random 16-byte salt,
     * encoded as a 32-character hexadecimal string.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Computes the PBKDF2-HMAC-SHA256 hash of a password using the given salt.
     *
     * @param password the plaintext password char sequence
     * @param saltHex  the 32-character hex-encoded salt
     * @return the 64-character hex-encoded derived key hash
     */
    public static String hashPassword(String password, String saltHex) {
        if (password == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt must not be null.");
        }
        try {
            byte[] salt = HexFormat.of().parseHex(saltHex);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword(); // Wipe sensitive char array from memory
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic algorithm unavailable: " + ALGORITHM, e);
        }
    }

    /**
     * Verifies whether a candidate plaintext password matches the stored PBKDF2 hash.
     * Uses constant-time byte comparison to thwart side-channel timing attacks.
     *
     * @param candidatePassword the password submitted by the user
     * @param storedHashHex     the expected hash from the database
     * @param storedSaltHex     the salt associated with the user
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String candidatePassword, String storedHashHex, String storedSaltHex) {
        if (candidatePassword == null || storedHashHex == null || storedSaltHex == null) {
            return false;
        }
        try {
            String candidateHashHex = hashPassword(candidatePassword, storedSaltHex);
            byte[] expectedBytes = HexFormat.of().parseHex(storedHashHex);
            byte[] candidateBytes = HexFormat.of().parseHex(candidateHashHex);
            return MessageDigest.isEqual(expectedBytes, candidateBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
