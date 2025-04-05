package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Utility class for cryptographic operations
 */
public class CryptoUtil {

    // The signature algorithm being used (likely RSA or ECDSA)
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA"; // or "SHA256withECDSA"

    /**
     * Signs data using the provided private key
     * 
     * @param data       The data to sign
     * @param privateKey The private key to use for signing
     * @return The signature as a byte array
     * @throws Exception If signing fails
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verifies a signature against the original data using a public key
     * 
     * @param data      The original data
     * @param signature The signature to verify
     * @param publicKey The public key to use for verification
     * @return true if the signature is valid, false otherwise
     * @throws Exception If verification fails
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }

    /**
     * Generates a SHA-256 hash of the input string
     * 
     * @param input The string to hash
     * @return The resulting hash as a byte array
     */
    public static byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a SHA-256 hash of the input byte array
     * 
     * @param input The byte array to hash
     * @return The resulting hash as a byte array
     */
    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string
     * 
     * @param bytes The byte array to convert
     * @return The hexadecimal string representation
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Converts a hexadecimal string to a byte array
     * 
     * @param hex The hexadecimal string to convert
     * @return The byte array representation
     */
    public static byte[] hexToBytes(String hex) {
        // Remove "0x" prefix if present
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }

        // Ensure even length
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }

        return data;
    }
}