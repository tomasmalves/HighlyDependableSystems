package consensus;

import java.util.Arrays;
import java.util.Scanner;

public class TestConsensus {
    public static void main(String[] args) throws Exception {
        // Check if we should start all nodes in this process (for testing)
        boolean localTest = args.length > 0 && "local".equals(args[0]);

        if (localTest) {
            // Start multiple nodes in separate threads for local testing
            for (int i = 1; i <= 4; i++) {
                final int nodeId = i;
                new Thread(() -> {
                    try {
                        ConsensusNode.main(new String[] { String.valueOf(nodeId) });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // Allow interaction with the leader node
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter values to propose (leader will be node 1):");

            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }

                // Send input to node 0 (leader)
                // In a real system, you'd use a client to send this to the leader
                // For this test, we're just printing
                System.out.println("Proposing: " + input);
            }

            scanner.close();
        } else {
            // Normal mode: just start one node based on command line arg
            if (args.length < 1) {
                System.out.println("Usage: java TestConsensus <nodeId> | local");
                System.exit(1);
            }

            ConsensusNode.main(new String[] { args[0] });
        }
    }
}