package consensus;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.lang.Long;

import communication.AuthenticatedPerfectLink;
import communication.Message;
import communication.MessageType;
import communication.DeliverCallback;

public class ConsensusNode implements DeliverCallback {

	private final int nodeId;
	private DatagramSocket clientSocket;
	private InetAddress inetAddress;
	private byte[] buffer = new byte[1024];
	private AuthenticatedPerfectLink apl;
	private Map<Long, String> writeSet;
	private Map<Long, String> tsValue = new HashMap<>();
	private final PublicKey publicKey;
	private final PrivateKey privateKey;
	private final ByzantineReadWriteConsensus consensus;
	private Map<String, ClientInfo> activeClients;
	private boolean isRunning = true;

	public ConsensusNode(int nodeId, InetAddress inetAddress) throws Exception {
		this.nodeId = nodeId;
		this.inetAddress = inetAddress;
		this.writeSet = new HashMap<>();
		this.activeClients = new HashMap<>();
		this.tsValue.put(0L, "");

		// Create the client-facing socket
		this.clientSocket = new DatagramSocket(5000 + nodeId);

		// Generate or load keys
		KeyPair keyPair = generateKeyPair();
		this.publicKey = keyPair.getPublic();
		this.privateKey = keyPair.getPrivate();

		// Create map for process information
		Map<Integer, ProcessInfo> processInfoMap = loadProcessInfo();

		// Initialize AuthenticatedPerfectLink
		this.apl = new AuthenticatedPerfectLink(nodeId, processInfoMap, privateKey, 6000 + nodeId);
		this.apl.registerDeliverCallback(this);

		// Create the ByzantineReadWriteConsensus instance
		List<Integer> processList = new ArrayList<>(processInfoMap.keySet());
		int maxByzantine = (processList.size() - 1) / 3; // f = (n-1)/3 for BFT

		// Get all public keys
		Map<Integer, PublicKey> publicKeys = new HashMap<>();
		for (Map.Entry<Integer, ProcessInfo> entry : processInfoMap.entrySet()) {
			publicKeys.put(entry.getKey(), entry.getValue().getPublicKey());
		}

		// Choose leader (for simplicity, process 1 is leader)
		int leaderId = 1;

		// Initialize consensus
		this.consensus = new ByzantineReadWriteConsensus(
				nodeId, leaderId, processList, maxByzantine, apl, privateKey, publicKeys);

		// Register callback for consensus decisions
		this.consensus.registerDecideCallback(this::onConsensusDecide);

		// Start communication layer
		this.apl.start();
	}

	// Add method to handle consensus decisions
	private void onConsensusDecide(String decidedValue) {
		System.out.println("Node " + nodeId + " decided value: " + decidedValue);

		// Parse the client ID from the decided value (format: "clientId/message")
		String[] parts = decidedValue.split("/", 2);
		if (parts.length == 2) {
			String clientId = parts[0];
			String result = parts[1];

			System.out.println("CLIENTID - " + clientId);
			// Find the client info
			ClientInfo clientInfo = activeClients.get(clientId);
			if (clientInfo != null) {
				// Report result back to the client
				System.out.println("REPORTING - " + result);
				reportToClient(result, clientInfo.getAddress(), clientInfo.getPort());
				// Remove client from active list after handling
				// activeClients.remove(clientId);
			}
		}
	}

	@Override
	public void onDeliver(Message message, int senderId) {
		// Handle delivered messages from the APL
		System.out.println("Node " + nodeId + " received message from node " + senderId);
	}

	// Fix the loadProcessInfo method
	private Map<Integer, ProcessInfo> loadProcessInfo() {
		Map<Integer, ProcessInfo> processMap = new HashMap<>();
		Properties properties = new Properties();

		try (FileInputStream input = new FileInputStream(
				"/home/ubunto/Desktop/sec/project/HighlyDependableSystems/src/communication/membership.properties")) {
			properties.load(input);

			// Get the number of nodes
			int nodeCount = Integer.parseInt(properties.getProperty("node.count", "4"));

			for (int i = 1; i <= nodeCount; i++) {
				String host = properties.getProperty(i + ".address", "localhost");
				int port = Integer.parseInt(properties.getProperty(i + ".port"));

				// Load or generate public key for this process
				PublicKey publicKey;
				if (i == nodeId) {
					publicKey = this.publicKey;
				} else {
					publicKey = loadPublicKeyForNode(i);
				}

				processMap.put(i, new ProcessInfo(host, port, publicKey));
			}
		} catch (IOException e) {
			System.err.println("Failed to load membership configuration: " + e.getMessage());
			// For testing, create default configuration
			for (int i = 1; i <= 4; i++) {
				PublicKey publicKey;
				if (i == nodeId) {
					publicKey = this.publicKey;
				} else {
					publicKey = loadPublicKeyForNode(i);
				}
				processMap.put(i, new ProcessInfo("localhost", 6000 + i, publicKey));
			}
		}

		return processMap;
	}

