package consensus;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import communication.NetworkHandler;
import config.MembershipConfig;
import java.security.*;
import communication.AuthenticatedPerfectLink;


//DONE para fase 1 do projeto
//Na fase 2 do projeto, adicionar writeSet e alterar value para <ts, val>
public class ConsensusNode {
    private final int nodeId;
    private final AuthenticatedPerfectLink apl;
    private final Map<Integer, String> proposedValue;
    
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    /**
     * Creates a new ConsensusNode with the specified ID and port.
     * 
     * @param nodeId The ID of this node
     * @param port   The port to listen on
     */
    
    public ConsensusNode(int nodeId, int port) throws Exception {
        
    	this.nodeId = nodeId;
    	
    	//alterar na fase 2 do projeto
        this.proposedValue = new HashMap<>();
        
    	// Generate a key pair for signing and verifying messages
        KeyPair keyPair = generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        
        this.apl = new AuthenticatedPerfectLink(nodeId, port, publicKey.toString());

    }

    public int getNodeId() {
        return nodeId;
    }
    
    public int getPort() {
    	return apl.getPort();
    }
    
    public Map<Integer, String> getProposedValue(){
    	return proposedValue;
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
    
    public void sendMessage(int targetPort, String message) throws Exception {
        apl.send(message, "localhost", targetPort);
    }
    
    public String receiveMessage() throws Exception {
        return apl.receive();
    }
}