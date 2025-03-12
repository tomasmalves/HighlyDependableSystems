package consensus;

import java.util.HashMap;
import java.util.Map;
import communication.NetworkHandler;
import config.MembershipConfig;
import java.security.*;

public class ConsensusNode {
    private final int nodeId;
    private final NetworkHandler networkHandler;
    private final Map<Integer, String> proposedValues;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final MembershipConfig membershipConfig;

    /**
     * Creates a new ConsensusNode with the specified ID and port.
     * 
     * @param nodeId The ID of this node
     * @param port   The port to listen on
     */
    public ConsensusNode(int nodeId, int port) throws Exception {
        this.nodeId = nodeId;
        this.networkHandler = new NetworkHandler(port);
        this.proposedValues = new HashMap<>();

        // Generate a key pair for signing and verifying messages
        KeyPair keyPair = generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();

        // Initialize membership configuration
        this.membershipConfig = new MembershipConfig(nodeId);

        System.out.println("ConsensusNode initialized with ID: " + nodeId);
        System.out.println("Total nodes in system: " + membershipConfig.getNodeCount());
        System.out.println("Leader node: " + membershipConfig.getLeaderInfo().getId());

        // Log if this node is the leader
        if (membershipConfig.isLeader()) {
            System.out.println("This node is the LEADER");
        } else {
            System.out.println("This node is a FOLLOWER");
        }
    }

    public int getNodeId() {
        return nodeId;
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

    /**
     * Get the membership configuration.
     * 
     * @return The membership configuration
     */
    public MembershipConfig getMembershipConfig() {
        return membershipConfig;
    }

    /**
     * Get the network handler.
     * 
     * @return The network handler
     */
    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    /**
     * Start the consensus protocol.
     */
    public void start() {
        System.out.println("Node " + nodeId + " starting consensus protocol...");
        // Implementation of the consensus protocol will go here
        // send to leader
    }
}