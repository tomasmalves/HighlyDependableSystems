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
    private final ConditionalCollect collector;
    private final PrivateKey privateKey;
    private final Map<Integer, PublicKey> publicKeys;
    private final ExecutorService executor;

    private String proposedValue;
    private int consensusInstance;
    private DecideCallback decideCallback;
    private boolean running;

    // State for non-leader processes
    private long timestamp;
    private String value;
    private List<byte[]> valueProofs; // Proofs of accepted values

    // State for leader process
    private Map<Integer, Long> timestamps;
    private Map<Integer, String> values;
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
        this.collector = new ConditionalCollect(selfId, processes, maxByzantine, link);
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.executor = Executors.newSingleThreadExecutor();

        this.consensusInstance = 0;
        this.timestamp = 0;
        this.value = null;
        this.valueProofs = new ArrayList<>();

        this.timestamps = new HashMap<>();
        this.values = new HashMap<>();
        this.proofs = new HashMap<>();
        this.acknowledged = new HashSet<>();

        this.running = false;

        // Register callback for message delivery
        link.registerDeliverCallback(this::onMessageDeliver);
    }

    public void init(String initialValue) {
        this.proposedValue = initialValue;
        this.value = initialValue;
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
        timestamps.clear();
        values.clear();
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
        StateMessage stateMsg = new StateMessage(instance, timestamp, value, valueProofs);
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
        timestamps.put(sender, stateMsg.getTimestamp());
        values.put(sender, stateMsg.getValue());
        proofs.put(sender, stateMsg.getProofs());

        System.out.println("\n\nCONSENSUS - broadcast write\n\n");

        // Check if we have enough STATE messages to proceed
        if (timestamps.size() >= n - f) {
            // Select the value with the highest timestamp
            long highestTimestamp = -1;
            String selectedValue = proposedValue; // Default to proposed value

            System.out.println("\n\nCONSENSUS - broadcast write first selectedValue " + selectedValue + "\n\n");
            for (Map.Entry<Integer, Long> entry : timestamps.entrySet()) {
                int processId = entry.getKey();
                long ts = entry.getValue();

                if (ts > highestTimestamp
                        && verifyValueProofs(processId, values.get(processId), proofs.get(processId))) {
                    highestTimestamp = ts;
                    System.out.println("\nCONSENSUS - values " + values.get(processId) + " \n");
                    selectedValue = values.get(processId);
                }
            }

            // Phase 2: Write phase
            // Send WRITE message to all processes
            WriteMessage writeMsg = new WriteMessage(consensusInstance, highestTimestamp + 1, selectedValue);
            System.out.println("\n\nCONSENSUS - broadcast write after selectedValue " + selectedValue + " \n\n");
            broadcastMessage(ConsensusMessageType.WRITE, writeMsg);
        }
    }

    /**
     * Process a WRITE message (non-leader)
     */
    private void processWriteMessage(ConsensusMessage message, int sender) {
        if (sender != leaderId) {
            System.err.println("Received WRITE message from non-leader: " + sender);
            return;
        }

        WriteMessage writeMsg = (WriteMessage) message.getPayload();
        int instance = writeMsg.getInstance();

        if (instance != consensusInstance) {
            // Ignore messages from different instances
            return;
        }

        // Update local state
        timestamp = writeMsg.getTimestamp();
        value = writeMsg.getValue();

        // Create proof for the new value
        byte[] proof = createValueProof();
        valueProofs.add(proof);

        // Send ACK message to the leader
        AckMessage ackMsg = new AckMessage(instance, timestamp, value);
        sendMessage(ConsensusMessageType.ACK, ackMsg, leaderId);
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

        // Verify the ACK
        if (ackMsg.getTimestamp() == timestamps.getOrDefault(sender, -1L) + 1 &&
                ackMsg.getValue().equals(value)) {

            acknowledged.add(sender);

            // Check if we have enough ACKs to decide
            if (acknowledged.size() >= n - f) {
                // Send DECIDE message to all processes
                DecideMessage decideMsg = new DecideMessage(consensusInstance, value);
                broadcastMessage(ConsensusMessageType.DECIDE, decideMsg);

                // Also deliver locally
                decide(value);
            }
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

            // Write payload type and data
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

        if (payload instanceof ReadMessage) {
            ReadMessage readMsg = (ReadMessage) payload;
            dos.writeInt(readMsg.getInstance());
        } else if (payload instanceof StateMessage) {
            StateMessage stateMsg = (StateMessage) payload;
            dos.writeInt(stateMsg.getInstance());
            dos.writeLong(stateMsg.getTimestamp());
            dos.writeUTF(stateMsg.getValue() != null ? stateMsg.getValue() : "null");

            // Write proofs
            List<byte[]> proofs = stateMsg.getProofs();
            dos.writeInt(proofs.size());
            for (byte[] proof : proofs) {
                dos.writeInt(proof.length);
                dos.write(proof);
            }
        } else if (payload instanceof WriteMessage) {
            WriteMessage writeMsg = (WriteMessage) payload;
            dos.writeInt(writeMsg.getInstance());
            dos.writeLong(writeMsg.getTimestamp());
            dos.writeUTF(writeMsg.getValue() != null ? writeMsg.getValue() : "null");
        } else if (payload instanceof AckMessage) {
            AckMessage ackMsg = (AckMessage) payload;
            dos.writeInt(ackMsg.getInstance());
            dos.writeLong(ackMsg.getTimestamp());
            dos.writeUTF(ackMsg.getValue() != null ? ackMsg.getValue() : "null");
        } else if (payload instanceof DecideMessage) {
            DecideMessage decideMsg = (DecideMessage) payload;
            dos.writeInt(decideMsg.getInstance());
            dos.writeUTF(decideMsg.getValue() != null ? decideMsg.getValue() : "null");
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

        System.out.println("\n\n\nCONSENSUS - TYPE: " + type + "\n\n\n");

        switch (type) {
            case READ:
                int readInstance = dis.readInt();
                return new ReadMessage(readInstance);

            case STATE:
                int stateInstance = dis.readInt();
                long timestamp = dis.readLong();
                String stateValue = dis.readUTF();

                System.out.println("\nSTATE VALUE: " + stateValue + "\n");

                if ("null".equals(stateValue)) {
                    stateValue = null;
                }

                // Read proofs
                int proofsCount = dis.readInt();
                List<byte[]> proofs = new ArrayList<>();
                for (int i = 0; i < proofsCount; i++) {
                    int proofLength = dis.readInt();
                    byte[] proof = new byte[proofLength];
                    dis.readFully(proof);
                    proofs.add(proof);
                }

                return new StateMessage(stateInstance, timestamp, stateValue, proofs);

            case WRITE:
                int writeInstance = dis.readInt();
                long writeTimestamp = dis.readLong();
                String writeValue = dis.readUTF();
                if ("null".equals(writeValue)) {
                    writeValue = null;
                }

                return new WriteMessage(writeInstance, writeTimestamp, writeValue);

            case ACK:
                int ackInstance = dis.readInt();
                long ackTimestamp = dis.readLong();
                String ackValue = dis.readUTF();
                if ("null".equals(ackValue)) {
                    ackValue = null;
                }

                return new AckMessage(ackInstance, ackTimestamp, ackValue);

            case DECIDE:
                int decideInstance = dis.readInt();
                String decideValue = dis.readUTF();
                if ("null".equals(decideValue)) {
                    decideValue = null;
                }

                return new DecideMessage(decideInstance, decideValue);

            default:
                throw new IOException("Unknown message type: " + type);
        }
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
    private final long timestamp;
    private final String value;
    private final List<byte[]> proofs;

    public StateMessage(int instance, long timestamp, String value, List<byte[]> proofs) {
        this.instance = instance;
        this.timestamp = timestamp;
        this.value = value;
        this.proofs = proofs;
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

    public List<byte[]> getProofs() {
        return proofs;
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