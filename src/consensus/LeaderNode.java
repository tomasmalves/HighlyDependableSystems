package consensus;

import java.net.*;

import communication.AuthenticatedPerfectLink;

public class LeaderNode extends ConsensusNode {
	
	

	public LeaderNode(int nodeId, AuthenticatedPerfectLink apl) throws Exception {
		super(nodeId, apl);
	}

}
