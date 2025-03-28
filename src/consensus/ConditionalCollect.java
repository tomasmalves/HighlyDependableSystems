package consensus;

import communication.AuthenticatedPerfectLink;
import communication.DeliverCallback;
import communication.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Implementation of the Conditional Collect abstraction
 */
public class ConditionalCollect {

    private final int selfId;
    private final List<Integer> processes;
    private final int maxByzantine;
    private final AuthenticatedPerfectLink link;
    private final Map<Long, CollectInstance> activeCollects;
    private final AtomicLong collectIdCounter;

    /**
     * Constructor
     * 
     * @param selfId       The ID of this process
     * @param processes    List of all process IDs
     * @param maxByzantine Maximum number of Byzantine processes
     * @param link         The authenticated perfect link
     */
    public ConditionalCollect(int selfId, List<Integer> processes, int maxByzantine, AuthenticatedPerfectLink link) {
        this.selfId = selfId;
        this.processes = processes;
        this.maxByzantine = maxByzantine;
        this.link = link;
        this.activeCollects = new ConcurrentHashMap<>();
        this.collectIdCounter = new AtomicLong(0);

        // Register callback for message delivery
        link.registerDeliverCallback(new CollectDeliverCallback());
    }

    /**
     * Start a new conditional collect operation
     * 
     * @param request   The request to send to all processes
     * @param condition The condition that must be satisfied to complete the collect
     * @param timeout   Maximum time to wait for responses (milliseconds)
     * @return A future that completes with the collected responses
     */
    public CompletableFuture<List<CollectResponse>> collect(
            byte[] request,
            Predicate<List<CollectResponse>> condition,
            long timeout) {

        long collectId = collectIdCounter.incrementAndGet();
        CompletableFuture<List<CollectResponse>> future = new CompletableFuture<>();

        // Create and store the collect instance
        CollectInstance instance = new CollectInstance(collectId, request, condition, future);
        activeCollects.put(collectId, instance);

        // Send request to all processes
        for (int processId : processes) {
            sendCollectRequest(instance, processId);
        }

        // Schedule timeout
        scheduleTimeout(instance, timeout);

        return future;
    }

    /**
     * Send a collect request to a specific process
     */
    private void sendCollectRequest(CollectInstance instance, int destination) {
        try {
            // Create a collect request message
            CollectMessage2 collectMsg = new CollectMessage2(
                    CollectMessageType.REQUEST,
                    instance.getCollectId(),
                    instance.getRequest());

            // Serialize and send
            byte[] payload = collectMsg.serialize();
            Message message = new Message(
                    communication.MessageType.DATA,
                    instance.getCollectId(),
                    payload);

            link.send(message, destination);

        } catch (Exception e) {
            System.err.println("Error sending collect request: " + e.getMessage());
        }
    }

    /**
     * Process a collect request and send a response
     */
    private void processCollectRequest(long collectId, byte[] request, int sender) {
        try {
            // Generate response (application-specific logic would go here)
            byte[] response = generateResponse(request);

            // Create a collect response message
            CollectMessage2 collectMsg = new CollectMessage2(
                    CollectMessageType.RESPONSE,
                    collectId,
                    response);

            // Serialize and send
            byte[] payload = collectMsg.serialize();
            Message message = new Message(
                    communication.MessageType.DATA,
                    collectId,
                    payload);

            link.send(message, sender);

        } catch (Exception e) {
            System.err.println("Error processing collect request: " + e.getMessage());
        }
    }

    /**
     * Process a collect response
     */
    private void processCollectResponse(long collectId, byte[] response, int sender) {
        CollectInstance instance = activeCollects.get(collectId);

        if (instance == null) {
            // This collect is no longer active
            return;
        }

        // Add the response
        CollectResponse collectResponse = new CollectResponse(sender, response);
        instance.addResponse(collectResponse);

        // Check if the condition is now satisfied
        List<CollectResponse> responses = instance.getResponses();
        if (instance.getCondition().test(responses)) {
            // Condition satisfied, complete the future
            CompletableFuture<List<CollectResponse>> future = instance.getFuture();
            if (!future.isDone()) {
                future.complete(new ArrayList<>(responses));
                activeCollects.remove(collectId);
            }
        }
    }

