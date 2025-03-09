package blockchain;

import java.util.List;
import java.util.ArrayList;

public class BlockchainService {
private final List<String> blockchain;
    
    public BlockchainService() {
        this.blockchain = new ArrayList<>();
    }
    
    public synchronized void appendBlock(String data) {
        blockchain.add(data);
        System.out.println("Block added: " + data);
    }
    
    public synchronized List<String> getBlockchain() {
        return new ArrayList<>(blockchain);
    }
}
