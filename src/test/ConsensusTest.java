package test;

import consensus.ConsensusNode;

public class ConsensusTest {
	
	public static void main(String[] args) throws Exception {
        ConsensusNode node = new ConsensusNode(1, 5003);
        node.proposeValue(1, "Block Data");
        
        String proposedValue = node.getProposedValue(1);
        if ("Block Data".equals(proposedValue)) {
            System.out.println("✅ Consensus Proposal Test Passed");
        } else {
            System.out.println("❌ Consensus Proposal Test Failed: Expected 'Block Data' but got '" + proposedValue + "'");
        }
    }
}
