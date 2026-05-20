package com.zenwealth.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    @Test
    void testTotalMarketValue() {
        Portfolio pf = new Portfolio();
        pf.addAsset(new CashAsset("活期", 50000));
        pf.addAsset(new DepositAsset("定存", 100000, 1.75));
        pf.addAsset(new EquityAsset("茅台", AssetType.STOCK, "600519.SH", 100, 1500));
        assertEquals(300000.0, pf.getTotalMarketValue(), 0.01);
    }

    @Test
    void testCategoryValue() {
        Portfolio pf = new Portfolio();
        pf.addAsset(new CashAsset("活期", 50000));
        pf.addAsset(new CashAsset("备用", 30000));
        pf.addAsset(new EquityAsset("茅台", AssetType.STOCK, "600519.SH", 100, 1500));
        assertEquals(80000.0, pf.getCategoryValue(AssetType.CASH), 0.01);
    }

    @Test
    void testRatios() {
        Portfolio pf = new Portfolio();
        pf.addAsset(new CashAsset("活期", 250000));
        pf.addAsset(new EquityAsset("茅台", AssetType.STOCK, "600519.SH", 100, 1500));
        pf.addAsset(new EquityAsset("五粮液", AssetType.STOCK, "000858.SZ", 100, 1000));
        assertEquals(0.5, pf.getCategoryRatios().get(AssetType.STOCK), 0.01);
    }

    @Test
    void testEmptyPortfolio() {
        Portfolio pf = new Portfolio();
        assertEquals(0.0, pf.getTotalMarketValue(), 0.01);
        assertEquals(0.0, pf.getCategoryRatios().get(AssetType.CASH), 0.01);
    }

    @Test
    void testEquityMarketValue() {
        EquityAsset ea = new EquityAsset("万科", AssetType.STOCK, "000002.SZ", 200, 15.5);
        assertEquals(3100.0, ea.getMarketValue(), 0.01);
    }

    @Test
    void testTargetValue() {
        CashAsset ca = new CashAsset("活期", 250000);
        assertEquals(100000.0, ca.getTargetValue(400000), 0.01);
    }

    @Test
    void testDeviationRatio() {
        CashAsset ca = new CashAsset("活期", 300000);
        assertEquals(0.5, ca.getDeviationRatio(800000), 0.01);
    }
}
