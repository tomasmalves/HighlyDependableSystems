package consensus;

import java.util.HashMap;
import java.util.Map;

import communication.AuthenticatedPerfectLink;

public class ConditionalCollect {

	private final int leaderId;
	private final AuthenticatedPerfectLink apl;
	private Map<Integer, Map<Object[], Map<Integer, String>>> messages;
	private boolean collected = false; // Have I collected the majority of messages to continue?
	private static Map<Integer, java.security.PublicKey> memberPublicKeys = new HashMap<>();

	public ConditionalCollect(int leaderId, AuthenticatedPerfectLink apl) {
		this.leaderId = leaderId;
		this.apl = apl;
		this.messages = new HashMap<>();
	}

	public void insertMessage(int senderId, Object[] tsvalue, Map<Integer, String> writeset) {

		// Adicionar o par de (value, timestamp) e os writes no mapa de mensagens
		Map<Object[], Map<Integer, String>> stateMap = new HashMap<>();
		stateMap.put(tsvalue, writeset);

		// Atualizar o mapa de mensagens
		messages.put(senderId, stateMap);
	}

	public void processCollected(int senderId, Map<Integer, String> receivedMessages,
			Map<Integer, String> receivedSignatures) {
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