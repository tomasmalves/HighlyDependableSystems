package consensus;

import communication.NetworkHandler;
import consensus.ConsensusMessage;
import java.net.*;

public class LeaderNode extends ConsensusNode {

	public LeaderNode(int nodeId, int port) throws Exception {
		super(nodeId, port);
	}

	public void sendProposal(int epoch, String value, InetAddress targetAddress, int targetPort) throws Exception {

		// Sign the message before sending
		ConsensusMessage proposal = new ConsensusMessage(ConsensusMessage.MessageType.PROPOSE, epoch, value,
				getPrivateKey());

		NetworkHandler networkHandler = new NetworkHandler(targetPort);
		networkHandler.sendMessage(proposal.toString(), targetAddress, targetPort);

		System.out.println("Leader " + getNodeId() + " sent proposal for epoch " + epoch + ": " + value);
	}
}
