// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "./BlacklistAccessControl.sol";


/**
 * @title ISTCoin
 * @dev Implementation of the IST Coin ERC-20 token with blacklist checking
 */
contract ISTCoin is ERC20, Ownable, BlacklistAccessControl {
    uint8 private _decimals = 2;
    uint256 private _totalSupply = 100000000 * (10 ** uint256(_decimals)); // 100 million units

    /**
     * @dev Constructor that gives the msg.sender all of existing tokens.
     */
    constructor() ERC20("IST Coin", "IST") BlacklistAccessControl() {
        _mint(msg.sender, _totalSupply);
    }

    /**
     * @dev See {ERC20-decimals}.
     */
    function decimals() public view virtual override returns (uint8) {
        return _decimals;
    }


    /**
     * @dev See {ERC20-_update}.
     * Overrides the _update function to include blacklist checking
     */
    function _update(address from, address to, uint256 value) internal virtual override {
        if (from != address(0) && isBlacklisted(from)) {
            revert("ISTCoin: sender is blacklisted");
        }
        if (to != address(0) && isBlacklisted(to)) {
            revert("ISTCoin: recipient is blacklisted");
        }
        super._update(from, to, value);
    }
}