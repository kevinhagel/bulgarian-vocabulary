package com.vocab.bulgarian.audio.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for generating deterministic content hashes for audio caching.
 * Thread-safe: creates new MessageDigest instance for each hash operation.
 */
public class ContentHashUtil {

    private ContentHashUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates SHA-256 hash for audio content identification.
     * Combines text and voice name to ensure unique hashing per voice.
     *
     * @param text the Bulgarian text to speak
     * @param voiceName the Azure TTS voice name (e.g., bg-BG-KalinaNeural)
     * @return 64-character lowercase hex string (SHA-256 hash)
     */
    public static String generateHash(String text, String voiceName) {
        try {
            // Create new instance for thread safety (MessageDigest is NOT thread-safe)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Pipe separator prevents collision: "ab" + "c" vs "a" + "bc"
            String content = text + "|" + voiceName;
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in JDK
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts byte array to lowercase hexadecimal string.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
