package consensus;

import java.util.HashMap;
import java.util.Map;
import communication.NetworkHandler;

public class ConsensusNode {
	private final int nodeId;
    private final NetworkHandler networkHandler;
    private final Map<Integer, String> proposedValues;

    public ConsensusNode(int nodeId, int port) throws Exception {
        this.nodeId = nodeId;
        this.networkHandler = new NetworkHandler(port);
        this.proposedValues = new HashMap<>();
    }
    
    public void proposeValue(int epoch, String value) {
        proposedValues.put(epoch, value);
        System.out.println("Node " + nodeId + " proposed value for epoch " + epoch + ": " + value);
    }
    
    public void decideValue(int epoch) {
        if (proposedValues.containsKey(epoch)) {
            System.out.println("Node " + nodeId + " decided on value: " + proposedValues.get(epoch));
        } else {
            System.out.println("Node " + nodeId + " has no proposed value for epoch " + epoch);
        }
    }
}
