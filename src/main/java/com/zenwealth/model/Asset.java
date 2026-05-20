package com.zenwealth.model;

public abstract class Asset {
    public String name;
    public AssetType type;

    public Asset() {}

    public Asset(String name, AssetType type) {
        this.name = name;
        this.type = type;
    }

    public abstract double getMarketValue();

    public double getTargetValue(double totalAssets) {
        return totalAssets * 0.25;
    }

    public double getDeviationRatio(double totalAssets) {
        double target = getTargetValue(totalAssets);
        if (target == 0) return 0;
        return (getMarketValue() - target) / target;
    }
}
