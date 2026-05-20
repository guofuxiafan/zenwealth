package com.zenwealth.service;

import com.zenwealth.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class StorageManagerTest {
    private StorageManager sm;
    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        sm = new StorageManager();
        tempFile = Files.createTempFile("test_assets", ".json");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testSaveAndLoad() throws IOException {
        Portfolio pf = new Portfolio();
        pf.addAsset(new CashAsset("活期", 50000));
        pf.addAsset(new EquityAsset("茅台", AssetType.STOCK, "600519.SH", 100, 1500));
        sm.save(pf, tempFile.toString());

        Portfolio loaded = sm.load(tempFile.toString());
        assertEquals(2, loaded.size());
        assertEquals(50000, loaded.getAssetsByType(AssetType.CASH).get(0).getMarketValue(), 0.01);
    }

    @Test
    void testLoadNonExistentFile() throws IOException {
        Portfolio loaded = sm.load("nonexistent.json");
        assertEquals(0, loaded.size());
    }

    @Test
    void testExists() throws IOException {
        assertFalse(sm.exists("nonexistent.json"));
        sm.save(new Portfolio(), tempFile.toString());
        assertTrue(sm.exists(tempFile.toString()));
    }

    @Test
    void testDepositAssetPersistence() throws IOException {
        Portfolio pf = new Portfolio();
        pf.addAsset(new DepositAsset("一年定存", 100000, 1.75));
        sm.save(pf, tempFile.toString());

        Portfolio loaded = sm.load(tempFile.toString());
        assertEquals(1, loaded.size());
        Asset a = loaded.getAssets().get(0);
        assertInstanceOf(DepositAsset.class, a);
        DepositAsset da = (DepositAsset) a;
        assertEquals(100000, da.amount, 0.01);
        assertEquals(1.75, da.interestRate, 0.01);
    }

    @Test
    void testGoldAndBondPersistence() throws IOException {
        Portfolio pf = new Portfolio();
        pf.addAsset(new EquityAsset("黄金ETF", AssetType.GOLD, "518880.SH", 100, 476.5));
        pf.addAsset(new EquityAsset("国债ETF", AssetType.BOND, "511010.SH", 1000, 102.3));
        sm.save(pf, tempFile.toString());

        Portfolio loaded = sm.load(tempFile.toString());
        assertEquals(2, loaded.size());
        assertEquals(AssetType.GOLD, loaded.getAssets().get(0).type);
        assertEquals(AssetType.BOND, loaded.getAssets().get(1).type);
    }
}
