package communication;

/**
 * Types of consensus messages
 */
public enum ConsensusMessageType {
    READ,
    STATE,
    COLLECT,
    WRITE,
    ACK,
    DECIDE
}