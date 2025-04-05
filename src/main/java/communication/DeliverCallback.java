package communication;

public interface DeliverCallback {
    /**
     * Called when a message is delivered
     * 
     * @param message The delivered message
     * @param sender  The sender process id
     */
    void onDeliver(Message message, int sender);
}
