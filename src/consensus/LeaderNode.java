package consensus;

import consensus.ConsensusMessage;
import java.net.*;

public class LeaderNode extends ConsensusNode {

	public LeaderNode(int nodeId, int port) throws Exception {
		super(nodeId, port);
	}

}