	// Method to load public keys (for testing, you might generate them)
	private PublicKey loadPublicKeyForNode(int nodeId) {
		// In a real system, you would load this from a keystore or a file
		try {
			// Simple approach for testing: create keys for all nodes at startup
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			return keyGen.generateKeyPair().getPublic();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create public key for node " + nodeId, e);
		}
	}

	/**
	 * Start listening for client requests
	 */
	public void listenForClientRequests() {
		Thread clientListener = new Thread(() -> {
			System.out.println("Node " + nodeId + " listening for client requests on port " + (5000 + nodeId));

			Long ts = 0L;

			while (isRunning) {
				try {
					// Create buffer for receiving
					buffer = new byte[1024];
					DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

					// Receive the message
					clientSocket.receive(receivePacket);
					InetAddress clientAddress = receivePacket.getAddress();
					int clientPort = receivePacket.getPort();

					String messageFromClient = new String(receivePacket.getData(), 0, receivePacket.getLength());
					System.out.println("Node " + nodeId + " received from client: " + messageFromClient +
							" from " + clientAddress + ":" + clientPort);

					// Generate a unique client ID
					String clientId = clientAddress.getHostAddress() + ":" + clientPort;

					String valueForConsensus = clientId + "/" + messageFromClient;

					// Store client info for later response
					activeClients.put(clientId, new ClientInfo(clientAddress, clientPort));
					System.out.println("Active clients: " + activeClients.size());
					System.out.println("ts - " + ts);
					this.tsValue.put(ts, valueForConsensus);

					// Start the consensus algorithm
					if (nodeId == 1) { // Assuming node 1 is the leader
						System.out.println("Node " + nodeId + " is the leader. Proposing value: " + valueForConsensus);
						consensus.init(this.tsValue.get(ts), writeSet);
						consensus.start();
						ts++;
					} else {
						System.out.println("Node " + nodeId + " participating in consensus");
						consensus.init(this.tsValue.get(ts), writeSet);
						consensus.start();
						ts++;
					}

				} catch (IOException e) {
					if (isRunning) {
						e.printStackTrace();
					}
				}
			}
		});

		clientListener.setDaemon(true);
		clientListener.start();
	}

	public void reportToClient(String response, InetAddress clientAddress, int clientPort) {
		try {
			// Create response message
			byte[] responseBytes = response.getBytes();

			// Create new packet for sending the response
			DatagramPacket sendPacket = new DatagramPacket(
					responseBytes,
					responseBytes.length,
					clientAddress,
					clientPort);

			System.out.println("Node " + nodeId + " sending response to: " + clientAddress + ":" + clientPort);
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048); // 2048-bit key for security
		return keyGen.generateKeyPair();
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

	public void shutdown() {
		isRunning = false;
		if (clientSocket != null) {
			clientSocket.close();
		}
		apl.stop();
	}

	// Helper class to track client information
	private static class ClientInfo {
		private final InetAddress address;
		private final int port;

		public ClientInfo(InetAddress address, int port) {
			this.address = address;
			this.port = port;
		}

		public InetAddress getAddress() {
			return address;
		}

		public int getPort() {
			return port;
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				System.out.println("Usage: java ConsensusNode <nodeId>");
				System.exit(1);
			}

			int nodeId = Integer.parseInt(args[0]);
			InetAddress inetAddress = InetAddress.getByName("localhost");

			ConsensusNode node = new ConsensusNode(nodeId, inetAddress);

			// Start listening for client requests
			node.listenForClientRequests();

			// Keep the main thread alive
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down node " + nodeId);
				node.shutdown();
			}));

			// Wait indefinitely
			Thread.currentThread().join();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}