package consensus;

import communication.AckMessage;
import communication.AuthenticatedPerfectLink;
import communication.CollectMessage;
import communication.ConsensusMessage;
import communication.ConsensusMessageType;
import communication.DecideMessage;
import communication.Message;
import communication.ReadMessage;
import communication.StateMessage;
import communication.WriteMessage;
import util.CryptoUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Implementation of Byzantine Read/Write Epoch Consensus
 * This implementation assumes a static, non-Byzantine leader.
 */
public class ByzantineReadWriteConsensus {

    private final int selfId;
    private final int leaderId;
    private final List<Integer> processes;
    private final int n; // Total number of processes
    private final int f; // Maximum number of Byzantine processes
    private final AuthenticatedPerfectLink link;
    private final Map<Integer, StateMessage> collected;
    private final PrivateKey privateKey;
    private final Map<Integer, PublicKey> publicKeys;
    private final ExecutorService executor;
    private Map<Long, String> writeSet;
    private List<Map<Long, String>> writesReceived;

    private String proposedValue;
    private int consensusInstance;
    private DecideCallback decideCallback;
    private boolean running;

    // State for non-leader processes
    private int timestamp;
    private String value;
    private List<byte[]> valueProofs; // Proofs of accepted values

    // State for leader process
    private Map<Integer, List<byte[]>> proofs;
    private Set<Integer> acknowledged;

    /**
     * Constructor
     * 
     * @param selfId       The ID of this process
     * @param leaderId     The ID of the leader process
     * @param processes    List of all process IDs
     * @param maxByzantine Maximum number of Byzantine processes
     * @param link         The authenticated perfect link
     * @param privateKey   The private key of this process
     * @param publicKeys   Map of process IDs to their public keys
     */
    public ByzantineReadWriteConsensus(
            int selfId,
            int leaderId,
            List<Integer> processes,
            int maxByzantine,
            AuthenticatedPerfectLink link,
            PrivateKey privateKey,
            Map<Integer, PublicKey> publicKeys) {

        this.selfId = selfId;
        this.leaderId = leaderId;
        this.processes = processes;
        this.n = processes.size();
        this.f = maxByzantine;
        this.link = link;
        this.collected = new HashMap<>();
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.executor = Executors.newSingleThreadExecutor();
        this.writesReceived = new ArrayList<>();

        this.consensusInstance = 0;
        this.timestamp = 0;
        this.value = null;
        this.valueProofs = new ArrayList<>();

        this.proofs = new HashMap<>();
        this.acknowledged = new HashSet<>();

        this.running = false;

        // Register callback for message delivery
        link.registerDeliverCallback(this::onMessageDeliver);
    }

    public void init(String initialValue, Map<Long, String> writeSet) {
        this.proposedValue = initialValue;
        this.value = initialValue;
        if (writeSet == null)
            this.writeSet = writeSet;
    }

    public void propose(String value) {
        this.proposedValue = value;

        if (selfId == leaderId) {
            startNewConsensusInstance();
        }
    }

    public void registerDecideCallback(DecideCallback callback) {
        this.decideCallback = callback;
    }

    public void start() {
        running = true;

        startNewConsensusInstance();
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }

    /**
     * Start a new consensus instance
     */
    private void startNewConsensusInstance() {
        if (selfId != leaderId) {
            System.out.println("Only the leader can start a new consensus instance");
        }

        consensusInstance++;
        collected.clear();
        proofs.clear();
        acknowledged.clear();
        writesReceived.clear();

        if (selfId == leaderId) {
            // Phase 1: Read phase
            // Send READ message to all processes
            ReadMessage readMsg = new ReadMessage(consensusInstance);
            broadcastMessage(ConsensusMessageType.READ, readMsg);
        }
    }

