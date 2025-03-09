package consensus;

import java.security.*;
import java.util.Base64;

public class ConsensusMessage {
	public enum MessageType {INIT, PROPOSE, DECIDE}
    
    private final MessageType type;
    private final int epoch;
    private final String value;
    private final String signature;
    
    // Constructor for creating new messages (signing required)
    public ConsensusMessage(MessageType type, int epoch, String value, PrivateKey privateKey) throws Exception {
        this.type = type;
        this.epoch = epoch;
        this.value = value;
        this.signature = signMessage(privateKey);
    }
    
    // Constructor for received messages (signature already exists)
    public ConsensusMessage(MessageType type, int epoch, String value, String signature) {
        this.type = type;
        this.epoch = epoch;
        this.value = value;
        this.signature = signature;
    }
    
    public MessageType getType() { return type; }
    public int getEpoch() { return epoch; }
    public String getValue() { return value; }
    public String getSignature() { return signature; }

    private String signMessage(PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update((type + "|" + epoch + "|" + value).getBytes());
        return Base64.getEncoder().encodeToString(signer.sign());
    }
    
    public boolean verifySignature(PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update((type + "|" + epoch + "|" + value).getBytes());
        return verifier.verify(Base64.getDecoder().decode(signature));
    }
    
    @Override
    public String toString() {
        return type + "|" + epoch + "|" + value + "|" + signature;
    }
}
