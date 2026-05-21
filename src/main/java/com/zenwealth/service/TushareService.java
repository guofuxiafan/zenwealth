package com.zenwealth.service;

import com.google.gson.*;
import com.zenwealth.model.AssetType;
import com.zenwealth.util.Constants;
import okhttp3.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TushareService {
    private final OkHttpClient client;
    private final String token;
    private static final MediaType MEDIA_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public TushareService() {
        this(Constants.TUSHARE_TOKEN);
    }

    public TushareService(String token) {
        this.token = token;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(15))
            .build();
    }

    public double fetchPrice(String code, AssetType type) throws IOException {
        String apiName = switch (type) {
            case STOCK -> "daily";
            case GOLD, BOND -> "fund_daily";
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
        String priceField = switch (type) {
            case STOCK -> "close";
            case GOLD, BOND -> "close";
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
        // 逐日向前回退，最多尝试5个交易日
        // 盘中时 daily/fund_daily 接口可能还没有生成当天的日K线数据
        String tradeDate = getLatestTradeDate();
        for (int i = 0; i < 5; i++) {
            JsonObject params = buildParams(code, tradeDate);
            JsonObject response = call(apiName, params);
            try {
                return extractFromResponse(response, priceField);
            } catch (IOException e) {
                // 当天无数据 → 尝试前一天 (跳过周末)
                LocalDate prev = LocalDate.parse(tradeDate, DATE_FMT).minusDays(1);
                int wd = prev.getDayOfWeek().getValue();
                if (wd == 6) prev = prev.minusDays(1);  // 周六→周五
                if (wd == 7) prev = prev.minusDays(2);  // 周日→周五
                tradeDate = prev.format(DATE_FMT);
            }
        }
        throw new IOException("No data for " + code + " after retrying 5 prior trading days");
    }

    private JsonObject buildParams(String code, String tradeDate) {
        JsonObject p = new JsonObject();
        p.addProperty("ts_code", code);
        p.addProperty("start_date", tradeDate);
        p.addProperty("end_date", tradeDate);
        return p;
    }

    private JsonObject call(String apiName, JsonObject params) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("api_name", apiName);
        body.addProperty("token", token);
        body.add("params", params);
        body.add("fields", new JsonArray());

        Request req = new Request.Builder()
            .url(Constants.TUSHARE_API_URL)
            .post(RequestBody.create(GSON.toJson(body), MEDIA_JSON))
            .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("HTTP " + resp.code() + ": " + resp.message());
            }
            String raw = resp.body() != null ? resp.body().string() : "{}";
            JsonObject json = GSON.fromJson(raw, JsonObject.class);
            int code = json.has("code") ? json.get("code").getAsInt() : -1;
            if (code != 0) {
                String msg = json.has("msg") ? json.get("msg").getAsString() : "unknown";
                throw new IOException("Tushare error [" + code + "]: " + msg);
            }
            return json;
        }
    }

    private double extractFromResponse(JsonObject response, String priceField) throws IOException {
        if (!response.has("data") || response.get("data").isJsonNull()) {
            throw new IOException("Tushare returned empty data (non-trading day or invalid code)");
        }
        JsonObject data = response.getAsJsonObject("data");
        JsonArray items = data.has("items") ? data.getAsJsonArray("items") : null;
        JsonArray fields = data.has("fields") ? data.getAsJsonArray("fields") : null;
        if (items == null || items.size() == 0 || fields == null) {
            throw new IOException("Empty data from Tushare for this date");
        }
        int idx = -1;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getAsString().equals(priceField)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            throw new IOException("Field '" + priceField + "' not found in Tushare response");
        }
        return items.get(0).getAsJsonArray().get(idx).getAsDouble();
    }

    private String getLatestTradeDate() {
        LocalDate d = LocalDate.now();
        int day = d.getDayOfWeek().getValue();
        if (day == 6) d = d.minusDays(1);
        if (day == 7) d = d.minusDays(2);
        return d.format(DATE_FMT);
    }
}
