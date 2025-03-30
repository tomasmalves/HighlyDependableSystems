package communication;

/**
 * ACK message for Phase 2 response
 */

public class AckMessage {
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
