package consensus;

import java.util.Base64;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import communication.AuthenticatedPerfectLink;

public class ConsensusNode {
	private final int nodeId;
	private Map<Integer, AuthenticatedPerfectLink> apls; // (nodeID, apl)
	private Map<Integer, String> writeSet;
	private Object[] tsValue = { 0, "" };
	private final PublicKey publicKey;
	private final PrivateKey privateKey;

	/**
     * Creates a new ConsensusNode with the specified ID and port.
     * 
     * @param nodeId The ID of this node
     * @param port   The port to listen on
     */
    
    public ConsensusNode(int nodeId) throws Exception {
        
    	this.nodeId = nodeId;
        this.writeSet = new HashMap<>();
        
    	// Generate a key pair for signing and verifying messages
        KeyPair keyPair = generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        
        //escrever info no ficheiro
        
        
        try {
        createApls(3);
        }catch (Exception e){
        	apls = new HashMap<Integer, AuthenticatedPerfectLink>();
        	System.out.println("O ficheiro tem algo de errado");
        	e.printStackTrace();
        }
    }

	public int getNodeId() {
		return nodeId;
	}

	public Map<Integer, String> getWriteSet() {
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

	private void createApls(int nNodes) throws Exception {

		for (int i = 0; i < nNodes; i++) {
			if (i != nodeId) {
				Properties properties = new Properties();
				try (FileInputStream input = new FileInputStream("config.properties")) {
					properties.load(input);
					String address = properties.getProperty(i + ".address");
					String port = properties.getProperty(i + ".port");					
					int portInt = Integer.parseInt(port);
					InetSocketAddress addressAndPort = new InetSocketAddress(address, portInt);
					AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(nodeId , apls.get(i).getPort() , i , addressAndPort );
					apls.put(i, apl);
					System.out.println("Creating apl with IP: " + address + " and port " + port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
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
	public boolean verifySignature(String message, String receivedSignature, PublicKey senderPublicKey)
			throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initVerify(senderPublicKey);
		signature.update(message.getBytes());
		byte[] signatureBytes = Base64.getDecoder().decode(receivedSignature);
		return signature.verify(signatureBytes);
	}
}