    /**
     * Process a received message
     */
    private void onMessageDeliver(Message message, int sender) {
        try {
            // Deserialize the consensus message
            ConsensusMessage consensusMsg = ConsensusMessage.deserialize(message.getPayload());

            /*
             * 
             * // Verify message signature
             * if (!verifyMessageSignature(consensusMsg, sender)) {
             * System.err.println("CONSENSUS - Invalid signature on message from sender: " +
             * sender);
             * return;
             * }
             */

            System.out.println("CONSENSUS - Message: " + consensusMsg.getType());

            // Process message based on its type
            switch (consensusMsg.getType()) {
                case READ:
                    processReadMessage(consensusMsg, sender);
                    break;

                case STATE:
                    processStateMessage(consensusMsg, sender);
                    break;

                case COLLECT:
                    processCollectedMessage(consensusMsg, sender);
                    break;

                case WRITE:
                    processWriteMessage(consensusMsg, sender);
                    break;

                case ACK:
                    processAckMessage(consensusMsg, sender);
                    break;

                case DECIDE:
                    processDecideMessage(consensusMsg, sender);
                    break;

                default:
                    System.err.println("Unknown consensus message type: " + consensusMsg.getType());
            }

        } catch (Exception e) {
            System.err.println("Error processing consensus message: " + e.getMessage());
        }
    }

    /**
     * Process a READ message (non-leader)
     */
    private void processReadMessage(ConsensusMessage message, int sender) {
        if (sender != leaderId) {
            System.err.println("Received READ message from non-leader: " + sender);
            return;
        }

        ReadMessage readMsg = (ReadMessage) message.getPayload();
        int instance = readMsg.getInstance();

        if (instance < consensusInstance) {
            // Ignore old instances
            return;
        }

        consensusInstance = instance;

        // Create proofs for the current value
        byte[] proof = createValueProof();
        valueProofs.add(proof);

        // Send STATE message to the leader
        StateMessage stateMsg = new StateMessage(instance, timestamp, value, valueProofs, writeSet);
        sendMessage(ConsensusMessageType.STATE, stateMsg, leaderId);
    }

    /**
     * Process a STATE message (leader only)
     */
    private void processStateMessage(ConsensusMessage message, int sender) {
        if (selfId != leaderId) {
            // Only the leader processes STATE messages
            return;
        }

        StateMessage stateMsg = (StateMessage) message.getPayload();
        int instance = stateMsg.getInstance();

        if (instance != consensusInstance) {
            // Ignore messages from different instances
            System.out.println(
                    "CONSENSUS - INSTANCE: " + instance + " and CONSENSUSINSTANCE: " + consensusInstance);
            return;
        }

        // Store the state from this process
        collected.put(selfId,
                new StateMessage(consensusInstance, this.timestamp, this.proposedValue, this.valueProofs,
                        this.writeSet));
        collected.put(sender, stateMsg);
        proofs.put(sender, stateMsg.getProofs());

        // Check if we have enough STATE messages to proceed
        if (collected.size() >= n - f) {
            // Phase 1: READ phase
            // Send COLLECTED message to all processes
            CollectMessage collectMsg = new CollectMessage(consensusInstance, collected);
            System.out.println("CONSENSUS - broadcast collect");
            broadcastMessage(ConsensusMessageType.COLLECT, collectMsg);
            processCollectedMessage(null, selfId);
        }
    }

    /**
     * Process a COLLECT message
     */
    private void processCollectedMessage(ConsensusMessage message, int sender) {

        CollectMessage collectMsg;

        if (selfId != leaderId) {
            // Only the leader should process COLLECT messages
            collectMsg = (CollectMessage) message.getPayload();
        } else {
            collectMsg = new CollectMessage(this.consensusInstance, collected);
        }

        int instance = collectMsg.getInstance();

        if (instance != consensusInstance) {
            System.out.println("CONSENSUS - Received COLLECT for different instance: " +
                    instance + " (current: " + consensusInstance + ")");
            return;
        }

        // Analyze collected state messages to determine the most appropriate value
        Map<Integer, StateMessage> collectedStates = collectMsg.getCollected();

        // Track value occurrences and timestamps
        Map<String, Integer> valueOccurrences = new HashMap<>();
        Map<Long, Integer> timestampOccurrences = new HashMap<>();

        long maxTimestamp = -1;
        String mostRecentValue = null;

        // Analyze collected states
        for (StateMessage stateMsg : collectedStates.values()) {
            String value = stateMsg.getValue();
            long timestamp = stateMsg.getTimestamp();

            // Count value occurrences
            if (value != null) {
                valueOccurrences.put(value, valueOccurrences.getOrDefault(value, 0) + 1);
            }

            // Track timestamp occurrences
            timestampOccurrences.put(timestamp, timestampOccurrences.getOrDefault(timestamp, 0) + 1);

            // Find most recent value
            if (timestamp > maxTimestamp) {
                maxTimestamp = timestamp;
                mostRecentValue = value;
            }
        }

        // Determine the value to write
        String writeValue = proposedValue; // Default to proposed value

        // Find a value that appears in more than f processes
        for (Map.Entry<String, Integer> entry : valueOccurrences.entrySet()) {
            if (entry.getValue() > f) {
                writeValue = entry.getKey();
                break;
            }
        }

        // If no value appears in more than f processes, use the most recent value
        if (writeValue == null && mostRecentValue != null) {
            writeValue = mostRecentValue;
        }

        // Determine the timestamp for the write
        long writeTimestamp = maxTimestamp + 1;

        // Phase 2: Write phase
        WriteMessage writeMsg = new WriteMessage(instance, writeTimestamp, writeValue);
        System.out.println("CONSENSUS - Broadcasting WRITE with value: " + writeValue);
        broadcastMessage(ConsensusMessageType.WRITE, writeMsg);
    }

