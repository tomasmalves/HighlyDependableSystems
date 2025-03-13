package consensus;

import java.io.ByteArrayOutputStream;
import java.security.*;
import java.util.Base64;

public class ConsensusMessage {

    public enum MessageType {INIT, PROPOSE,VOTE, DECIDE}

    private final MessageType type;
    private final int epoch;
    private final String value;
    private final String signature;

    // Constructor for creating new messages (signing required)
    public ConsensusMessage(MessageType type, int epoch, String value, PrivateKey privateKey) throws Exception {
        if (type == null || value == null || privateKey == null) {
            throw new IllegalArgumentException("Type, value, and privateKey cannot be null.");
        }
        this.type = type;
        this.epoch = epoch;
        this.value = value;
        this.signature = signMessage(privateKey);
    }

    // Constructor for received messages (signature already exists)
    public ConsensusMessage(MessageType type, int epoch, String value, String signature) {
        if (type == null || value == null || signature == null) {
            throw new IllegalArgumentException("Type, value, and signature cannot be null.");
        }
        this.type = type;
        this.epoch = epoch;
        this.value = value;
        this.signature = signature;
    }

    public MessageType getType() { return type; }
    public int getEpoch() { return epoch; }
    public String getValue() { return value; }
    public String getSignature() { return signature; }

    // Sign the message using the private key
    private String signMessage(PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);

        // Use ByteArrayOutputStream to create a secure byte array for signing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(type.ordinal());  // Using ordinal to store the type
        outputStream.write(epoch);
        outputStream.write(value.getBytes());

        signer.update(outputStream.toByteArray());
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    // Verify the signature of the message using the public key
    public boolean verifySignature(PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);

        // Use ByteArrayOutputStream to create a secure byte array for verification
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(type.ordinal());  // Using ordinal to store the type
        outputStream.write(epoch);
        outputStream.write(value.getBytes());

        verifier.update(outputStream.toByteArray());
        return verifier.verify(Base64.getDecoder().decode(signature));
    }

    @Override
    public String toString() {
        return type + "|" + epoch + "|" + value + "|" + signature;
    }
}
