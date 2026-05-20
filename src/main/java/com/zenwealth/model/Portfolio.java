package com.zenwealth.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Portfolio {
    private final List<Asset> assets = new ArrayList<>();

    public void addAsset(Asset asset) {
        assets.add(asset);
    }

    public void removeAsset(int index) {
        if (index >= 0 && index < assets.size()) {
            assets.remove(index);
        }
    }

    public void replaceAsset(int index, Asset asset) {
        if (index >= 0 && index < assets.size()) {
            assets.set(index, asset);
        }
    }

    public List<Asset> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public List<Asset> getAssetsByType(AssetType type) {
        return assets.stream()
            .filter(a -> a.type == type)
            .collect(Collectors.toList());
    }

    public double getTotalMarketValue() {
        return assets.stream()
            .mapToDouble(Asset::getMarketValue)
            .sum();
    }

    public double getCategoryValue(AssetType type) {
        return assets.stream()
            .filter(a -> a.type == type)
            .mapToDouble(Asset::getMarketValue)
            .sum();
    }

    public Map<AssetType, Double> getCategoryRatios() {
        double total = getTotalMarketValue();
        if (total == 0) {
            Map<AssetType, Double> zeros = new LinkedHashMap<>();
            zeros.put(AssetType.CASH, 0.0);
            zeros.put(AssetType.DEPOSIT, 0.0);
            zeros.put(AssetType.STOCK, 0.0);
            zeros.put(AssetType.GOLD, 0.0);
            zeros.put(AssetType.BOND, 0.0);
            return zeros;
        }
        Map<AssetType, Double> ratios = new LinkedHashMap<>();
        for (AssetType t : AssetType.values()) {
            ratios.put(t, getCategoryValue(t) / total);
        }
        return ratios;
    }

    public void clear() {
        assets.clear();
    }

    public int size() {
        return assets.size();
    }
}
