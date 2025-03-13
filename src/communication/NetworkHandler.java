package communication;

import consensus.ConsensusMessage;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;

public class NetworkHandler {
	private final DatagramSocket socket;
	private final int port;

	public NetworkHandler(int port) throws Exception {
		this.port = port;
		this.socket = new DatagramSocket(port);  // Open the socket to listen for incoming messages
	}

	// Send a message to a specific address and port
	public void sendMessage(String message, InetAddress address, int targetPort) throws Exception {
		byte[] data = message.getBytes(StandardCharsets.UTF_8);  // Convert message to bytes
		DatagramPacket packet = new DatagramPacket(data, data.length, address, targetPort);
		socket.send(packet);  // Send the message via UDP
	}

	// Receive a message, validate it using the sender's public key, and return a ConsensusMessage
	public ConsensusMessage receiveMessage(PublicKey senderPublicKey) throws Exception {
		byte[] buffer = new byte[2048];  // Increased buffer size to 2048 bytes for larger messages
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);  // Wait to receive a message

		String receivedData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
		String[] parts = receivedData.split("\\|");

		// Ensure correct format, handle malformed messages
		if (parts.length != 4) {
			System.out.println("Received malformed message: " + receivedData);
			return null;
		}

		// Extract the message components
		ConsensusMessage.MessageType type = ConsensusMessage.MessageType.valueOf(parts[0]);
		int epoch = Integer.parseInt(parts[1]);
		String value = parts[2];
		String signature = parts[3];

		// Create the message object
		ConsensusMessage receivedMessage = new ConsensusMessage(type, epoch, value, signature);

		// Verify the message signature to ensure integrity
		if (receivedMessage.verifySignature(senderPublicKey)) {
			System.out.println("✅ Message verified: " + receivedMessage.toString());
			return receivedMessage;
		} else {
			System.out.println("❌ WARNING: Received a tampered message!");
			return null;
		}
	}

	// Method to broadcast a message to all peers in the network
	public void broadcast(ConsensusMessage message, InetAddress[] peers, int port) throws Exception {
		for (InetAddress peer : peers) {
			sendMessage(message.toString(), peer, port);  // Send the message to each peer
		}
	}
}
