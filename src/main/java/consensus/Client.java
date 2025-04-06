package consensus;

import java.io.IOException;
import java.math.BigInteger;

import blockchain.Transaction;
import util.CryptoUtil;
import accounts.EOAccount;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.google.gson.Gson;

public class Client {
	private DatagramSocket datagramSocket;
	private InetAddress inetAddress;
	private EOAccount account;

	public Client(DatagramSocket datagramSocket, InetAddress inetAddress, EOAccount account) {
		this.datagramSocket = datagramSocket;
		this.inetAddress = inetAddress;
		this.account = account;
	}

	public void sendToConsensus() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Send a transaction to the consensus system (type 'exit' at any prompt to quit).");

		while (true) {
			try {
				System.out.print("Wallet Destination: ");
				String destination = scanner.nextLine();
				if ("exit".equalsIgnoreCase(destination))
					break;

				System.out.print("ISTCoin to transfer: ");
				String amountInput = scanner.nextLine();
				if ("exit".equalsIgnoreCase(amountInput))
					break;

				BigInteger amount = null;
				Bytes data = null;

				if (amountInput.isEmpty()) {
					amount = BigInteger.valueOf(10L);
					data = Bytes.fromHexString("");
				} else {
					amount = BigInteger.valueOf(0L);

					// transfer tokens to user destination
					String transferBackData = "a9059cbb" +
							padHexStringTo256Bit(this.account.getAddress().substring(2)) +
							convertIntegerToHex256Bit(Integer.parseInt(amountInput));

					data = Bytes.fromHexString(transferBackData);
				}

				Long nonce = account.getNonce();
				account.incrementNonce();

				Transaction transaction = new Transaction(this.account.getAddress(), destination, amount, nonce, data,
						System.currentTimeMillis());

				// Sign the transaction with the sender's private key
				transaction.sign(this.account.getPrivateKey());

				// Serialize the transaction to a format suitable for network transmission
				// Option 1: Format as pipe-delimited string for simple parsing
				String txString = transaction.getFrom() + "|" + transaction.getTo() + "|" +
						transaction.getValue().toString() + "|" + transaction.getNonce() + "|" +
						transaction.getTimestamp() + "|" + transaction.getData() + "|" +
						CryptoUtil.bytesToHex(transaction.getSignature());

				// Option 2: Use transaction.toMap() and convert to JSON
				// String txJson = new Gson().toJson(transaction.toMap());

				byte[] sendBuffer = txString.getBytes();

				// Send to nodes
				DatagramPacket packet1 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5001);
				DatagramPacket packet2 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5002);
				DatagramPacket packet3 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5003);
				DatagramPacket packet4 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5004);

				datagramSocket.send(packet1);
				datagramSocket.send(packet2);
				datagramSocket.send(packet3);
				datagramSocket.send(packet4);

				System.out.println("Transaction sent to all nodes.");

				// Set timeout and listen for responses
				datagramSocket.setSoTimeout(15000);

				try {
					for (int i = 1; i <= 4; i++) {
						byte[] receiveBuffer = new byte[1024];
						DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
						try {
							datagramSocket.receive(receivePacket);
							String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
							System.out.println("Response from " + receivePacket.getAddress() + ":" +
									receivePacket.getPort() + " - " + response);
						} catch (java.net.SocketTimeoutException e) {
							System.out.println("Timeout waiting for response from node " + i);
							break;
						}
					}
				} finally {
					datagramSocket.setSoTimeout(0);
				}

			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace();
				break;
			}
		}

		scanner.close();
	}

	private static String calculateMappingKey(String address, int mappingSlot) {
		if (address.startsWith("0x")) {
			address = address.substring(2);
		}

		String paddedAddress = padHexStringTo256Bit(address);
		String slotIndex = convertIntegerToHex256Bit(mappingSlot);

		return Numeric.toHexStringNoPrefix(
				Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + slotIndex)));
	}

	public static String convertIntegerToHex256Bit(int number) {
		BigInteger bigInt = BigInteger.valueOf(number);
		return String.format("%064x", bigInt);
	}

	public static String padHexStringTo256Bit(String hexString) {
		if (hexString.startsWith("0x")) {
			hexString = hexString.substring(2);
		}

		int length = hexString.length();
		int targetLength = 64;

		if (length >= targetLength) {
			return hexString.substring(0, targetLength);
		}

		return "0".repeat(targetLength - length) + hexString;
	}

	// apagar?
	public void interactWithConsensus() {
		Scanner scanner = new Scanner(System.in);

		while (true) {
			try {
				// Get message from user
				System.out.print("> ");
				String messageToSend = scanner.nextLine();
				byte[] sendBuffer = messageToSend.getBytes();

				if ("exit".equalsIgnoreCase(messageToSend)) {
					break;
				}

				// Create and send packets to different nodes
				DatagramPacket packet1 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5001);
				DatagramPacket packet2 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5002);
				DatagramPacket packet3 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5003);
				DatagramPacket packet4 = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, 5004);

				datagramSocket.send(packet1);
				datagramSocket.send(packet2);
				datagramSocket.send(packet3);
				datagramSocket.send(packet4);

				System.out.println("Sent messages to all nodes");

				// Set a timeout for receiving responses
				datagramSocket.setSoTimeout(15000); // 15 seconds timeout

				// Try to receive responses from each node
				try {
					List<String> responses = new ArrayList<>();
					for (int i = 1; i <= 4; i++) {
						byte[] receiveBuffer = new byte[1024];
						DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

						try {
							datagramSocket.receive(receivePacket);
							String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
							responses.add(response);
							// System.out.println("Response from " + receivePacket.getAddress() + ":" +
							// receivePacket.getPort() + " - " + response);
						} catch (java.net.SocketTimeoutException e) {
							System.out.println("Timeout waiting for response from node " + i);
							break; // Exit the loop if we timeout
						}
					}
					if (responses.size() > 2)
						System.out.println("Block: " + messageToSend + " appended to blockchain!");
				} finally {
					// Reset timeout for the next iteration
					datagramSocket.setSoTimeout(0);
				}

			} catch (IOException e) {
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace();
			} finally {
				// Reset timeout for next iteration
				try {
					datagramSocket.setSoTimeout(0);
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		}

		scanner.close();
		System.out.println("Client exiting...");
	}

	public static void main(String[] args) throws SocketException, UnknownHostException {
		try {
			// Create socket and get address
			DatagramSocket datagramSocket = new DatagramSocket();
			InetAddress inetAddress = InetAddress.getByName("localhost");
			EOAccount account = new EOAccount("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef", BigInteger.valueOf(10000L),
					0L);

			// Create and run client
			Client client = new Client(datagramSocket, inetAddress, account);
			System.out.println("Client started. Connecting to consensus nodes on localhost.");
			client.sendToConsensus();

			// Clean up
			datagramSocket.close();

		} catch (SocketException | UnknownHostException e) {
			System.err.println("Error initializing client: " + e.getMessage());
			e.printStackTrace();
		}
	}
}