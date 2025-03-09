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
		this.socket = new DatagramSocket(port);
	}

	public void sendMessage(String message, InetAddress address, int targetPort) throws Exception {
		byte[] data = message.getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(data, data.length, address, targetPort);
		socket.send(packet);
	}

	public ConsensusMessage receiveMessage(PublicKey senderPublicKey) throws Exception {
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);

		String receivedData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
		String[] parts = receivedData.split("\\|");

		// Ensure correct format
		if (parts.length != 4) {
			System.out.println("Received malformed message: " + receivedData);
			return null;
		}

		// Extract message fields
		ConsensusMessage.MessageType type = ConsensusMessage.MessageType.valueOf(parts[0]);
		int epoch = Integer.parseInt(parts[1]);
		String value = parts[2];
		String signature = parts[3];

		// Create a ConsensusMessage object
		ConsensusMessage receivedMessage = new ConsensusMessage(type, epoch, value, signature);

		// Verify signature
		if (receivedMessage.verifySignature(senderPublicKey)) {
			System.out.println("✅ Message verified: " + receivedMessage.toString());
			return receivedMessage;
		} else {
			System.out.println("❌ WARNING: Received a tampered message!");
			return null;
		}
	}
}
