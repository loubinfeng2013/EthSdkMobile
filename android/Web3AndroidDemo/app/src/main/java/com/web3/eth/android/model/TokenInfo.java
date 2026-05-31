package com.web3.eth.android.model;

/**
 * ERC-20 Token 数据模型
 */
public class TokenInfo {

    private final String contractAddress;
    private String name;
    private String symbol;
    private String balance;

    public TokenInfo(String contractAddress) {
        this.contractAddress = contractAddress;
        this.name = contractAddress; // 获取到真实名称前先显示合约地址
        this.symbol = "--";
        this.balance = "--";
    }

    public TokenInfo(String contractAddress, String name, String symbol, String balance) {
        this.contractAddress = contractAddress;
        this.name = name;
        this.symbol = symbol;
        this.balance = balance;
    }

    public String getContractAddress() { return contractAddress; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }

    /** 显示用的简短合约地址，如 0x1234…abcd */
    public String getShortAddress() {
        if (contractAddress == null || contractAddress.length() < 10) return contractAddress;
        return contractAddress.substring(0, 6) + "…" + contractAddress.substring(contractAddress.length() - 4);
    }
}
