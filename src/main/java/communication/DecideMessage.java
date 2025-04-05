package communication;

/**
 * DECIDE message for the final decision
 */
public class DecideMessage {
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
