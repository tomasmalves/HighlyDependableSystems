package communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COLLECT message for Phase 2
 */
public class CollectMessage {
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