    /**
     * Process a WRITE message
     */
    private void processWriteMessage(ConsensusMessage message, int sender) {
        WriteMessage writeMsg = (WriteMessage) message.getPayload();
        int instance = writeMsg.getInstance();

        // Check if the message is for the current consensus instance
        if (instance != consensusInstance) {
            System.out.println("CONSENSUS - processWrite - Ignoring message from different instance. " +
                    "Current: " + consensusInstance + ", Received: " + instance);
            return;
        }

        // Create a map for this write message
        Map<Long, String> writeMap = new HashMap<>();
        writeMap.put(writeMsg.getTimestamp(), writeMsg.getValue());

        // Add the map to writesReceived list
        writesReceived.add(writeMap);

        // Check if we have received enough writes and they are consistent
        if (writesReceived.size() >= n - f) {
            // Verify that all maps in writesReceived have the same pairs
            boolean isConsistent = areMapsConsistent(writesReceived);

            if (isConsistent) {
                // Get the consistent value from the first map
                Map<Long, String> firstMap = writesReceived.get(0);
                Long timestamp = firstMap.keySet().iterator().next();
                value = firstMap.get(timestamp);

                // Create proof for the new value
                byte[] proof = createValueProof();
                valueProofs.add(proof);

                System.out.println(
                        "CONSENSUS - processWrite - Sending value " + value + " to leader");

                // Send ACK message to the leader
                AckMessage ackMsg = new AckMessage(instance, timestamp, value);
                sendMessage(ConsensusMessageType.ACK, ackMsg, leaderId);
            } else {
                // Handle inconsistent writes
                System.out.println("CONSENSUS - processWrite - Inconsistent writes received");
            }
        }
    }

    // Helper method to check consistency of maps
    private boolean areMapsConsistent(List<Map<Long, String>> maps) {
        if (maps.isEmpty()) {
            System.out.println("writemaps are empty");
            return false;
        }

        // Get the first map to compare against
        Map<Long, String> firstMap = maps.get(0);

        // Ensure all maps have exactly one entry
        for (Map<Long, String> map : maps) {
            if (map.size() != 1) {
                return false;
            }
        }

        // Get the value from the first map
        String firstValue = firstMap.values().iterator().next();

        // Check that all maps have the same value
        for (Map<Long, String> map : maps) {
            String currentValue = map.values().iterator().next();
            if (!firstValue.equals(currentValue)) {
                System.out.println("current value: " + currentValue + " and first value: " + firstValue);
                return false;
            }
        }

        return true;
    }

