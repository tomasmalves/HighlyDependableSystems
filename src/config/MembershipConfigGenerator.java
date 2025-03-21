package config;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import nodes.ConsensusNode;

public class MembershipConfigGenerator { // TODO

    private final String BASE_ADDRESS;
    private final int BASE_PORT;
    private final int NODE_COUNT;
    private HashMap<String, PublicKey> membersPublicKeys;

    public MembershipConfigGenerator() throws GeneralSecurityException, IOException {
        this.BASE_ADDRESS = "localhost";
        this.BASE_PORT = 5000;
        this.NODE_COUNT = 4;
        generateMembershipConfig(NODE_COUNT, BASE_ADDRESS, BASE_PORT);
    }

    @SuppressWarnings("unchecked")
    public void generateMembershipConfig(int nodeCount, String baseAddress, int basePort)
            throws GeneralSecurityException, IOException {

        for (int i = 0; i < nodeCount; i++) {
            String nodeId = "node" + (i + 1);
            // String nodeAddress = baseAddress;
            // int nodePort = basePort + i+1;
            InetAddress nodeAddress = InetAddress.getByName(baseAddress);
            int nodePort = basePort + i + 1;
            DatagramSocket nodeSocket = new DatagramSocket(nodePort);

            // Generate keys for this node
            KeyPair keyPair = generateRSAKeyPair();

            ConsensusNode node = new ConsensusNode(nodeSocket, nodeAddress);

            // Save public key
            this.membersPublicKeys.put(nodeId, keyPair.getPublic());
        }
    }

    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048 bit keys are secure enough for this project
        return keyGen.generateKeyPair();
    }
}