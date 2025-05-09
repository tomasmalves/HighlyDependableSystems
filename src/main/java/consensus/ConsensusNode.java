package consensus;

import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.evm.worldstate.WorldState;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.Long;
import java.math.BigInteger;

import communication.AuthenticatedPerfectLink;
import communication.Message;
import communication.MessageType;
import communication.DeliverCallback;
import blockchain.Block;
import blockchain.Blockchain;
import blockchain.GenesisBlockLoader;
import blockchain.Transaction;

public class ConsensusNode implements DeliverCallback {

	private final int nodeId;
	private DatagramSocket clientSocket;
	private InetAddress inetAddress;
	private byte[] buffer = new byte[1024];
	private AuthenticatedPerfectLink apl;
	private Map<Long, String> writeSet;
	private Map<Long, String> tsValue = new HashMap<>();
	private PublicKey publicKey;
	private PrivateKey privateKey;
	private final ByzantineReadWriteConsensus consensus;
	private Map<String, ClientInfo> activeClients;
	private boolean isRunning = true;
	private List<Transaction> transactionsPool;
	private Blockchain blockchain;

	public ConsensusNode(int nodeId, InetAddress inetAddress) throws Exception {
		this.nodeId = nodeId;
		this.inetAddress = inetAddress;
		this.writeSet = new HashMap<>();
		this.activeClients = new HashMap<>();
		this.tsValue.put(0L, "");
		this.transactionsPool = new LinkedList<>();
		this.blockchain = new Blockchain();

		// Create the client-facing socket
		this.clientSocket = new DatagramSocket(5000 + nodeId);

		// Load keys from membership.json
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("communication/membership.json");
		if (inputStream == null) {
			throw new FileNotFoundException("membership.json not found in resources");
		}

		String jsonContent;
		try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
			jsonContent = scanner.useDelimiter("\\A").next(); // Read entire content
		}

		JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
		JsonArray nodes = root.getAsJsonArray("nodes");

		for (int i = 0; i < nodes.size(); i++) {
			JsonObject nodeObj = nodes.get(i).getAsJsonObject();
			if (nodeObj.get("id").getAsInt() == nodeId) {
				// Decode public key
				String publicKeyStr = nodeObj.get("publicKey").getAsString();
				byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
				X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				this.publicKey = keyFactory.generatePublic(pubKeySpec);

				// Decode private key
				String privateKeyStr = nodeObj.get("privateKey").getAsString();
				byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
				PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
				this.privateKey = keyFactory.generatePrivate(privKeySpec);

				break;
			}
		}

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
		this.consensus = new ByzantineReadWriteConsensus(nodeId, leaderId, processList, maxByzantine, apl, privateKey,
				publicKeys, blockchain);

		// Register callback for consensus decisions
		this.consensus.registerDecideCallback(value -> {
			try {
				onConsensusDecide(value);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		// Start communication layer
		this.apl.start();
	}

	// Add method to handle consensus decisions
	private void onConsensusDecide(String decidedValue) throws Exception {
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

				// Convert the result string into a Transaction object
				Transaction tx = parseTransactionFromString(result);
				if (tx == null) {
					System.err.println("Failed to parse transaction from decided value.");
					return; // confirmar se dar return é a melhor opção
				}

				// Create a new block with the transaction
				List<Transaction> transactions = new ArrayList<>();
				transactions.add(tx);

				if (tx.getData() == null || tx.getData().isEmpty()) {
					System.out.println("Creating block for DepCoin transfer.");
				} else {
					System.out.println("Creating block for ISTCoin (smart contract) transfer.");
				}

				blockchain.addBlock(transactions);

				// Remove client from active list after handling
				activeClients.remove(clientId);
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

		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("communication/membership.json");
			if (inputStream == null) {
				throw new FileNotFoundException("membership.json not found in resources");
			}

			String jsonContent;
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				jsonContent = scanner.useDelimiter("\\A").next(); // Read entire content
			}

			JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
			JsonArray nodes = root.getAsJsonArray("nodes");

			for (int i = 0; i < nodes.size(); i++) {
				JsonObject nodeObj = nodes.get(i).getAsJsonObject();

				int id = nodeObj.get("id").getAsInt();
				String address = nodeObj.get("address").getAsString();
				int port = nodeObj.get("port").getAsInt();

				// Load public key
				PublicKey publicKey;
				if (id == nodeId) {
					publicKey = this.publicKey;
				} else {
					String publicKeyStr = nodeObj.get("publicKey").getAsString();
					byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
					X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
					KeyFactory keyFactory = KeyFactory.getInstance("RSA");
					publicKey = keyFactory.generatePublic(keySpec);
				}

				processMap.put(id, new ProcessInfo(address, port, publicKey));
			}
		} catch (Exception e) {
			System.err.println("Failed to load membership JSON config: " + e.getMessage());
			e.printStackTrace();
		}

		return processMap;
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
					System.out.println("Node " + nodeId + " received from client: " + messageFromClient + " from "
							+ clientAddress + ":" + clientPort);

					// Split the message using '|'
					String[] parts = messageFromClient.split("\\|");

					if (parts.length < 6) {
						System.out.println("Invalid message format. Skipping...");
						continue;
					}

					String from = parts[0];
					String to = parts[1];
					BigInteger value = BigInteger.valueOf(Integer.parseInt(parts[2]));
					long nonce = Long.valueOf(parts[3]);
					long timestamp = Long.valueOf(parts[4]);
					Bytes data = Bytes.fromHexString(parts[5]);
					String signature = parts[6];

					// Build transaction object
					Transaction transaction = new Transaction(from, to, value, nonce, data, timestamp);

					// ???
					transaction.setSignature(signature.getBytes());

					System.out.println("Parsed transaction: " + transaction.toString());

					// Add to transactions waiting list
					transactionsPool.add(transaction);

					// Generate a unique client ID
					String clientId = clientAddress.getHostAddress() + ":" + clientPort;

					String valueForConsensus = clientId + "/" + transaction.toString();

					// Store client info for later response
					activeClients.put(clientId, new ClientInfo(clientAddress, clientPort));
					System.out.println("Active clients: " + activeClients.size());
					System.out.println("ts - " + ts);
					this.tsValue.put(ts, valueForConsensus);

					// Start the consensus algorithm
					if (transactionsPool.size() >= 1) {
						System.out.println("Node " + nodeId + " is the leader. Proposing value: " + valueForConsensus);
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
			DatagramPacket sendPacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddress,
					clientPort);

			System.out.println("Node " + nodeId + " sending response to: " + clientAddress + ":" + clientPort);
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Transaction parseTransactionFromString(String txString) {
		try {
			// Remove the prefix "Transaction{" and suffix "}"
			txString = txString.replace("Transaction{", "").replace("}", "");

			Map<String, String> values = new HashMap<>();
			String[] fields = txString.split(", ");
			for (String field : fields) {
				String[] kv = field.split("=", 2);
				if (kv.length == 2) {
					values.put(kv[0].trim(), kv[1].trim());
				}
			}

			String from = values.get("from").replace("'", "");
			String to = values.get("to").replace("'", "");
			BigInteger value = new BigInteger(values.get("value"));
			long nonce = Long.parseLong(values.get("nonce"));
			String dataStr = values.get("data");
			long timestamp = Long.parseLong(values.get("timestamp"));

			Bytes data = null;
			if (dataStr != null && !dataStr.equalsIgnoreCase("")) {
				data = Bytes.fromHexString(dataStr);
			}

			return new Transaction(from, to, value, nonce, data, timestamp);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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