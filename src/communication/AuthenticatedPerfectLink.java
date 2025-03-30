package communication;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import consensus.ProcessInfo;

/**
 * Implementation of Authenticated Perfect Links over UDP
 */
public class AuthenticatedPerfectLink {

    private final int selfId;
    private final Map<Integer, ProcessInfo> processes;
    private final PrivateKey privateKey;
    private final DatagramSocket socket;
    private final ExecutorService executor;
    private final Set<MessageId> delivered;
    private final Map<MessageId, Message> pendingAcks;

    private DeliverCallback deliverCallback;
    private boolean running;

    /**
     * Constructor
     * 
     * @param selfId     The ID of this process
     * @param processes  Map of process IDs to their network information
     * @param privateKey Private key of this process
     * @param port       UDP port to bind to
     */
    public AuthenticatedPerfectLink(int selfId, Map<Integer, ProcessInfo> processes, PrivateKey privateKey,
            int port)
            throws SocketException {
        this.selfId = selfId;
        this.processes = processes;
        this.privateKey = privateKey;
        this.socket = new DatagramSocket(port);
        this.executor = Executors.newFixedThreadPool(2); // One for receiving, one for retransmission
        this.delivered = Collections.synchronizedSet(new HashSet<>());
        this.pendingAcks = new ConcurrentHashMap<>();
        this.running = false;
    }

    public void send(Message message, int destination) {
        if (!running) {
            throw new IllegalStateException("AuthenticatedPerfectLink is not running");
        }

        if (!processes.containsKey(destination)) {
            throw new IllegalArgumentException("Unknown destination process: " + destination);
        }

        // Sign the message
        SignedMessage signedMessage = signMessage(message);

        // Store for potential retransmission
        MessageId msgId;

        if (message.getType() == MessageType.DATA) {
            // Extract consensus message type for DATA messages
            ConsensusMessageType consensusType = extractConsensusType(message);
            msgId = new MessageId(consensusType, message.getSequenceNumber(), selfId, destination);
        } else {
            // For ACK messages, use the basic constructor
            msgId = new MessageId(message.getSequenceNumber(), selfId, destination);
        }

        pendingAcks.put(msgId, message);

        // Send the message
        sendSignedMessage(signedMessage, destination);
    }

    public void registerDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        // Start receiving thread
        executor.submit(this::receiveLoop);

