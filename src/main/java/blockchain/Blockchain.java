package blockchain;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import accounts.Account;
import accounts.ContractAccount;
import accounts.EOAccount;
import blockchain.Block;
import util.CryptoUtil;

/**
 * Main blockchain class that manages the blockchain state
 */
public class Blockchain {
    private final List<Block> blocks;
    private final Map<String, Account> currentState;
    private final String dataDir;
    private final ReadWriteLock lock;

    /**
     * Constructor for creating a new blockchain
     * 
     * @param genesisPath The path to the genesis file
     * @param dataDir     The directory to store blockchain data
     */
    public Blockchain(String genesisPath, String dataDir) throws Exception {
        this.blocks = new ArrayList<>();
        this.currentState = new HashMap<>();
        this.dataDir = dataDir;
        this.lock = new ReentrantReadWriteLock();

        // Load genesis block
        loadGenesisBlock(genesisPath);

        // Execute genesis block contracts
        executeGenesisContracts();

        // Load existing blocks
        loadExistingBlocks();
    }

    /**
     * Executes any contract initialization logic in the genesis block
     */
    private void executeGenesisContracts() throws Exception {
        // Get the genesis block
        if (blocks.isEmpty()) {
            throw new Exception("No genesis block found");
        }

        Block genesisBlock = blocks.get(0);
        Map<String, Account> genesisState = genesisBlock.getState();

        // Create EVM execution environment and SimpleWorld state
        SimpleWorld simpleWorld = new SimpleWorld();

        // First populate the world with all accounts
        for (Map.Entry<String, Account> entry : genesisState.entrySet()) {
            String address = entry.getKey();
            Account account = entry.getValue();

            // Convert address to Besu format
            Address besuAddress = Address.fromHexString(address);

            // Create account with balance and nonce
            simpleWorld.createAccount(besuAddress, account.getNonce(), Wei.of(account.getBalance()));

            // If contract account, add code and storage
            if (account instanceof EOAccount) {
                EOAccount eoaccount = (EOAccount) account;
                MutableAccount besuAccount = (MutableAccount) simpleWorld.get(besuAddress);
            }
        }

        // Then execute contract deployment initialization for each contract account
        for (Map.Entry<String, Account> entry : genesisState.entrySet()) {
            Account account = entry.getValue();

            if (account instanceof ContractAccount) {
                ContractAccount caccount = (ContractAccount) account;
                Address caddress = Address.fromHexString(caccount.getAddress());
                MutableAccount contractAccount = (MutableAccount) simpleWorld.get(caddress);
                Bytes code = caccount.getCode();

                // Skip if no code or code is empty
                if (code == null || code.bitLength() == 0 || code.toHexString().trim().equals("") ||
                        code.bitLength() == 1 && code.isZero()) {
                    continue;
                }

                // Log contract initialization
                System.out.println("Initializing contract at: " + caddress);

                // Create EVM executor for contract initialization
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream printStream = new PrintStream(byteArrayOutputStream);
                StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

                var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
                executor.tracer(tracer);

                // Contract deployment in genesis typically uses constructor arguments
                // which are usually embedded in the code
                executor.code(Bytes.wrap(code));

                // Use a default "system" address as sender for genesis deployment
                // or use the first EOA in genesis state
                String senderAddress = findGenesisDeployer(genesisState);
                executor.sender(Address.fromHexString(senderAddress));
                executor.receiver(caddress);
                executor.worldUpdater(simpleWorld.updater());

                // Execute initialization
                executor.execute();

                // Replace with runtime ISTCoin bytecode
                String runtime_bytecode = extractReturnData(byteArrayOutputStream);
                contractAccount.setCode(Bytes.fromHexString(runtime_bytecode));
                executor.code(contractAccount.getCode());
            }
        }
    }

    /**
     * Finds an appropriate address to use as the deployer for genesis contracts
     */
    private String findGenesisDeployer(Map<String, Account> genesisState) {
        // Default system address if no EOAs found
        String defaultAddress = "0x0000000000000000000000000000000000000000";

        // Try to find the first EOA account
        for (Map.Entry<String, Account> entry : genesisState.entrySet()) {
            if (entry.getValue() instanceof EOAccount) {
                return entry.getKey();
            }
        }

        return defaultAddress;
    }

    /**
     * Updates the genesis block with initialized contract state
     */
    private void updateGenesisBlock(Map<String, Account> updatedState) throws Exception {
        // Update genesis block
        Block genesisBlock = blocks.get(0);
        // This assumes Block class has a method to update its state
        // genesisBlock.updateState(updatedState);

        // Update current state
        currentState.clear();
        currentState.putAll(updatedState);

        // Save updated genesis block
        saveBlock(genesisBlock);
    }

