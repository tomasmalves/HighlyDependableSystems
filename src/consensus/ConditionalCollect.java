package consensus;

import java.util.HashMap;
import java.util.Map;

import communication.AuthenticatedPerfectLink;

public class ConditionalCollect {
	
	private final int leaderId;
    private final AuthenticatedPerfectLink apl;
	private Map<Integer, Map<Object[], Map<Integer, String>>> messages;
	private boolean collected = false; //Have I collected the majority of messages to continue?
	private static Map<Integer, java.security.PublicKey> memberPublicKeys = new HashMap<>();
	
	public ConditionalCollect(int leaderId, AuthenticatedPerfectLink apl) {
        this.leaderId = leaderId;
        this.apl = apl;
    }

    public void insertState(int senderId, String message, String signature) {
        messages.put( leaderId);
    }

    public void deliverMessage(int senderId, String message, String signature) {
        if (leaderId == apl.getNodeId()) { // Only leader processes messages
            messages.put(senderId, message);
            signatures.put(senderId, signature);
        }
    }

    public void processCollected(int senderId, Map<Integer, String> receivedMessages, Map<Integer, String> receivedSignatures) {
        if (!collected && receivedMessages.size() >= messages.size() && checkPredicate(receivedMessages)) {
            collected = true;
            triggerCollectedEvent(receivedMessages);
        }
    }

    private boolean checkPredicate(Map<Integer, String> messages) {
        return !messages.isEmpty(); // Modify based on consensus logic
    }

    private void triggerCollectedEvent(Map<Integer, String> collectedMessages) {
        System.out.println("Collected event triggered with messages: " + collectedMessages);
    }
}
