package com.zenwealth.service;

import com.google.gson.*;
import com.zenwealth.model.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageManager {
    private static final Gson GSON;

    static {
        GSON = new GsonBuilder()
            .registerTypeAdapter(Asset.class, new AssetDeserializer())
            .setPrettyPrinting()
            .create();
    }

    public void save(Portfolio portfolio, String filePath) throws IOException {
        JsonPortfolio jp = new JsonPortfolio();
        jp.assets = portfolio.getAssets();
        String json = GSON.toJson(jp);
        Files.writeString(Path.of(filePath), json, StandardCharsets.UTF_8);
    }

    public Portfolio load(String filePath) throws IOException {
        Portfolio portfolio = new Portfolio();
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return portfolio;
        }
        String json = Files.readString(path, StandardCharsets.UTF_8);
        JsonPortfolio jp = GSON.fromJson(json, JsonPortfolio.class);
        if (jp.assets != null) {
            for (Asset asset : jp.assets) {
                portfolio.addAsset(asset);
            }
        }
        return portfolio;
    }

    public boolean exists(String filePath) {
        return Files.exists(Path.of(filePath));
    }

    private static class AssetDeserializer implements JsonDeserializer<Asset> {
        @Override
        public Asset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
            JsonObject obj = json.getAsJsonObject();
            AssetType assetType = AssetType.valueOf(obj.get("type").getAsString());
            return switch (assetType) {
                case CASH -> ctx.deserialize(json, CashAsset.class);
                case DEPOSIT -> ctx.deserialize(json, DepositAsset.class);
                case STOCK, GOLD, BOND -> ctx.deserialize(json, EquityAsset.class);
            };
        }
    }
}