    /**
     * Loads the genesis block from a file
     * 
     * @param genesisPath The path to the genesis file
     */
    private void loadGenesisBlock(String genesisPath) throws Exception {
        File genesisFile = new File(genesisPath);
        if (!genesisFile.exists()) {
            throw new Exception("Genesis file not found: " + genesisPath);
        }

        try (FileReader reader = new FileReader(genesisFile)) {
            JsonObject genesisJson = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, Account> genesisState = parseState(genesisJson.getAsJsonObject("state"));

            List<Transaction> transactions = new ArrayList<>();
            long timestamp = genesisJson.has("timestamp") ? genesisJson.get("timestamp").getAsLong()
                    : System.currentTimeMillis() / 1000;

            Block genesisBlock = new Block(null, 0, timestamp, transactions, genesisState);

            // Add genesis block
            blocks.add(genesisBlock);

            // Update current state
            currentState.putAll(genesisState);

            // Save genesis block
            saveBlock(genesisBlock);
        }
    }

    /**
     * Parses the state from a JSON object
     * 
     * @param stateJson The state JSON object
     * @return A map of accounts
     */
    private Map<String, Account> parseState(JsonObject stateJson) {
        Map<String, Account> state = new HashMap<>();

        for (String address : stateJson.keySet()) {
            JsonObject accountJson = stateJson.getAsJsonObject(address);
            BigInteger balance = new BigInteger(accountJson.get("balance").getAsString());
            long nonce = accountJson.has("nonce") ? accountJson.get("nonce").getAsLong() : 0;

            if (accountJson.has("code")) {
                // Contract account
                String codeHex = accountJson.get("code").getAsString();
                Bytes code = Bytes.fromHexString(codeHex);

                Map<String, String> storage = new HashMap<>();
                if (accountJson.has("storage")) {
                    JsonObject storageJson = accountJson.getAsJsonObject("storage");
                    for (String key : storageJson.keySet()) {
                        storage.put(key, storageJson.get(key).getAsString());
                    }
                }

                state.put(address, new ContractAccount(address, balance, nonce, code, storage));
            } else {
                // EOA account
                state.put(address, new EOAccount(address, balance, nonce));
            }
        }

        return state;
    }

    /**
     * Loads existing blocks from the data directory
     */
    private void loadExistingBlocks() {
        File dataDirectory = new File(dataDir);
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
            return;
        }

