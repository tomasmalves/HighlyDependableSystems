package test;

import communication.NetworkHandler;
import consensus.ConsensusMessage;
import java.net.InetAddress;
import java.security.*;

public class NetworkTest {

	public static void main(String[] args) throws Exception {
		// Generate key pairs for sender and receiver
		KeyPair senderKeyPair = generateKeyPair();
		KeyPair receiverKeyPair = generateKeyPair();

		// Create NetworkHandlers
		NetworkHandler sender = new NetworkHandler(5001);
		NetworkHandler receiver = new NetworkHandler(5002);

		// Create a signed message
		String testMessage = "Test Message";
		ConsensusMessage signedMessage = new ConsensusMessage(ConsensusMessage.MessageType.PROPOSE, 1, testMessage, senderKeyPair.getPrivate());

		// Send the signed message
		sender.sendMessage(signedMessage.toString(), InetAddress.getByName("localhost"), 5002);

		// Receive and verify the message
		ConsensusMessage receivedMessage = receiver.receiveMessage(senderKeyPair.getPublic());

		if (receivedMessage != null && testMessage.equals(receivedMessage.getValue())) {
			System.out.println("✅ Test Passed: Message received and verified successfully.");
		} else {
			System.out.println("❌ Test Failed: Received an invalid or tampered message.");
		}
	}

	// Helper function to generate an RSA key pair
	private static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		return keyGen.generateKeyPair();
	}
}
