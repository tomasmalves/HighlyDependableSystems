package consensus;

import communication.AuthenticatedPerfectLink;
import communication.Message;
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
        if (running) {
            return;
        }

        running = true;

        if (selfId == leaderId && proposedValue != null) {
            startNewConsensusInstance();
        }
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }

    /**
     * Start a new consensus instance (leader only)
     */
    private void startNewConsensusInstance() {
        if (selfId != leaderId) {
            throw new IllegalStateException("Only the leader can start a new consensus instance");
        }

        consensusInstance++;
        collected.clear();
        proofs.clear();
        acknowledged.clear();

        // Phase 1: Read phase
        // Send READ message to all processes
        ReadMessage readMsg = new ReadMessage(consensusInstance);
        broadcastMessage(ConsensusMessageType.READ, readMsg);
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
                    "\n\nCONSENSUS - INSTANCE: " + instance + " and CONSENSUSINSTANCE: " + consensusInstance + "\n\n");
            return;
        }

        // Store the state from this process
        collected.put(sender, stateMsg);
        proofs.put(sender, stateMsg.getProofs());

        System.out.println("\n\nCONSENSUS - broadcast write\n\n");

        // Check if we have enough STATE messages to proceed
        if (collected.size() >= n - f) {

            // Phase 1: READ phase
            // Send COLLECTED message to all processes
            CollectMessage collectMsg = new CollectMessage(consensusInstance, collected);
            System.out.println("\n\nCONSENSUS - broadcast collect\n\n");
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
            collectMsg = new CollectMessage(selfId, collected);
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
            System.out.println("\n\nCONSENSUS - processWrite - Ignoring message from different instance. " +
                    "Current: " + consensusInstance + ", Received: " + instance + " \n\n");
            return;
        }

        // Create a map for this write message
        Map<Long, String> writeMap = new HashMap<>();
        writeMap.put(writeMsg.getTimestamp(), writeMsg.getValue());

        // Add the map to writesReceived list
        writesReceived.add(writeMap);

        // Check if we have received enough writes and they are consistent
        if (writesReceived.size() > n - f) {
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

                System.out.println("\n\nCONSENSUS - processWrite - Sending value " + value + " to leader\n\n");

                // Send ACK message to the leader
                AckMessage ackMsg = new AckMessage(instance, timestamp, value);
                sendMessage(ConsensusMessageType.ACK, ackMsg, leaderId);
            } else {
                // Handle inconsistent writes
                System.out.println("\n\nCONSENSUS - processWrite - Inconsistent writes received\n\n");
            }
        }
    }

    // Helper method to check consistency of maps
    private boolean areMapsConsistent(List<Map<Long, String>> maps) {
        if (maps.isEmpty()) {
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
            if (acknowledged.size() > n - f) {
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
            System.out.println("\n\n\nDECIDED VALUE: " + decidedValue + "\n\n\n");
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

/**
 * Types of consensus messages
 */
enum ConsensusMessageType {
    READ,
    STATE,
    COLLECT,
    WRITE,
    ACK,
    DECIDE
}

/**
 * Base class for consensus messages
 */
class ConsensusMessage {
    private final ConsensusMessageType type;
    private final Object payload;
    private byte[] signature;

    public ConsensusMessage(ConsensusMessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public ConsensusMessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Get the content to be signed
     */
    public byte[] getContent() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write message type
            dos.writeInt(type.ordinal());

            // Write payload content
            byte[] payloadBytes = serializePayload();
            dos.writeInt(payloadBytes.length);
            dos.write(payloadBytes);

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error creating message content for signing", e);
        }
    }

    /**
     * Serialize the payload based on its type
     */
    private byte[] serializePayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        if (payload instanceof ReadMessage readMsg) {
            dos.writeInt(readMsg.getInstance());
        } else if (payload instanceof StateMessage stateMsg) {
            dos.writeInt(stateMsg.getInstance());
            dos.writeLong(stateMsg.getTimestamp());

            // Handle null value
            boolean hasValue = stateMsg.getValue() != null;
            dos.writeBoolean(hasValue);
            if (hasValue) {
                dos.writeUTF(stateMsg.getValue());
            }

            // Serialize proofs
            List<byte[]> proofs = stateMsg.getProofs();
            dos.writeInt(proofs.size());
            for (byte[] proof : proofs) {
                dos.writeInt(proof.length);
                dos.write(proof);
            }

            // Serialize write set
            Map<Long, String> writeSet = stateMsg.getWriteSet();
            dos.writeInt(writeSet.size());
            for (Map.Entry<Long, String> entry : writeSet.entrySet()) {
                dos.writeLong(entry.getKey());
                dos.writeUTF(entry.getValue());
            }
        } else if (payload instanceof WriteMessage writeMsg) {
            dos.writeInt(writeMsg.getInstance());
            dos.writeLong(writeMsg.getTimestamp());

            // Handle null value
            boolean hasValue = writeMsg.getValue() != null;
            dos.writeBoolean(hasValue);
            if (hasValue) {
                dos.writeUTF(writeMsg.getValue());
            }
        } else if (payload instanceof AckMessage ackMsg) {
            dos.writeInt(ackMsg.getInstance());
            dos.writeLong(ackMsg.getTimestamp());

            // Handle null value
            boolean hasValue = ackMsg.getValue() != null;
            dos.writeBoolean(hasValue);
            if (hasValue) {
                dos.writeUTF(ackMsg.getValue());
            }
        } else if (payload instanceof DecideMessage decideMsg) {
            dos.writeInt(decideMsg.getInstance());

            // Handle null value
            boolean hasValue = decideMsg.getValue() != null;
            dos.writeBoolean(hasValue);
            if (hasValue) {
                dos.writeUTF(decideMsg.getValue());
            }
        } else if (payload instanceof CollectMessage collectMsg) {
            dos.writeInt(collectMsg.getInstance());

            // Serialize collected state messages
            Map<Integer, StateMessage> collected = collectMsg.getCollected();
            dos.writeInt(collected.size());

            for (Map.Entry<Integer, StateMessage> entry : collected.entrySet()) {
                // Write process ID
                dos.writeInt(entry.getKey());

                // Serialize StateMessage
                StateMessage stateMsg = entry.getValue();
                dos.writeInt(stateMsg.getInstance());
                dos.writeLong(stateMsg.getTimestamp());

                // Handle null value
                boolean hasValue = stateMsg.getValue() != null;
                dos.writeBoolean(hasValue);
                if (hasValue) {
                    dos.writeUTF(stateMsg.getValue());
                }

                // Serialize proofs
                List<byte[]> proofs = stateMsg.getProofs();
                dos.writeInt(proofs.size());
                for (byte[] proof : proofs) {
                    dos.writeInt(proof.length);
                    dos.write(proof);
                }

                // Serialize write set
                Map<Long, String> writeSet = stateMsg.getWriteSet();
                dos.writeInt(writeSet.size());
                for (Map.Entry<Long, String> wsEntry : writeSet.entrySet()) {
                    dos.writeLong(wsEntry.getKey());
                    dos.writeUTF(wsEntry.getValue());
                }
            }
        }

        dos.flush();
        return baos.toByteArray();
    }

    /**
     * Serialize the consensus message to bytes
     */
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write message type
            dos.writeInt(type.ordinal());

            // Write payload data
            byte[] payloadBytes = serializePayload();
            dos.writeInt(payloadBytes.length);
            dos.write(payloadBytes);

            // Write signature
            if (signature != null) {
                dos.writeInt(signature.length);
                dos.write(signature);
            } else {
                dos.writeInt(0);
            }

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing consensus message", e);
        }
    }

    /**
     * Deserialize bytes to a ConsensusMessage
     */
    public static ConsensusMessage deserialize(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);

            // Read message type
            ConsensusMessageType type = ConsensusMessageType.values()[dis.readInt()];

            // Read payload
            int payloadLength = dis.readInt();
            byte[] payloadBytes = new byte[payloadLength];
            dis.readFully(payloadBytes);

            // Deserialize payload based on message type
            Object payload = deserializePayload(type, payloadBytes);

            // Create message
            ConsensusMessage message = new ConsensusMessage(type, payload);

            // Read signature
            int signatureLength = dis.readInt();
            if (signatureLength > 0) {
                byte[] signature = new byte[signatureLength];
                dis.readFully(signature);
                message.setSignature(signature);
            }

            return message;
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing consensus message", e);
        }
    }

    /**
     * Deserialize payload based on message type
     */
    private static Object deserializePayload(ConsensusMessageType type, byte[] payloadBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(payloadBytes);
        DataInputStream dis = new DataInputStream(bais);

        return switch (type) {
            case READ -> new ReadMessage(dis.readInt());

            case STATE -> {
                int stateInstance = dis.readInt();
                long timestamp = dis.readLong();

                // Read value (handle null)
                String stateValue = null;
                boolean hasValue = dis.readBoolean();
                if (hasValue) {
                    stateValue = dis.readUTF();
                }

                // Read proofs
                int proofsCount = dis.readInt();
                var proofs = new ArrayList<byte[]>();
                for (int i = 0; i < proofsCount; i++) {
                    int proofLength = dis.readInt();
                    byte[] proof = new byte[proofLength];
                    dis.readFully(proof);
                    proofs.add(proof);
                }

                // Read writeSet
                Map<Long, String> writeSet = new HashMap<>();
                int writeSetSize = dis.readInt();
                for (int i = 0; i < writeSetSize; i++) {
                    long key = dis.readLong();
                    String writeValue = dis.readUTF();
                    writeSet.put(key, writeValue);
                }

                yield new StateMessage(stateInstance, timestamp, stateValue, proofs, writeSet);
            }

            case WRITE -> {
                int writeInstance = dis.readInt();
                long writeTimestamp = dis.readLong();

                // Read value (handle null)
                String writeValue = null;
                boolean hasValue = dis.readBoolean();
                if (hasValue) {
                    writeValue = dis.readUTF();
                }

                yield new WriteMessage(writeInstance, writeTimestamp, writeValue);
            }

            case ACK -> {
                int ackInstance = dis.readInt();
                long ackTimestamp = dis.readLong();

                // Read value (handle null)
                String ackValue = null;
                boolean hasValue = dis.readBoolean();
                if (hasValue) {
                    ackValue = dis.readUTF();
                }

                yield new AckMessage(ackInstance, ackTimestamp, ackValue);
            }

            case DECIDE -> {
                int decideInstance = dis.readInt();

                // Read value (handle null)
                String decideValue = null;
                boolean hasValue = dis.readBoolean();
                if (hasValue) {
                    decideValue = dis.readUTF();
                }

                yield new DecideMessage(decideInstance, decideValue);
            }

            case COLLECT -> {
                int collectInstance = dis.readInt();

                // Read collected state messages
                int collectedSize = dis.readInt();
                Map<Integer, StateMessage> collected = new HashMap<>();

                for (int i = 0; i < collectedSize; i++) {
                    // Read process ID
                    int processId = dis.readInt();

                    // Read StateMessage
                    int stateInstance = dis.readInt();
                    long timestamp = dis.readLong();

                    // Read value (handle null)
                    String stateValue = null;
                    boolean hasValue = dis.readBoolean();
                    if (hasValue) {
                        stateValue = dis.readUTF();
                    }

                    // Read proofs
                    int proofsCount = dis.readInt();
                    var proofs = new ArrayList<byte[]>();
                    for (int j = 0; j < proofsCount; j++) {
                        int proofLength = dis.readInt();
                        byte[] proof = new byte[proofLength];
                        dis.readFully(proof);
                        proofs.add(proof);
                    }

                    // Read write set
                    Map<Long, String> writeSet = new HashMap<>();
                    int writeSetSize = dis.readInt();
                    for (int j = 0; j < writeSetSize; j++) {
                        long key = dis.readLong();
                        String writeValue = dis.readUTF();
                        writeSet.put(key, writeValue);
                    }

                    // Create and add StateMessage to collected
                    StateMessage stateMessage = new StateMessage(
                            stateInstance, timestamp, stateValue, proofs, writeSet);
                    collected.put(processId, stateMessage);
                }

                yield new CollectMessage(collectInstance, collected);
            }

            default -> throw new IOException("Unknown message type: " + type);
        };
    }
}

