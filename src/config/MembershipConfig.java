package config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import consensus.ConsensusNode;

//DONE
public class MembershipConfig {
    // Constants for configuration
    private static final int LEADER_ID = 0;
    private static final int BASE_PORT = 10000;
    private static final int DEFAULT_NODE_COUNT = 4;

    // Membership information
    private List<ConsensusNode> membership = new ArrayList<ConsensusNode>();
    private final ConsensusNode localNode;

    /**
     * Creates a new MembershipConfig for the specified local node.
     * 
     * @param localNodeId The ID of the local node
     */
    public MembershipConfig(ConsensusNode localNode) throws Exception {
        this.localNode = localNode;
        membership.add(localNode);
    }

    /**
     * Creates a new MembershipConfig with a custom node count.
     * 
     * @param localNodeId The ID of the local node
     * @param nodeCount   The total number of nodes in the system
     */
    public MembershipConfig(ConsensusNode localNode, int nodeCount) throws Exception {
        this.localNode = localNode;
        initializeNodes(nodeCount);
    }

    /**
     * Initialize all nodes in the system.
     * 
     * @param nodeCount The total number of nodes to initialize
     */
    private void initializeNodes(int nodeCount) throws Exception {
        for (int i = 1; i <= nodeCount; i++) {
        	membership.add(new ConsensusNode(i, BASE_PORT + i));
        }
    }

    public int getNodeCount() {
        return membership.size();
    }

    /**
     * Get all node information.
     * 
     * @return A consensus node from the membership list, based on the index
     */
    public ConsensusNode getNode(int index) {
        return membership.get(index);
    }

}