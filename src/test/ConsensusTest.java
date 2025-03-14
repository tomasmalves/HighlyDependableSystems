package test;

import consensus.ConsensusNode;

public class ConsensusTest {
    public static void main(String[] args) throws Exception {

        ConsensusNode node1 = new ConsensusNode(1, 5001);
        ConsensusNode node2 = new ConsensusNode(2, 5002);
        
        // Node 1 signs a message
        String message = "Proposed Value: 42";
        String signedMessage = node1.signMessage(message);
        System.out.println("Node 1 Signed Message: " + signedMessage);

        // Node 2 verifies the message
        boolean isValid = node2.verifySignature(message, signedMessage, node1.getPublicKey());
        System.out.println("Node 2 Verification Result: " + isValid);

//        // Node 1 sends a message to Node 2
//        new Thread(() -> {
//            try {
//                node1.sendMessage(5002, "Hello from Node 1!");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//        // Node 2 receives the message
//        String receivedMessage = node2.receiveMessage();
//        System.out.println("Node 2 received: " + receivedMessage);
    }
}