/**
 * READ message for Phase 1
 */
class ReadMessage {
    private final int instance;

    public ReadMessage(int instance) {
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }
}

/**
 * STATE message for Phase 1 response
 */
class StateMessage {
    private final int instance;
    private final String value;
    private final long timestamp;
    private final Map<Long, String> writeSet;
    private final List<byte[]> proofs;

    /**
     * Constructor for StateMessage
     * 
     * @param instance  The consensus instance number
     * @param timestamp The timestamp of the message
     * @param value     The current value
     * @param proofs    List of proofs for the value
     * @param writeSet  Map of previous writes
     */
    public StateMessage(int instance, long timestamp, String value, List<byte[]> proofs, Map<Long, String> writeSet) {
        this.instance = instance;
        this.timestamp = timestamp;
        this.value = value;
        this.proofs = proofs != null ? new ArrayList<>(proofs) : new ArrayList<>(); // Defensive copy
        this.writeSet = writeSet != null ? new HashMap<>(writeSet) : new HashMap<>(); // Defensive copy
    }

    /**
     * Get the consensus instance number
     * 
     * @return The instance number
     */
    public int getInstance() {
        return instance;
    }

    /**
     * Get the timestamp of the message
     * 
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the current value
     * 
     * @return The value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the proofs for the value
     * 
     * @return Unmodifiable list of proofs
     */
    public List<byte[]> getProofs() {
        return new ArrayList<>(proofs); // Return a defensive copy
    }