        // Start acknowledgment checking and retransmission
        // executor.submit(this::retransmissionLoop);
    }

    public void stop() {
        running = false;
        executor.shutdown();
        socket.close();
    }

    /**
     * Main receive loop
     */
    private void receiveLoop() {
        byte[] buffer = new byte[8192]; // Adjust size as needed
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Deserialize the signed message
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                SignedMessage signedMessage = SignedMessage.deserialize(data);

                // Process the message
                processIncomingMessage(signedMessage);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error receiving UDP packet: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println(
                        "Error processing incoming message. Trying to understand it as a client message...");

            }
        }
    }

    /**
     * Process an incoming signed message
     */
    private void processIncomingMessage(SignedMessage signedMessage) {
        int senderId = signedMessage.getSenderId();

        // Verify the sender exists
        if (!processes.containsKey(senderId)) {
            System.err.println("Received message from unknown sender: " + senderId);
            return;
        }

        // Get sender's public key
        PublicKey senderPublicKey = processes.get(senderId).getPublicKey();

        Message message = signedMessage.getMessage();

        // Create a MessageId based on message type
        MessageId msgId;

        if (message.getType() == MessageType.DATA) {
            // Extract consensus message type for DATA messages
            ConsensusMessageType consensusType = extractConsensusType(message);
            msgId = new MessageId(consensusType, message.getSequenceNumber(), senderId, selfId);
        } else {
            // For ACK messages, use the basic constructor
            msgId = new MessageId(message.getSequenceNumber(), senderId, selfId);
        }

        System.out.println("AUTH - processIncomingMessage - type: " + message.getType() +
                (message.getType() == MessageType.DATA ? " consensus type: " + extractConsensusType(message) : "") +
                " with sequencenumber: " + msgId.sequenceNumber);

        // Handle different message types
        switch (message.getType()) {
            case DATA:
                // If this is a new message (including consensus type), deliver it and send ACK
                if (delivered.add(msgId)) {
                    if (deliverCallback != null) {
                        deliverCallback.onDeliver(message, senderId);
                    }
                }
                // Always send ACK, even for duplicates
                sendAcknowledgment(message, senderId);
                break;

            case ACK:
                // Remove from pending acknowledgments
                // For ACK messages, we need to reconstruct the original message ID
                // This depends on how you're tracking sent messages
                MessageId originalMsgId = new MessageId(message.getAckSequenceNumber(), selfId, senderId);
                pendingAcks.remove(originalMsgId);
                break;

            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    /**
     * Send an acknowledgment for a received message
     */
    private void sendAcknowledgment(Message receivedMsg, int destination) {
        Message ackMessage = new Message(
                MessageType.ACK,
                receivedMsg.getSequenceNumber(), // Set as ackSequenceNumber
                null // No payload for ACK
        );
        SignedMessage signedAck = signMessage(ackMessage);
        sendSignedMessage(signedAck, destination);
    }

    /**
     * Retransmission loop for reliable delivery
     */
    private void retransmissionLoop() {
        while (running) {
            try {
                // Check all pending messages
                for (Map.Entry<MessageId, Message> entry : pendingAcks.entrySet()) {
                    MessageId msgId = entry.getKey();
                    Message message = entry.getValue();

                    // Retransmit
                    SignedMessage signedMessage = signMessage(message);
                    sendSignedMessage(signedMessage, msgId.getDestination());
                }

                // Sleep before next retransmission cycle
                Thread.sleep(1000); // 1 second, adjust as needed

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error in retransmission loop: " + e.getMessage());
            }
        }
    }

    /**
     * Schedule periodic retransmission of a message
     */
    private void scheduleRetransmission(MessageId msgId, int destination) {
        // The actual retransmission is handled by the retransmissionLoop
    }

    /**
     * Sign a message using this process's private key
     */
    private SignedMessage signMessage(Message message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(message.serialize());

            byte[] signatureBytes = signature.sign();
            return new SignedMessage(message, signatureBytes, selfId);

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("Failed to sign message: " + e.getMessage(), e);
        }
    }

    /**
     * Verify the signature on a signed message
     */
    private boolean verifySignature(SignedMessage signedMessage, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(signedMessage.getMessage().serialize());

            return signature.verify(signedMessage.getSignature());

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            System.err.println("Error verifying signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a signed message over UDP
     */
    private void sendSignedMessage(SignedMessage signedMessage, int destination) {
        try {
            ProcessInfo destInfo = processes.get(destination);
            byte[] data = signedMessage.serialize();

            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(destInfo.getHost()),
                    destInfo.getPort());

            socket.send(packet);

        } catch (IOException e) {
            System.err.println("Error sending UDP packet: " + e.getMessage());
        }
    }

    private ConsensusMessageType extractConsensusType(Message message) {
        if (message.getType() != MessageType.DATA || message.getPayload() == null) {
            return null;
        }

        try {
            // Extract ConsensusMessage from the payload
            ConsensusMessage consensusMsg = ConsensusMessage.deserialize(message.getPayload());
            return consensusMsg.getType();
        } catch (Exception e) {
            System.err.println("Failed to extract consensus type: " + e.getMessage());
            return null;
        }
    }

    /**
     * Class to identify a message uniquely
     */
    private static class MessageId {
        private final MessageType messageType;
        private final ConsensusMessageType consensusPhase;
        private final long sequenceNumber;
        private final int sender;
        private final int destination;

        // Constructor for consensus messages
        public MessageId(long sequenceNumber, int sender, int destination) {
            this.messageType = null; // Consensus messages are DATA messages
            this.consensusPhase = null;
            this.sequenceNumber = sequenceNumber;
            this.sender = sender;
            this.destination = destination;
        }

        // Constructor for consensus messages
        public MessageId(ConsensusMessageType consensusPhase, long sequenceNumber, int sender, int destination) {
            this.messageType = MessageType.DATA; // Consensus messages are DATA messages
            this.consensusPhase = consensusPhase;
            this.sequenceNumber = sequenceNumber;
            this.sender = sender;
            this.destination = destination;
        }

        public int getDestination() {
            return destination;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            MessageId messageId = (MessageId) o;

            if (sequenceNumber != messageId.sequenceNumber)
                return false;
            if (sender != messageId.sender)
                return false;
            if (destination != messageId.destination)
                return false;
            // For ACK messages, we only compare the above fields
            if (messageType == MessageType.ACK && messageId.messageType == MessageType.ACK)
                return true;
            // For DATA messages, also compare the consensus phase
            return consensusPhase == messageId.consensusPhase;
        }

        @Override
        public int hashCode() {
            int result = messageType == MessageType.ACK ? 0 : (consensusPhase != null ? consensusPhase.hashCode() : 0);
            result = 31 * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
            result = 31 * result + sender;
            result = 31 * result + destination;
            return result;
        }
    }
}
