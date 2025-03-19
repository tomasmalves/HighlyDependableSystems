package test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import communication.AuthenticatedPerfectLink;

public class NetworkTest {
    public static void main(String[] args) throws Exception {
        // Secret keys for each node (in real systems, securely distribute them)
        String secretKey = "superSecretKey123"; 

        // Define all nodes and their IPs/ports
        Map<Integer, InetSocketAddress> nodes = new HashMap<>();
        nodes.put(2, new InetSocketAddress("localhost", 5002));
        nodes.put(3, new InetSocketAddress("localhost", 5003));
        nodes.put(4, new InetSocketAddress("localhost", 5004));

        // Initialize each node with the mapping of all other nodes
        AuthenticatedPerfectLink node1 = new AuthenticatedPerfectLink(1, 5001, secretKey, nodes);

        // Example usage:
        node1.send("Hello, Node 2!", 2);  // Send a message to Node 2
        node1.send("Hey Node 3, how are you?", 3);  // Send a message to Node 3

        // Start listening for messages (in a separate thread ideally)
        new Thread(() -> {
            try {
                while (true) {
                    node1.receive();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

