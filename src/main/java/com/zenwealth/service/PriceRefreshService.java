package com.zenwealth.service;

import com.zenwealth.model.Asset;
import com.zenwealth.model.EquityAsset;
import com.zenwealth.model.Portfolio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PriceRefreshService {
    private final TushareService tushareService;

    public PriceRefreshService(TushareService tushareService) {
        this.tushareService = tushareService;
    }

    public RefreshResult refreshAll(Portfolio portfolio) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Asset asset : portfolio.getAssets()) {
            if (asset instanceof EquityAsset ea) {
                try {
                    double newPrice = tushareService.fetchPrice(ea.code, ea.type);
                    ea.unitPrice = newPrice;
                    success++;
                } catch (IOException e) {
                    failed++;
                    errors.add(ea.code + ": " + e.getMessage());
                }
            }
        }
        return new RefreshResult(success, failed, errors);
    }

    public record RefreshResult(int success, int failed, List<String> errors) {
        public boolean hasFailed() {
            return failed > 0;
        }
    }
}
