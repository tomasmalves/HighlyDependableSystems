package test;

import consensus.ConsensusNode;

public class ConsensusTest {

    public static void main(String[] args) throws Exception {

        ConsensusNode node = new ConsensusNode(0, 5000);
        node.proposeValue(1, "Block Data");
        String proposedValue = node.getProposedValue(1);

        /*
         * ConsensusNode node0 = new ConsensusNode(0, 5001);
         * ConsensusNode node1 = new ConsensusNode(1, 5002);
         * ConsensusNode node2 = new ConsensusNode(2, 5003);
         * ConsensusNode node3 = new ConsensusNode(3, 5004);
         * 
         * node0.proposeValue(1, "Genesis Block Data");
         * node1.proposeValue(1, "Block Data 1");
         * node2.proposeValue(1, "Block Data 2");
         * node3.proposeValue(1, "Block Data 3");
         * 
         * String proposedValue1 = node0.getProposedValue(1);
         * String proposedValue2 = node1.getProposedValue(1);
         * String proposedValue3 = node2.getProposedValue(1);
         * String proposedValue4 = node3.getProposedValue(1);
         * 
         */

        if ("Genesis Block Data".equals(proposedValue)) {
            System.out.println("✅ Consensus Proposal Test Passed");
        } else {
            System.out
                    .println("❌ Consensus Proposal Test Failed: Expected 'Block Data' but got '" + "'");
        }
    }
}
