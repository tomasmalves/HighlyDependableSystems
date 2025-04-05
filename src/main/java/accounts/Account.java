package accounts;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Base account class representing an account in the DepChain blockchain
 */
public abstract class Account {
    protected final String address;
    protected BigInteger balance;
    protected long nonce;

    /**
     * Constructor for creating a new account
     * 
     * @param address The address of the account
     * @param balance The initial balance
     * @param nonce   The initial nonce
     */
    public Account(String address, BigInteger balance, long nonce) {
        this.address = address;
        this.balance = balance;
        this.nonce = nonce;
    }

    /**
     * Gets the address of the account
     * 
     * @return The account address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the balance of the account
     * 
     * @return The account balance
     */
    public BigInteger getBalance() {
        return balance;
    }

    /**
     * Sets the balance of the account
     * 
     * @param balance The new balance
     */
    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    /**
     * Gets the nonce of the account
     * 
     * @return The account nonce
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Increments the nonce of the account
     */
    public void incrementNonce() {
        this.nonce++;
    }

    /**
     * Converts the account to a map for JSON serialization
     * 
     * @return A map representing the account
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("balance", balance.toString());
        map.put("nonce", nonce);
        return map;
    }
}