    /**
     * Process an ACK message (leader only)
     */
    private void processAckMessage(ConsensusMessage message, int sender) {
        if (selfId != leaderId) {
            // Only the leader processes ACK messages
            return;
        }

        AckMessage ackMsg = (AckMessage) message.getPayload();
        int instance = ackMsg.getInstance();

        if (instance != consensusInstance) {
            // Ignore messages from different instances
            return;
        }

        // Safely retrieve the collected state message
        StateMessage senderState = collected.get(sender);
        if (senderState == null) {
            System.err.println("No collected state found for sender: " + sender);
            return;
        }

        // More relaxed timestamp and value verification
        boolean isValidTimestamp = ackMsg.getTimestamp() >= senderState.getTimestamp();
        boolean isValidValue = Objects.equals(ackMsg.getValue(), value);

        if (isValidTimestamp && isValidValue) {
            acknowledged.add(sender);

            // Check if we have enough ACKs to decide
            if (acknowledged.size() >= n - f) {
                // Send DECIDE message to all processes
                DecideMessage decideMsg = new DecideMessage(consensusInstance, value);
                broadcastMessage(ConsensusMessageType.DECIDE, decideMsg);

                // Also deliver locally
                decide(value);
            }
        } else {
            System.err.println("Invalid ACK from sender: " + sender +
                    ", timestamp valid: " + isValidTimestamp +
                    ", value valid: " + isValidValue);
        }
    }

    /**
     * Process a DECIDE message
     */
    private void processDecideMessage(ConsensusMessage message, int sender) {
        if (sender != leaderId) {
            System.err.println("Received DECIDE message from non-leader: " + sender);
            return;
        }

        DecideMessage decideMsg = (DecideMessage) message.getPayload();
        int instance = decideMsg.getInstance();

        if (instance != consensusInstance) {
            // Ignore messages from different instances
            return;
        }

        // Deliver the decided value
        decide(decideMsg.getValue());
    }

    /**
     * Deliver a decided value
     */
    private void decide(String decidedValue) {
        if (decideCallback != null) {
            System.out.println("DECIDED VALUE: " + decidedValue);
            decideCallback.onDecide(decidedValue);
        }
    }

    /**
     * Create a proof for the current value
     */
    private byte[] createValueProof() {
        try {
            // Create a value proof by signing the current timestamp and value
            String data = timestamp + ":" + (value != null ? value : "null");
            return CryptoUtil.sign(data.getBytes(StandardCharsets.UTF_8), privateKey);

        } catch (Exception e) {
            System.err.println("Error creating value proof: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Verify the proofs for a value
     */
    private boolean verifyValueProofs(int processId, String value, List<byte[]> proofs) {
        if (proofs == null || proofs.isEmpty()) {
            return false;
        }

        // In a real implementation, we would verify each proof
        // For simplicity, we'll just return true
        return true;
    }

    /**
     * Verify the signature on a consensus message
     */
    private boolean verifyMessageSignature(ConsensusMessage message, int sender) {
        try {
            PublicKey publicKey = publicKeys.get(sender);
            if (publicKey == null) {
                return false;
            }

            return CryptoUtil.verify(
                    message.getContent(),
                    message.getSignature(),
                    publicKey);

        } catch (Exception e) {
            System.err.println("Error verifying message signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Broadcast a message to all processes
     */
    private void broadcastMessage(ConsensusMessageType type, Object payload) {
        for (int processId : processes) {
            if (processId != selfId) {
                sendMessage(type, payload, processId);
            }
        }
    }

    /**
     * Send a message to a specific process
     */
    private void sendMessage(ConsensusMessageType type, Object payload, int destination) {
        try {
            // Create and sign the consensus message
            ConsensusMessage consensusMsg = createConsensusMessage(type, payload);

            // Serialize and send
            byte[] msgBytes = consensusMsg.serialize();
            Message message = new Message(
                    communication.MessageType.DATA,
                    consensusInstance,
                    msgBytes);

            link.send(message, destination);

        } catch (Exception e) {
            System.err.println("Error sending consensus message: " + e.getMessage());
        }
    }

    /**
     * Create a signed consensus message
     */
    private ConsensusMessage createConsensusMessage(ConsensusMessageType type, Object payload) {
        try {
            ConsensusMessage message = new ConsensusMessage(type, payload);
            byte[] content = message.getContent();
            byte[] signature = CryptoUtil.sign(content, privateKey);
            message.setSignature(signature);
            return message;

        } catch (Exception e) {
            throw new RuntimeException("Error creating consensus message: " + e.getMessage(), e);
        }
    }
}
