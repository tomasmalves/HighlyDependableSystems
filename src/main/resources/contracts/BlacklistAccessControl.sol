// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title BlacklistAccessControl
 * @dev Contract that provides blacklist functionality
 */
contract BlacklistAccessControl is Ownable {
    mapping(address => bool) private _blacklisted;
    
    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    /**
     * @dev Constructor
     */
    constructor() Ownable(msg.sender) {}

    /**
     * @dev Adds an address to the blacklist
     * @param account Address to add to the blacklist
     * @return bool indicating if the operation was successful
     */
    function addToBlacklist(address account) external onlyOwner returns (bool) {
        require(account != address(0), "BlacklistAccessControl: cannot blacklist the zero address");
        require(!_blacklisted[account], "BlacklistAccessControl: account already blacklisted");
        
        _blacklisted[account] = true;
        emit AddedToBlacklist(account);
        return true;
    }

    /**
     * @dev Removes an address from the blacklist
     * @param account Address to remove from the blacklist
     * @return bool indicating if the operation was successful
     */
    function removeFromBlacklist(address account) external onlyOwner returns (bool) {
        require(_blacklisted[account], "BlacklistAccessControl: account not blacklisted");
        
        _blacklisted[account] = false;
        emit RemovedFromBlacklist(account);
        return true;
    }

    /**
     * @dev Checks if an address is blacklisted
     * @param account Address to check
     * @return bool indicating if the address is blacklisted
     */
    function isBlacklisted(address account) public view returns (bool) {
        return _blacklisted[account];
    }
}