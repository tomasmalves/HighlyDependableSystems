package blockchain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GenesisBlockLoader {
    public Block loadGenesisBlock(String genesisFilePath) throws IOException {
        // Read the genesis file
        String genesisJson = Files.readString(Path.of(genesisFilePath));
        
        // Parse JSON to your Block object using Jackson or similar
        ObjectMapper mapper = new ObjectMapper();
        Block genesisBlock = mapper.readValue(genesisJson, Block.class);
        
        // Validate the genesis block
        validateGenesisBlock(genesisBlock);
        
        return genesisBlock;
    }
    
    private void validateGenesisBlock(Block block) {
        // Ensure it's block 0
        assert block.getBlockNumber() == 0;
        
        // Ensure previous hash is null
        assert block.getPreviousBlockHash() == null;
        
        // Add other validations as needed
    }
}