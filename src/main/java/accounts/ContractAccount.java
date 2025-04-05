package accounts;

import java.math.BigInteger;
import java.util.Map;

/**
 * Contract Account class
 */
public class ContractAccount extends Account {
    private final byte[] code;
    private final Map<String, String> storage;

    /**
     * Constructor for creating a new contract account
     * 
     * @param address The address of the account
     * @param balance The initial balance
     * @param nonce   The initial nonce
     * @param code    The contract bytecode
     * @param storage The initial storage state
     */
    public ContractAccount(String address, BigInteger balance, long nonce, byte[] code, Map<String, String> storage) {
        super(address, balance, nonce);
        this.code = code;
        this.storage = storage;
    }

    /**
     * Gets the contract bytecode
     * 
     * @return The contract bytecode
     */
    public byte[] getCode() {
        return code;
    }

    /**
     * Gets the contract storage
     * 
     * @return The contract storage
     */
    public Map<String, String> getStorage() {
        return storage;
    }

    /**
     * Updates a key in the contract storage
     * 
     * @param key   The storage key
     * @param value The storage value
     */
    public void updateStorage(String key, String value) {
        storage.put(key, value);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("code", "0x" + bytesToHex(code));
        map.put("storage", storage);
        return map;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}