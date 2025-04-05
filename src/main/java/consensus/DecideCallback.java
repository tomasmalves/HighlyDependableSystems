/**
 * Callback interface for consensus decisions
 */
package consensus;

public interface DecideCallback {
    /**
     * Called when a value is decided by the consensus
     * 
     * @param value The decided value
     */
    void onDecide(String value);
}