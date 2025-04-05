package communication;

/**
 * WRITE message for Phase 2
 */
public class WriteMessage {
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