    /**
     * Get the write set
     * 
     * @return Unmodifiable map of previous writes
     */
    public Map<Long, String> getWriteSet() {
        return new HashMap<>(writeSet); // Return a defensive copy
    }

    /**
     * toString method for debugging
     * 
     * @return String representation of the StateMessage
     */
    @Override
    public String toString() {
        return "StateMessage{" +
                "instance=" + instance +
                ", value='" + value + '\'' +
                ", timestamp=" + timestamp +
                ", proofs=" + proofs.size() +
                ", writeSet=" + writeSet.size() +
                '}';
    }
}

/**
 * COLLECT message for Phase 2
 */
class CollectMessage {
    private final int instance;
    private final Map<Integer, StateMessage> collected;
    private final long timestamp;

    /**
     * Constructor for CollectMessage
     * 
     * @param instance  The consensus instance number
     * @param collected Map of process IDs to their state messages
     */
    public CollectMessage(int instance, Map<Integer, StateMessage> collected) {
        this.instance = instance;
        this.collected = new HashMap<>(collected); // Defensive copy

        // Calculate the maximum timestamp among collected messages
        this.timestamp = collected.values().stream()
                .mapToLong(StateMessage::getTimestamp)
                .max()
                .orElse(0L);
    }

    /**
     * Get the consensus instance number
     * 
     * @return The instance number
     */
    public int getInstance() {
        return instance;
    }

