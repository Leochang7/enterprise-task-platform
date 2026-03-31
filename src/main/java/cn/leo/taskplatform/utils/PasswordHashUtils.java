package cn.leo.taskplatform.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordHashUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHashUtils() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public static String hash(String rawPassword, String salt) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not supported", ex);
        }
    }

    public static boolean matches(String rawPassword, String salt, String expectedHash) {
        if (expectedHash == null || salt == null) {
            return false;
        }
        return expectedHash.equalsIgnoreCase(hash(rawPassword, salt));
    }
}
