package accounts;

import java.math.BigInteger;

/**
 * Externally Owned Account (EOA) class
 */
public class EOAccount extends Account {

    /**
     * Constructor for creating a new EOA
     * 
     * @param address The address of the account
     * @param balance The initial balance
     * @param nonce   The initial nonce
     */
    public EOAccount(String address, BigInteger balance, long nonce) {
        super(address, balance, nonce);
    }
}
