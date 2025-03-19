package consensus;

import java.util.Base64;

import java.util.HashMap;
import java.util.Map;
import java.security.*;
import communication.AuthenticatedPerfectLink;

public class ConsensusNode {
    private final int nodeId;
    private final AuthenticatedPerfectLink apl;
    private Map<Integer, String> writeSet;
    private Object[] tsValue = {0, ""};
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    /**
     * Creates a new ConsensusNode with the specified ID and port.
     * 
     * @param nodeId The ID of this node
     * @param port   The port to listen on
     */
    
    public ConsensusNode(int nodeId, AuthenticatedPerfectLink apl) throws Exception {
        
    	this.nodeId = nodeId;
        this.writeSet = new HashMap<>();
        
    	// Generate a key pair for signing and verifying messages
        KeyPair keyPair = generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        
        this.apl = apl;;

    }

    public int getNodeId() {
        return nodeId;
    }
    
    public int getPort() {
    	return apl.getPort();
    }
    
    public Map<Integer, String> getWriteSet(){
    	return writeSet;
    }

    public Object[] getTsValue() {
    	return tsValue;
    }
    
    public void setTsValue(int ts, String string) {
    	tsValue[0] = ts;
    	tsValue[1] = string;
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
    
    /**
     * Signs a message using the node's private key.
     */
    public String signMessage(String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * Verifies a message signature using the sender's public key.
     */
    public boolean verifySignature(String message, String receivedSignature, PublicKey senderPublicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(senderPublicKey);
        signature.update(message.getBytes());
        byte[] signatureBytes = Base64.getDecoder().decode(receivedSignature);
        return signature.verify(signatureBytes);
    }
    
}