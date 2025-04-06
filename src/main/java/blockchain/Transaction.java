package blockchain;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;

import util.CryptoUtil;

/**
 * Represents a transaction in the DepChain blockchain
 */
public class Transaction {
    private final String from;
    private final String to;
    private final BigInteger value;
    private final Bytes data;
    private final long nonce;
    private final long timestamp;
    private byte[] signature;
    private String hash;

    /**
     * Constructor for creating a new transaction
     * 
     * @param from      The sender address
     * @param to        The recipient address
     * @param value     The amount to transfer
     * @param data      The transaction data (for contract calls)
     * @param nonce     The sender's nonce
     * @param timestamp The transaction timestamp
     */
    public Transaction(String from, String to, BigInteger value, long nonce, Bytes data, long timestamp) {
        this.from = from;
        this.to = to;
        this.value = value;
        this.data = data;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.hash = calculateHash();
    }

    /**
     * Signs the transaction with the sender's private key
     * 
     * @param privateKey The sender's private key
     * @throws Exception If signing fails
     */
    public void sign(PrivateKey privateKey) throws Exception {
        byte[] data = getSignatureData();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        this.signature = signature.sign();
        // Recalculate hash with signature
        this.hash = calculateHash();
    }

    /**
     * Verifies the transaction signature
     * 
     * @param publicKey The sender's public key
     * @return True if the signature is valid, false otherwise
     * @throws Exception If verification fails
     */
    public boolean verify(PublicKey publicKey) throws Exception {
        if (signature == null) {
            return false;
        }

        byte[] data = getSignatureData();
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(this.signature);
    }

    /**
     * Gets the data to be signed
     * 
     * @return The data to be signed
     */
    public byte[] getSignatureData() {
        return CryptoUtil.sha256(
                from + to + value.toString() + data.toHexString() + nonce + timestamp);
    }

    /**
     * Calculates the transaction hash
     * 
     * @return The transaction hash
     */
    private String calculateHash() {
        byte[] hashData = getSignatureData();
        if (signature != null) {
            byte[] combined = new byte[hashData.length + signature.length];
            System.arraycopy(hashData, 0, combined, 0, hashData.length);
            System.arraycopy(signature, 0, combined, hashData.length, signature.length);
            hashData = combined;
        }
        return "0x" + CryptoUtil.bytesToHex(CryptoUtil.sha256(hashData));
    }

    /**
     * Converts the transaction to a map for JSON serialization
     * 
     * @return A map representing the transaction
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("hash", hash);
        map.put("from", from);
        map.put("to", to);
        map.put("value", value.toString());
        map.put("data", data != null ? "0x" + data.toHexString() : null);
        map.put("nonce", nonce);
        map.put("timestamp", timestamp);
        map.put("signature", signature != null ? "0x" + CryptoUtil.bytesToHex(signature) : null);
        return map;
    }

    // Getters
    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getValue() {
        return value;
    }

    public Bytes getData() {
        return data;
    }

    public long getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getHash() {
        return hash;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}