        // Genesis block is already loaded, so start from block 1
        long blockNumber = 1;
        while (true) {
            File blockFile = new File(dataDir, "block_" + blockNumber + ".json");
            if (!blockFile.exists()) {
                break;
            }

            try (FileReader reader = new FileReader(blockFile)) {
                JsonObject blockJson = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Account> blockState = parseState(blockJson.getAsJsonObject("state"));

                // Parse transactions
                List<Transaction> transactions = new ArrayList<>();
                // TODO: Parse transactions from blockJson

                Block block = new Block(
                        blockJson.get("previous_block_hash").getAsString(),
                        blockJson.get("block_number").getAsLong(),
                        blockJson.get("timestamp").getAsLong(),
                        transactions,
                        blockState);

                // Add block
                blocks.add(block);

                // Update current state
                currentState.putAll(blockState);

                blockNumber++;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Adds a new block to the blockchain
     * 
     * @param transactions The list of transactions to include in the block
     * @return The newly created block
     */
    public Block addBlock(List<Transaction> transactions) throws Exception {
        lock.writeLock().lock();
        try {
            Block latestBlock = getLatestBlock();
            Map<String, Account> newState = executeTransactions(transactions);

            Block newBlock = new Block(
                    latestBlock.getBlockHash(),
                    latestBlock.getBlockNumber() + 1,
                    System.currentTimeMillis() / 1000,
                    transactions,
                    newState);

            // Add block
            blocks.add(newBlock);

            // Update current state
            currentState.putAll(newState);

            // Save block
            saveBlock(newBlock);

            return newBlock;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Executes transactions and returns the new state
     *
     * @param transactions The list of transactions to execute
     * @return The new state after executing all transactions
     */
    private Map<String, Account> executeTransactions(List<Transaction> transactions) throws Exception {
        // Start with a copy of the current state
        Map<String, Account> newState = new HashMap<>(currentState);

        // Create EVM executor
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        executor.code(Bytes.fromHexString(""));
        // executor.sender(senderAddress);
        // executor.receiver(contractAddress);
        // executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();

        // Execute each transaction
        for (Transaction tx : transactions) {
            // Verify the transaction signature
            if (!verifyTransaction(tx)) {
                throw new Exception("Invalid transaction signature: " + tx.getHash());
            }

            // Check if sender has enough balance for the transaction
            Account sender = newState.get(tx.getFrom());
            if (sender == null) {
                throw new Exception("Sender account not found: " + tx.getFrom());
            }

            if (sender.getBalance().compareTo(tx.getValue()) < 0) {
                throw new Exception("Insufficient balance for transaction: " + tx.getHash());
            }

            // Check nonce
            if (sender.getNonce() != tx.getNonce()) {
                throw new Exception("Invalid nonce for transaction: " + tx.getHash());
            }

            // Execute the transaction
            if (tx.getData() != null && tx.getData().bitLength() > 0) {
                // Contract call
                // executor.executeTransaction(tx);
                executor.callData(Bytes.fromHexString("f1351b93"));
                executor.execute();
            } else {
                // Simple value transfer
                Account recipient = newState.get(tx.getTo());
                if (recipient == null) {
                    // Create new EOA
                    recipient = new EOAccount(tx.getTo(), BigInteger.ZERO, 0);
                    newState.put(tx.getTo(), recipient);
                }

                // Transfer value
                sender.setBalance(sender.getBalance().subtract(tx.getValue()));
                recipient.setBalance(recipient.getBalance().add(tx.getValue()));
            }

            // Increment sender nonce
            sender.incrementNonce();
        }

        return newState;
    }

    public static String extractReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        System.out.println("Size of content: " + byteArrayOutputStream.toString().length());
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        return memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
    }

    public static BigInteger extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();

        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        // Extract the hex string from memory
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        // Convert to BigInteger (supports large values)
        return new BigInteger(returnData, 16);
    }

    private static String extractRevertReasonFromTrace(String traceOutput) {
        // This is a simplified implementation - in reality, you would need more robust
        // parsing
        // Look for revert reason in output trace - this could vary based on how tracer
        // works
        if (traceOutput.contains("revertReason")) {
            // Try to extract from JSON
            try {
                String[] lines = traceOutput.split("\\r?\\n");
                for (String line : lines) {
                    if (line.contains("revertReason")) {
                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        if (json.has("revertReason")) {
                            return json.get("revertReason").getAsString();
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to simple string search if JSON parsing fails
                int start = traceOutput.indexOf("revertReason") + "revertReason".length() + 3; // Skip ": "
                int end = traceOutput.indexOf("\"", start);
                if (start > 0 && end > start) {
                    return traceOutput.substring(start, end);
                }
            }
        }

        // Default message if we couldn't extract a specific reason
        return "Transaction reverted";
    }

    private static String readStringFromStorage(SimpleWorld world, Address istCoinAddress, int slot) {
        // This is a simplified implementation - reading strings from storage is complex
        // In a real implementation, you'd need to handle both short and long strings
        return world.get(istCoinAddress).getStorageValue(UInt256.valueOf(slot)).toString();
    }

    private static String calculateMappingKey(String address, int mappingSlot) {
        if (address.startsWith("0x")) {
            address = address.substring(2);
        }

        String paddedAddress = padHexStringTo256Bit(address);
        String slotIndex = convertIntegerToHex256Bit(mappingSlot);

        return Numeric.toHexStringNoPrefix(
                Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + slotIndex)));
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);
        return String.format("%064x", bigInt);
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) + hexString;
    }

    /**
     * Verifies a transaction's signature
     * 
     * @param tx The transaction to verify
     * @return True if the signature is valid, false otherwise
     */
    private boolean verifyTransaction(Transaction tx) {
        // TODO: Implement transaction verification
        // This would involve getting the sender's public key from their address
        // and verifying the transaction signature
        return true;
    }

    /**
     * Saves a block to a file
     * 
     * @param block The block to save
     */
    private void saveBlock(Block block) throws Exception {
        File blockFile = new File(dataDir, "block_" + block.getBlockNumber() + ".json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String blockJson = gson.toJson(block.toMap());

        try (FileWriter writer = new FileWriter(blockFile)) {
            writer.write(blockJson);
        }
    }

    /**
     * Gets the latest block in the blockchain
     * 
     * @return The latest block
     */
    public Block getLatestBlock() {
        lock.readLock().lock();
        try {
            return blocks.get(blocks.size() - 1);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the current state of the blockchain
     * 
     * @return The current state
     */
    public Map<String, Account> getCurrentState() {
        lock.readLock().lock();
        try {
            return new HashMap<>(currentState);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the balance of an account
     * 
     * @param address The account address
     * @return The account balance
     */
    public BigInteger getBalance(String address) {
        lock.readLock().lock();
        try {
            Account account = currentState.get(address);
            return account != null ? account.getBalance() : BigInteger.ZERO;
        } finally {
            lock.readLock().unlock();
        }
    }
}