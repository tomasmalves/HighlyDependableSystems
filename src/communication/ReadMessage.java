package communication;

/**
 * READ message for Phase 1
 */
public class ReadMessage {
    private final int instance;

    public ReadMessage(int instance) {
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }
}
