package consensus;

import java.util.HashMap;
import java.util.Map;

public class ConditionalCollect {
	private Map<Integer, Map<Object[], Map<Integer, String>>> messages;
	private boolean collected; //Have I collected the majority of messages to continue?
	private static Map<Integer, java.security.PublicKey> memberPublicKeys = new HashMap<>();
	
	
}
