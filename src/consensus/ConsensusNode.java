package consensus;

import java.util.HashMap;
import java.util.Map;
import communication.NetworkHandler;
import java.security.*;

public class ConsensusNode {
	protected final int nodeId;
    private final NetworkHandler networkHandler;
    private final Map<Integer, String> proposedValues;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public ConsensusNode(int nodeId, int port) throws Exception {
        this.nodeId = nodeId;
        this.networkHandler = new NetworkHandler(port);
        this.proposedValues = new HashMap<>();
        
     // Generate a key pair for signing and verifying messages
        KeyPair keyPair = generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }
    
    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048-bit key for security
        return keyGen.generateKeyPair();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    
    public void proposeValue(int epoch, String value) {
        proposedValues.put(epoch, value);
        System.out.println("Node " + nodeId + " proposed value for epoch " + epoch + ": " + value);
    }
    
    public void decideValue(int epoch) {
        if (proposedValues.containsKey(epoch)) {
            System.out.println("Node " + nodeId + " decided on value: " + proposedValues.get(epoch));
        } else {
            System.out.println("Node " + nodeId + " has no proposed value for epoch " + epoch);
        }
    }

    public String getProposedValue(int epoch) {
        return proposedValues.getOrDefault(epoch, null);
    }
}
