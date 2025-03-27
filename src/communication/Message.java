package communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Message {

    private final MessageType type;
    private final long sequenceNumber;
    private final long ackSequenceNumber; // Used only for ACK messages
    private final byte[] payload;

    public Message(MessageType type, long sequenceNumber, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.ackSequenceNumber = type == MessageType.ACK ? sequenceNumber : -1;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getAckSequenceNumber() {
        return ackSequenceNumber;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Serialize the message to bytes
     */
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write message type
            dos.writeInt(type.ordinal());

            // Write sequence number
            dos.writeLong(sequenceNumber);

            // Write ack sequence number
            dos.writeLong(ackSequenceNumber);

            // Write payload (may be null for ACK messages)
            if (payload == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(payload.length);
                dos.write(payload);
            }

            dos.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error serializing message", e);
        }
    }

    /**
     * Deserialize bytes to a Message
     */
    public static Message deserialize(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);

            // Read message type
            MessageType type = MessageType.values()[dis.readInt()];

            // Read sequence number
            long sequenceNumber = dis.readLong();

            // Read ack sequence number (discard, will be set in constructor)
            dis.readLong();

            // Read payload
            int payloadLength = dis.readInt();
            byte[] payload = null;
            if (payloadLength >= 0) {
                payload = new byte[payloadLength];
                dis.readFully(payload);
            }

            return new Message(type, sequenceNumber, payload);

        } catch (IOException e) {
            throw new RuntimeException("Error deserializing message", e);
        }
    }
}
