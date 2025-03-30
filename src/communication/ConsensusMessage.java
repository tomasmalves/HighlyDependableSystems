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
 * Base class for consensus messages
 */
public class ConsensusMessage {
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