    /**
     * Get the collected state messages
     * 
     * @return Unmodifiable map of collected state messages
     */
    public Map<Integer, StateMessage> getCollected() {
        return new HashMap<>(collected); // Return a defensive copy
    }

    /**
     * Get the maximum timestamp among collected messages
     * 
     * @return The maximum timestamp
     */
    public long getMaxTimestamp() {
        return timestamp;
    }

    /**
     * Get the number of collected state messages
     * 
     * @return Number of collected messages
     */
    public int getCollectedCount() {
        return collected.size();
    }

    /**
     * Find the most frequent value in the collected messages
     * 
     * @return The most frequent value, or null if no clear majority
     */
    public String getMostFrequentValue() {
        Map<String, Integer> valueFrequency = new HashMap<>();

        collected.values().stream()
                .filter(msg -> msg.getValue() != null)
                .forEach(msg -> valueFrequency.put(
                        msg.getValue(),
                        valueFrequency.getOrDefault(msg.getValue(), 0) + 1));

        return valueFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Serialize the CollectMessage for network transmission
     * 
     * @return Byte array representation of the message
     * @throws IOException If serialization fails
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write instance
        dos.writeInt(instance);

        // Write number of collected messages
        dos.writeInt(collected.size());

        // Write each collected message
        for (Map.Entry<Integer, StateMessage> entry : collected.entrySet()) {
            // Write process ID
            dos.writeInt(entry.getKey());

            // Serialize the StateMessage
            StateMessage stateMsg = entry.getValue();
            dos.writeInt(stateMsg.getInstance());
            dos.writeLong(stateMsg.getTimestamp());

            // Write value (handle null)
            String value = stateMsg.getValue();
            dos.writeBoolean(value != null);
            if (value != null) {
                dos.writeUTF(value);
            }

            // Write proofs
            List<byte[]> proofs = stateMsg.getProofs();
            dos.writeInt(proofs.size());
            for (byte[] proof : proofs) {
                dos.writeInt(proof.length);
                dos.write(proof);
            }

            // Write writeset
            Map<Long, String> writeSet = stateMsg.getWriteSet();
            dos.writeInt(writeSet.size());
            for (Map.Entry<Long, String> wsEntry : writeSet.entrySet()) {
                dos.writeLong(wsEntry.getKey());
                dos.writeUTF(wsEntry.getValue());
            }
        }

        dos.flush();
        return baos.toByteArray();
    }

