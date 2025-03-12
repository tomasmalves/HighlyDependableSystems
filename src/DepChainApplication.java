import consensus.ConsensusNode;
import consensus.LeaderNode;
import blockchain.BlockchainService;
import communication.NetworkHandler;
import config.MembershipConfig;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class to start a DepChain node with the appropriate ID and role.
 */
public class DepChainApplication {
    // Static configuration for the system membership
    private static final int LEADER_ID = 0;
    private static final int BASE_PORT = 10000;

    // Store public keys of all nodes for verification
    private static final Map<Integer, java.security.PublicKey> memberPublicKeys = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            if (args.length < 1) {
                System.err.println("Usage: DepChainApplication <nodeId>");
                System.exit(1);
            }

            int nodeId = Integer.parseInt(args[0]);
            int port = BASE_PORT + nodeId;

            System.out.println("Starting DepChain node " + nodeId + " on port " + port);

            // Initialize blockchain service
            BlockchainService blockchainService = new BlockchainService();
            MembershipConfig config = new MembershipConfig(nodeId);

            // Create the appropriate node type based on ID
            if (nodeId == LEADER_ID) {
                // Leader node (which is always correct according to project requirements)
                LeaderNode leaderNode = new LeaderNode(nodeId, port);
                System.out.println("Initialized as LEADER node with ID: " + nodeId);

                // Store leader's public key for verification by other nodes
                memberPublicKeys.put(nodeId, leaderNode.getPublicKey());

                // Initialize connections to all other nodes
                initializeLeaderConnections(leaderNode);

                // Start leader services
                startLeaderServices(leaderNode, blockchainService);
            } else {
                // Regular consensus node
                ConsensusNode node = new ConsensusNode(nodeId, port);
                System.out.println("Initialized as CONSENSUS node with ID: " + nodeId);

                // Store node's public key
                memberPublicKeys.put(nodeId, node.getPublicKey());

                // Connect to leader
                connectToLeader(node);

                // Start node services
                startNodeServices(node, blockchainService);
            }

            // Keep the application running
            System.out.println("Node " + nodeId + " is running...");

            // Register shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Node " + nodeId + " shutting down...");
                // Perform cleanup here if needed
            }));

            // Add this block to prevent the main thread from exiting
            try {
                // This will keep the main thread alive until interrupted
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                System.out.println("Node interrupted, shutting down...");
            }

        } catch (NumberFormatException e) {
            System.err.println("Node ID must be an integer");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error starting node: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Initialize leader connections to all other nodes in the system.
     */
    private static void initializeLeaderConnections(LeaderNode leader) throws Exception {
        System.out.println("Initializing leader connections to follower nodes...");

        // For simplifications purposes, we'll use a fixed number of nodes
        int totalNodes = 4; // Including leader

        for (int i = 1; i < totalNodes; i++) {
            int followerPort = BASE_PORT + i;
            System.out.println("Leader connecting to node " + i + " at port " + followerPort);
        }
    }

    /**
     * Start services for the leader node.
     */
    private static void startLeaderServices(LeaderNode leader, BlockchainService blockchainService) {
        // Start a thread to handle leader-specific tasks
        new Thread(() -> {
            try {
                System.out.println("Leader services started on node " + leader.getNodeId());

                // For demo purposes, propose a sample value
                leader.proposeValue(1, "Genesis Block");
                blockchainService.appendBlock("Genesis Block");

            } catch (Exception e) {
                System.err.println("Error in leader services: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Connect this node to the leader.
     */
    private static void connectToLeader(ConsensusNode node) throws Exception {
        int leaderPort = BASE_PORT + LEADER_ID;
        System.out.println("Node " + node.getNodeId() + " connecting to leader at port " + leaderPort);
    }

    /**
     * Start services for a regular consensus node.
     */
    private static void startNodeServices(ConsensusNode node, BlockchainService blockchainService) {
        // Start a thread to handle node-specific tasks
        new Thread(() -> {
            try {
                System.out.println("Node services started on node " + node.getNodeId());
            } catch (Exception e) {
                System.err.println("Error in node services: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}