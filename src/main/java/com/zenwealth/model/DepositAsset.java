package com.zenwealth.model;

public class DepositAsset extends Asset {
    public double amount;
    public double interestRate;

    public DepositAsset() {
        this.type = AssetType.DEPOSIT;
    }

    public DepositAsset(String name, double amount, double interestRate) {
        super(name, AssetType.DEPOSIT);
        this.amount = amount;
        this.interestRate = interestRate;
    }

    @Override
    public double getMarketValue() {
        return amount;
    }
}
