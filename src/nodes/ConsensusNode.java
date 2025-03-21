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

    private DatagramSocket mainServerSocket;
    private InetAddress inetAddress;
    private final byte[] privateKey;
    private byte[] buffer = new byte[256];

    public ConsensusNode(DatagramSocket mainServerSocket, InetAddress inetAddress) throws NoSuchAlgorithmException {
        this.mainServerSocket = mainServerSocket;
        this.inetAddress = inetAddress;
        KeyPair keyPair = generateRSAKeyPair();
        this.privateKey = keyPair.getPrivate().getEncoded();
        // storePublicKey(keyPair.getPublic());
    }

    public void receiveFromServer() {
        while (true) {
            try {
                // Create new buffer for receiving
                buffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                // Receive the message from the server
                mainServerSocket.receive(receivePacket);
                InetAddress serverAddress = receivePacket.getAddress();
                int serverPort = receivePacket.getPort();
                String messageFromServer = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Message from server: " + messageFromServer);

                // Create a new byte array for response
                byte[] responseBytes = "hi from leader".getBytes();

                // Create a new packet for sending the response
                DatagramPacket sendPacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        serverAddress,
                        serverPort);

                System.out.println("Leader sending response to: " + serverAddress + ":" + serverPort);
                mainServerSocket.send(sendPacket);

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /*
     * @SuppressWarnings("unchecked")
     * public static void generateMembershipConfig(String baseAddress, int basePort)
     * throws GeneralSecurityException, IOException {
     * 
     * JSONObject config = new JSONObject();
     * JSONArray membersArray = new JSONArray();
     * 
     * for (int i = 0; i < nodeCount; i++) {
     * String nodeId = "node" + (i + 1);
     * String role = i == 0 ? "leader" : "member"; // First node is the leader
     * String nodeAddress = baseAddress;
     * int nodePort = basePort + i;
     * 
     * // Generate keys for this node
     * KeyPair keyPair = generateRSAKeyPair();
     * 
     * // Save private key
     * String privKeyPath = KEY_DIR + nodeId + "_priv.key";
     * saveKeyToFile(keyPair.getPrivate().getEncoded(), privKeyPath);
     * 
     * // Save public key
     * String pubKeyPath = KEY_DIR + nodeId + "_pub.key";
     * saveKeyToFile(keyPair.getPublic().getEncoded(), pubKeyPath);
     * 
     * // Create node info JSON object
     * JSONObject nodeInfo = new JSONObject();
     * nodeInfo.put("id", nodeId);
     * nodeInfo.put("address", nodeAddress);
     * nodeInfo.put("port", nodePort);
     * nodeInfo.put("role", role);
     * nodeInfo.put("publicKeyPath", pubKeyPath);
     * 
     * // Add to members array
     * membersArray.add(nodeInfo);
     * 
     * System.out.println("Generated keys for " + nodeId + " (" + role + ")");
     * }
     * 
     * config.put("members", membersArray);
     * 
     * // Write configuration to file
     * try (FileWriter file = new FileWriter(CONFIG_FILE)) {
     * file.write(config.toJSONString());
     * }
     * }
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048 bit keys are secure enough for this project
        return keyGen.generateKeyPair();
    }

    /*
     * public static void saveKeyToFile(byte[] keyBytes, String filePath) throws
     * IOException {
     * try (FileOutputStream fos = new FileOutputStream(filePath)) {
     * fos.write(keyBytes);
     * }
     * }
     * 
     * public static PublicKey readPublicKey(String keyPath)
     * throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
     * byte[] encoded;
     * try (FileInputStream fis = new FileInputStream(keyPath)) {
     * encoded = new byte[fis.available()];
     * fis.read(encoded);
     * }
     * 
     * KeyFactory keyFactory = KeyFactory.getInstance("RSA");
     * X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
     * return keyFactory.generatePublic(keySpec);
     * }
     * 
     * public static PrivateKey readPrivateKey(String keyPath)
     * throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
     * byte[] encoded;
     * try (FileInputStream fis = new FileInputStream(keyPath)) {
     * encoded = new byte[fis.available()];
     * fis.read(encoded);
     * }
     * 
     * KeyFactory keyFactory = KeyFactory.getInstance("RSA");
     * PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
     * return keyFactory.generatePrivate(keySpec);
     * }
     */

    public static void main(String[] args) throws SocketException, UnknownHostException, NoSuchAlgorithmException {
        DatagramSocket mainServerSocket = new DatagramSocket(5001);
        InetAddress inetAddress = InetAddress.getByName("localhost");
        ConsensusNode node = new ConsensusNode(mainServerSocket, inetAddress);
        node.receiveFromServer();
    }

}
