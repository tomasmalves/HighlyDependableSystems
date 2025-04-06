package accounts;

import java.math.BigInteger;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;

/**
 * Contract Account class
 */
public class ContractAccount extends Account {
    private Bytes code;
    private Map<String, String> storage;

    /**
     * Constructor for creating a new contract account
     * 
     * @param address The address of the account
     * @param balance The initial balance
     * @param nonce   The initial nonce
     * @param code    The contract bytecode
     * @param storage The initial storage state
     */
    public ContractAccount(String address, BigInteger balance, long nonce, Bytes code, Map<String, String> storage) {
        super(address, balance, nonce);
        this.code = code;
        this.storage = storage;
    }

    /**
     * Gets the contract bytecode
     * 
     * @return The contract bytecode
     */
    public Bytes getCode() {
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

    public void setCode(Bytes code) {
        this.code = code;
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

    /**
     * Updates a key in the contract storage
     * 
     * @param key   The storage key
     * @param value The storage value
     */
    public void updateStorage(Map<String, String> updatedStorage) {
        this.storage = updatedStorage;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("code", code.toHexString());
        map.put("storage", storage);
        return map;
    }
}