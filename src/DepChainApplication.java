import consensus.ConsensusNode;

import consensus.LeaderNode;
import blockchain.BlockchainService;
import config.MembershipConfig;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import consensus.ConditionalCollect;

/**
 * Main class to start a DepChain node with the appropriate ID and role.
 */

//REUTILIZADO PARA O MAIN DO SERVER
public class DepChainApplication {
	// Static configuration for the system membership
	private static final int LEADER_ID = 0;
	private static final int BASE_PORT = 10000;
	private static final int DEFAULT_NODE_COUNT = 4;
	private ConditionalCollect cc;
	

	// Store public keys of all nodes for verification
	//Para já, guardar aqui
	private static Map<Integer, java.security.PublicKey> memberPublicKeys = new HashMap<>();

	
	//Inicialização da membership feita, agora começar
	public static void main(String[] args) {
		try {
			
			

			LeaderNode leader = new LeaderNode(0, BASE_PORT);

			System.out.println("Starting DepChain with leader node " + leader.getNodeId() + " on port " + leader.getPort());

			// Initialize blockchain service
			BlockchainService blockchainService = new BlockchainService();

			// Initialize membership with size DEFAULT_NODE_COUNT
			MembershipConfig membership = new MembershipConfig(leader, DEFAULT_NODE_COUNT);

			// Start leader services
			startLeaderServices(leader, blockchainService);

			// For each member, add their public keys to the list
			for (int i = 0; i <= DEFAULT_NODE_COUNT; i++) {
				memberPublicKeys.put(membership.getNode(i).getNodeId(), membership.getNode(i).getPublicKey());
				System.out.println("The public key of the node " + membership.getNode(i).getNodeId() + "is: "
						+ membership.getNode(i).getPublicKey());
			}

			for (int i = 1; i <= DEFAULT_NODE_COUNT; i++) {
				// Start node services
				startNodeServices(membership.getNode(i), blockchainService);
			}
			// Start leader services
			startLeaderServices(leader, blockchainService);
			
			
			

		} catch (Exception e) {
			System.err.println("Could not create the Membership");
			System.exit(1);
		}
	}

	/**
	 * Start services for the leader node.
	 */
	// Por alterar para enviar um init
	private static void startLeaderServices(LeaderNode leader, BlockchainService blockchainService) {

		// Start a thread to handle leader-specific tasks
		new Thread(() -> {
			try {
				System.out.println("Leader services started on node " + leader.getNodeId());
				//INIT AQUI
				
			} catch (Exception e) {
				System.err.println("Error in leader services: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
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