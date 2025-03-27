package util;

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

}