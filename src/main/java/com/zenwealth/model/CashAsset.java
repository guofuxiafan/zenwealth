package com.zenwealth.model;

public class CashAsset extends Asset {
    public double amount;

    public CashAsset() {
        this.type = AssetType.CASH;
    }

    public CashAsset(String name, double amount) {
        super(name, AssetType.CASH);
        this.amount = amount;
    }

    @Override
    public double getMarketValue() {
        return amount;
    }
}
