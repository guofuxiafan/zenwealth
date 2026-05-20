package com.zenwealth.model;

public class EquityAsset extends Asset {
    public String code;
    public double quantity;
    public double unitPrice;

    public EquityAsset() {}

    public EquityAsset(String name, AssetType type, String code, double quantity, double unitPrice) {
        super(name, type);
        this.code = code;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    @Override
    public double getMarketValue() {
        return quantity * unitPrice;
    }
}
