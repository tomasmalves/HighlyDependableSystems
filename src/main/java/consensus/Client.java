package consensus;

import java.io.IOException;
import java.math.BigInteger;

import blockchain.Transaction;
import accounts.Account;
import accounts.EOAccount;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
	            if ("exit".equalsIgnoreCase(destination)) break;

	            System.out.print("Amount to Transfer (integer): ");
	            String amountInput = scanner.nextLine();
	            if ("exit".equalsIgnoreCase(amountInput)) break;
	            int amount = Integer.parseInt(amountInput);

	            System.out.print("Data (in case you're transfering ISTCoin, press Enter to skip): ");
	            String dataInput = scanner.nextLine();
	            if ("exit".equalsIgnoreCase(dataInput)) break;
	            String data = dataInput.isEmpty() ? "null" : dataInput;
	            
	            String nonce = Long.toString(account.getNonce());
	            account.incrementNonce();

	            // Build the string message: source|destination|amount|data
	            String messageToSend = account.getAddress() + "|" + destination + "|" + amount + "|" + data + "|" + nonce;

	            byte[] sendBuffer = messageToSend.getBytes();

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

	        } catch (IOException | NumberFormatException e) {
	            System.out.println("Error: " + e.getMessage());
	            e.printStackTrace();
	            break;
	        }
	    }

	    scanner.close();
	}

	//apagar?
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
			EOAccount account = new EOAccount("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef", BigInteger.valueOf(10000L), 0L);

			
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