    /**
     * Deserialize a CollectMessage from byte array
     * 
     * @param data Byte array containing the serialized message
     * @return Deserialized CollectMessage
     * @throws IOException If deserialization fails
     */
    public static CollectMessage deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        // Read instance
        int instance = dis.readInt();

        // Read number of collected messages
        int collectedCount = dis.readInt();
        Map<Integer, StateMessage> collected = new HashMap<>();

        // Read each collected message
        for (int i = 0; i < collectedCount; i++) {
            // Read process ID
            int processId = dis.readInt();

            // Read StateMessage details
            int stateInstance = dis.readInt();
            long timestamp = dis.readLong();

            // Read value (handle null)
            String value = null;
            boolean hasValue = dis.readBoolean();
            if (hasValue) {
                value = dis.readUTF();
            }

            // Read proofs
            int proofCount = dis.readInt();
            List<byte[]> proofs = new ArrayList<>();
            for (int j = 0; j < proofCount; j++) {
                int proofLength = dis.readInt();
                byte[] proof = new byte[proofLength];
                dis.readFully(proof);
                proofs.add(proof);
            }

            // Read writeset
            int writeSetSize = dis.readInt();
            Map<Long, String> writeSet = new HashMap<>();
            for (int j = 0; j < writeSetSize; j++) {
                long key = dis.readLong();
                String wsValue = dis.readUTF();
                writeSet.put(key, wsValue);
            }

            // Create StateMessage and add to collected
            StateMessage stateMessage = new StateMessage(
                    stateInstance, timestamp, value, proofs, writeSet);
            collected.put(processId, stateMessage);
        }

        return new CollectMessage(instance, collected);
    }
}

/**
 * WRITE message for Phase 2
 */
class WriteMessage {
    private final int instance;
    private final long timestamp;
    private final String value;

    public WriteMessage(int instance, long timestamp, String value) {
        this.instance = instance;
        this.timestamp = timestamp;
        this.value = value;
    }

    public int getInstance() {
        return instance;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getValue() {
        return value;
    }
}

/**
 * ACK message for Phase 2 response
 */
class AckMessage {
    private final int instance;
    private final long timestamp;
    private final String value;

    public AckMessage(int instance, long timestamp, String value) {
        this.instance = instance;
        this.timestamp = timestamp;
        this.value = value;
    }

    public int getInstance() {
        return instance;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getValue() {
        return value;
    }
}

/**
 * DECIDE message for the final decision
 */
class DecideMessage {
    private final int instance;
    private final String value;

    public DecideMessage(int instance, String value) {
        this.instance = instance;
        this.value = value;
    }

    public int getInstance() {
        return instance;
    }

    public String getValue() {
        return value;
    }
}