    /**
     * Generate a response for a collect request (application-specific)
     */
    private byte[] generateResponse(byte[] request) {
        // In a real implementation, this would process the request and generate a
        // response
        // For this example, we'll just return a simple response
        String requestStr = new String(request, StandardCharsets.UTF_8);
        String responseStr = "Response from " + selfId + " to: " + requestStr;
        return responseStr.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Schedule a timeout for a collect operation
     */
    private void scheduleTimeout(CollectInstance instance, long timeout) {
        CompletableFuture.delayedExecutor(timeout, TimeUnit.MILLISECONDS).execute(() -> {
            CompletableFuture<List<CollectResponse>> future = instance.getFuture();
            if (!future.isDone()) {
                List<CollectResponse> responses = instance.getResponses();
                if (instance.getCondition().test(responses)) {
                    future.complete(new ArrayList<>(responses));
                } else {
                    future.completeExceptionally(new TimeoutException("Collect operation timed out"));
                }
                activeCollects.remove(instance.getCollectId());
            }
        });
    }

    /**
     * Callback for message delivery
     */
    private class CollectDeliverCallback implements DeliverCallback {
        @Override
        public void onDeliver(Message message, int sender) {
            try {
                // Deserialize the collect message
                CollectMessage2 collectMsg = CollectMessage2.deserialize(message.getPayload());
                long collectId = collectMsg.getCollectId();

                switch (collectMsg.getType()) {
                    case REQUEST:
                        processCollectRequest(collectId, collectMsg.getPayload(), sender);
                        break;

                    case RESPONSE:
                        processCollectResponse(collectId, collectMsg.getPayload(), sender);
                        break;

                    default:
                        System.err.println("Unknown collect message type: " + collectMsg.getType());
                }

            } catch (Exception e) {
                System.err.println("Error processing collect message: " + e.getMessage());
            }
        }
    }

    /**
     * Exception for timeout
     */
    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}

/**
 * Types of collect messages
 */
enum CollectMessageType {
    REQUEST,
    RESPONSE
}

/**
 * Collect message class
 */
class CollectMessage2 {
    private final CollectMessageType type;
    private final long collectId;
    private final byte[] payload;

    public CollectMessage2(CollectMessageType type, long collectId, byte[] payload) {
        this.type = type;
        this.collectId = collectId;
        this.payload = payload;
    }

    public CollectMessageType getType() {
        return type;
    }

    public long getCollectId() {
        return collectId;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Serialize the collect message to bytes
     */
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write message type
            dos.writeInt(type.ordinal());

            // Write collect ID
            dos.writeLong(collectId);

            // Write payload
            if (payload == null) {
                dos.writeInt(-1);
            } else {
                dos.writeInt(payload.length);
                dos.write(payload);
            }

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error serializing collect message", e);
        }
    }

    /**
     * Deserialize bytes to a CollectMessage
     */
    public static CollectMessage2 deserialize(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);

            // Read message type
            CollectMessageType type = CollectMessageType.values()[dis.readInt()];

            // Read collect ID
            long collectId = dis.readLong();

            // Read payload
            int payloadLength = dis.readInt();
            byte[] payload = null;
            if (payloadLength >= 0) {
                payload = new byte[payloadLength];
                dis.readFully(payload);
            }

            return new CollectMessage2(type, collectId, payload);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing collect message", e);
        }
    }
}

/**
 * Response for a collect operation
 */
class CollectResponse {
    private final int sender;
    private final byte[] response;

    public CollectResponse(int sender, byte[] response) {
        this.sender = sender;
        this.response = response;
    }

    public int getSender() {
        return sender;
    }

    public byte[] getResponse() {
        return response;
    }
}

/**
 * Instance of an ongoing collect operation
 */
class CollectInstance {
    private final long collectId;
    private final byte[] request;
    private final Predicate<List<CollectResponse>> condition;
    private final CompletableFuture<List<CollectResponse>> future;
    private final List<CollectResponse> responses;

    public CollectInstance(
            long collectId,
            byte[] request,
            Predicate<List<CollectResponse>> condition,
            CompletableFuture<List<CollectResponse>> future) {
        this.collectId = collectId;
        this.request = request;
        this.condition = condition;
        this.future = future;
        this.responses = new ArrayList<>();
    }

    public synchronized void addResponse(CollectResponse response) {
        responses.add(response);
    }

    public synchronized List<CollectResponse> getResponses() {
        return new ArrayList<>(responses);
    }

    public long getCollectId() {
        return collectId;
    }

    public byte[] getRequest() {
        return request;
    }

    public Predicate<List<CollectResponse>> getCondition() {
        return condition;
    }

    public CompletableFuture<List<CollectResponse>> getFuture() {
        return future;
    }
}