package communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STATE message for Phase 1 response
 */
public class StateMessage {
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