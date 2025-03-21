package nodes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class ConsensusNode {

    private final int nodeId;
    private DatagramSocket mainServerSocket;
    private InetAddress inetAddress;
    private final byte[] privateKey;
    private byte[] buffer = new byte[256];

    public ConsensusNode(int nodeId, DatagramSocket mainServerSocket, InetAddress inetAddress)
            throws NoSuchAlgorithmException {
        this.nodeId = nodeId;
        this.mainServerSocket = mainServerSocket;
        this.inetAddress = inetAddress;
        KeyPair keyPair = generateRSAKeyPair();
        this.privateKey = keyPair.getPrivate().getEncoded();
    }

    public void receiveFromServer() {
        while (true) {
            try {
                // Create new buffer for receiving
                buffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                // Receive the message
                mainServerSocket.receive(receivePacket);
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String messageFromClient = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Node " + nodeId + " received: " + messageFromClient +
                        " from " + clientAddress + ":" + clientPort);

                // Create response message
                String responseMessage = "Response from node " + nodeId;
                byte[] responseBytes = responseMessage.getBytes();

                // Create new packet for sending the response
                DatagramPacket sendPacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        clientAddress,
                        clientPort);

                System.out.println("Node " + nodeId + " sending response to: " + clientAddress + ":" + clientPort);
                mainServerSocket.send(sendPacket);

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048 bit keys are secure enough for this project
        return keyGen.generateKeyPair();
    }

    public static void main(String[] args) throws SocketException, UnknownHostException, NoSuchAlgorithmException {
        int nodeId = Integer.parseInt(args[0]);
        DatagramSocket mainServerSocket = new DatagramSocket(5000 + nodeId);
        InetAddress inetAddress = InetAddress.getByName("localhost");
        ConsensusNode node = new ConsensusNode(nodeId, mainServerSocket, inetAddress);
        node.receiveFromServer();
    }

}
