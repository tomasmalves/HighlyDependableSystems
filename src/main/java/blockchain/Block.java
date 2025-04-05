package blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import accounts.Account;
import util.CryptoUtil;

/**
 * Represents a block in the DepChain blockchain
 */
public class Block {
    private final String previousBlockHash;
    private final long blockNumber;
    private final long timestamp;
    private final List<Transaction> transactions;
    private final Map<String, Account> state;
    private String blockHash;

    /**
     * Constructor for creating a new block
     * 
     * @param previousBlockHash The hash of the previous block
     * @param blockNumber       The block number
     * @param timestamp         The block timestamp
     * @param transactions      The list of transactions
     * @param state             The state after executing all transactions
     */
    public Block(String previousBlockHash, long blockNumber, long timestamp, List<Transaction> transactions,
            Map<String, Account> state) {
        this.previousBlockHash = previousBlockHash;
        this.blockNumber = blockNumber;
        this.timestamp = timestamp;
        this.transactions = transactions;
        this.state = state;
        this.blockHash = calculateBlockHash();
    }

    /**
     * Calculates the block hash
     * 
     * @return The block hash
     */
    private String calculateBlockHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(previousBlockHash == null ? "null" : previousBlockHash);
        sb.append(blockNumber);
        sb.append(timestamp);

        for (Transaction tx : transactions) {
            sb.append(tx.getHash());
        }

        return "0x" + CryptoUtil.bytesToHex(CryptoUtil.sha256(sb.toString()));
    }

    /**
     * Converts the block to a map for JSON serialization
     * 
     * @return A map representing the block
     */
    public Map<String, Object> toMap() {
        Map<String, Object> blockMap = new HashMap<>();
        blockMap.put("block_hash", blockHash);
        blockMap.put("previous_block_hash", previousBlockHash);
        blockMap.put("block_number", blockNumber);
        blockMap.put("timestamp", timestamp);

        List<Map<String, Object>> txMaps = new ArrayList<>();
        for (Transaction tx : transactions) {
            txMaps.add(tx.toMap());
        }
        blockMap.put("transactions", txMaps);

        Map<String, Map<String, Object>> stateMap = new HashMap<>();
        for (Map.Entry<String, Account> entry : state.entrySet()) {
            stateMap.put(entry.getKey(), entry.getValue().toMap());
        }
        blockMap.put("state", stateMap);

        return blockMap;
    }

    // Getters
    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Map<String, Account> getState() {
        return state;
    }

    public String getBlockHash() {
        return blockHash;
    }
}
