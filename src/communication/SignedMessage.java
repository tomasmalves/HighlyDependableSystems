package communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Signed message wrapper
 */
public class SignedMessage {
    private final Message message;
    private final byte[] signature;
    private final int senderId;

    public SignedMessage(Message message, byte[] signature, int senderId) {
        this.message = message;
        this.signature = signature;
        this.senderId = senderId;
    }

    public Message getMessage() {
        return message;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getSenderId() {
        return senderId;
    }

    /**
     * Serialize the signed message to bytes
     */
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write sender ID
            dos.writeInt(senderId);

            // Write serialized message
            byte[] messageBytes = message.serialize();
            dos.writeInt(messageBytes.length);
            dos.write(messageBytes);

            // Write signature
            dos.writeInt(signature.length);
            dos.write(signature);

            dos.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error serializing signed message", e);
        }
    }

    /**
     * Deserialize bytes to a SignedMessage
     */
    public static SignedMessage deserialize(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);

            // Read sender ID
            int senderId = dis.readInt();

            // Read serialized message
            int messageLength = dis.readInt();
            byte[] messageBytes = new byte[messageLength];
            dis.readFully(messageBytes);
            Message message = Message.deserialize(messageBytes);

            // Read signature
            int signatureLength = dis.readInt();
            byte[] signature = new byte[signatureLength];
            dis.readFully(signature);

            return new SignedMessage(message, signature, senderId);

        } catch (IOException e) {
            throw new RuntimeException("Error deserializing signed message", e);
        }
    }
}