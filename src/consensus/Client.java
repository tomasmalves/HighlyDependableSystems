package consensus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private DatagramSocket datagramSocket;
    private InetAddress inetAddress;

    public Client(DatagramSocket datagramSocket, InetAddress inetAddress) {
        this.datagramSocket = datagramSocket;
        this.inetAddress = inetAddress;
    }

    public void sendToConsensus() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter messages to send to the consensus system (or 'exit' to quit):");
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
                            break; // Exit the loop if we timeout
                        }
                    }
                } finally {
                    // Reset timeout for the next iteration
                    datagramSocket.setSoTimeout(0);
                }

            } catch (IOException e) {
                e.printStackTrace();
                scanner.close();
                break;
            }
        }
    }

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
                            break; // Exit the loop if we timeout
                        }
                    }
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

            // Create and run client
            Client client = new Client(datagramSocket, inetAddress);
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