package nodes;

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

    public void sendThenReceive() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                // Get message from user
                String messageToSend = scanner.nextLine();
                byte[] sendBuffer = messageToSend.getBytes();

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
                datagramSocket.setSoTimeout(5000); // 2 seconds timeout

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

    public static void main(String[] args) throws SocketException, UnknownHostException {
        DatagramSocket datagramSocket = new DatagramSocket();
        InetAddress inetAddress = InetAddress.getByName("localhost");
        Client client = new Client(datagramSocket, inetAddress);
        System.out.println("Send datagram packets to a server.");
        client.sendThenReceive();
    }
}