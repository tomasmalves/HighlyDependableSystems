package config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the static system membership configuration for DepChain.
 */
public class MembershipConfig {
    // Constants for configuration
    private static final int LEADER_ID = 0;
    private static final int BASE_PORT = 10000;
    private static final int DEFAULT_NODE_COUNT = 4;

    // Membership information
    private final Map<Integer, NodeInfo> nodeInfo = new HashMap<>();
    private final int localNodeId;

    /**
     * Creates a new MembershipConfig for the specified local node.
     * 
     * @param localNodeId The ID of the local node
     */
    public MembershipConfig(int localNodeId) throws Exception {
        this.localNodeId = localNodeId;
        initializeNodes(DEFAULT_NODE_COUNT);
    }

    /**
     * Creates a new MembershipConfig with a custom node count.
     * 
     * @param localNodeId The ID of the local node
     * @param nodeCount   The total number of nodes in the system
     */
    public MembershipConfig(int localNodeId, int nodeCount) throws Exception {
        this.localNodeId = localNodeId;
        initializeNodes(nodeCount);
    }

    /**
     * Initialize all nodes in the system.
     * 
     * @param nodeCount The total number of nodes to initialize
     */
    private void initializeNodes(int nodeCount) throws Exception {
        for (int i = 0; i < nodeCount; i++) {
            // Generate key pair for this node
            KeyPair keyPair = generateKeyPair();

            // Create node info and store it
            NodeInfo info = new NodeInfo(
                    i,
                    InetAddress.getLocalHost(),
                    BASE_PORT + i,
                    keyPair.getPublic(),
                    i == LEADER_ID);

            nodeInfo.put(i, info);
        }
    }

    /**
     * Generate a key pair for cryptographic operations.
     */
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * Get information about the specified node.
     * 
     * @param nodeId The ID of the node
     * @return The NodeInfo for the specified node
     */
    public NodeInfo getNodeInfo(int nodeId) {
        return nodeInfo.get(nodeId);
    }

    /**
     * Get information about the local node.
     * 
     * @return The NodeInfo for the local node
     */
    public NodeInfo getLocalNodeInfo() {
        return nodeInfo.get(localNodeId);
    }

    /**
     * Get information about the leader node.
     * 
     * @return The NodeInfo for the leader node
     */
    public NodeInfo getLeaderInfo() {
        return nodeInfo.get(LEADER_ID);
    }

    /**
     * Check if the local node is the leader.
     * 
     * @return true if the local node is the leader, false otherwise
     */
    public boolean isLeader() {
        return localNodeId == LEADER_ID;
    }

    /**
     * Get the number of nodes in the system.
     * 
     * @return The number of nodes
     */
    public int getNodeCount() {
        return nodeInfo.size();
    }

    /**
     * Get all node information.
     * 
     * @return A map of node IDs to NodeInfo objects
     */
    public Map<Integer, NodeInfo> getAllNodeInfo() {
        return new HashMap<>(nodeInfo);
    }

    /**
     * Represents information about a node in the system.
     */
    public static class NodeInfo {
        private final int id;
        private final InetAddress address;
        private final int port;
        private final PublicKey publicKey;
        private final boolean isLeader;

        /**
         * Creates a new NodeInfo.
         * 
         * @param id        The node ID
         * @param address   The node's network address
         * @param port      The node's network port
         * @param publicKey The node's public key
         * @param isLeader  Whether this node is the leader
         */
        public NodeInfo(int id, InetAddress address, int port, PublicKey publicKey, boolean isLeader) {
            this.id = id;
            this.address = address;
            this.port = port;
            this.publicKey = publicKey;
            this.isLeader = isLeader;
        }

        // Getters
        public int getId() {
            return id;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public boolean isLeader() {
            return isLeader;
        }

        @Override
        public String toString() {
            return "NodeInfo{" +
                    "id=" + id +
                    ", address=" + address +
                    ", port=" + port +
                    ", isLeader=" + isLeader +
                    '}';
        }
    }
}