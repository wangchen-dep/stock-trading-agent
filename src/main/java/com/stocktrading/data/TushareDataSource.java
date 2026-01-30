package com.stocktrading.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Tushare 数据源实现
 * 使用 Tushare API 获取 A 股历史数据
 * 文档：https://tushare.pro/document/2?doc_id=27
 */
public class TushareDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(TushareDataSource.class);
    private static final String API_URL = "http://api.tushare.pro";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OkHttpClient client;
    private final String token;
    private final Gson gson;

    /**
     * 构造函数
     * 
     * @param token Tushare API Token
     */
    public TushareDataSource(String token) {
        this.token = token;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    @Override
    public List<StockData> fetchData(String symbol, LocalDate startDate, LocalDate endDate) throws Exception {
        logger.info("Fetching data for {} from {} to {}", symbol, startDate, endDate);

        List<StockData> allData = new ArrayList<>();
        // Tushare API 每次最多返回一定数量的数据，需要按日期范围分批获取
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDate batchEndDate = currentDate.plusMonths(3);
            if (batchEndDate.isAfter(endDate)) {
                batchEndDate = endDate;
            }

            List<StockData> batchData = fetchBatch(symbol, currentDate, batchEndDate);
            allData.addAll(batchData);

            currentDate = batchEndDate.plusDays(1);

            // 添加延迟避免频率限制
            Thread.sleep(300);
        }

//        List<StockData> allData = fetchBatch(symbol, startDate, endDate);

        logger.info("Successfully fetched {} data points", allData.size());
        return allData;
    }

    /**
     * 批量获取数据
     */
    private List<StockData> fetchBatch(String tsCode, LocalDate startDate, LocalDate endDate) throws IOException {
        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("api_name", "daily");
        requestBody.addProperty("token", token);

        JsonObject params = new JsonObject();
        params.addProperty("ts_code", tsCode);
        params.addProperty("start_date", startDate.format(DATE_FORMATTER));
        params.addProperty("end_date", endDate.format(DATE_FORMATTER));
        requestBody.add("params", params);

        // 指定返回字段
        requestBody.addProperty("fields", "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount");

        // 发送请求
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch data: " + response);
            }

            String responseBody = response.body().string();
            return parseResponse(responseBody);
        }
    }

    /**
     * 解析 API 响应
     */
    private List<StockData> parseResponse(String responseBody) {
        List<StockData> dataList = new ArrayList<>();

        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            // 检查返回码
            int code = json.get("code").getAsInt();
            if (code != 0) {
                String msg = json.has("msg") ? json.get("msg").getAsString() : "Unknown error";
                logger.error("API returned error code {}: {}", code, msg);
                return dataList;
            }

            // 获取数据
            JsonObject data = json.getAsJsonObject("data");
            if (data == null) {
                logger.warn("No data returned from API");
                return dataList;
            }

            JsonArray fields = data.getAsJsonArray("fields");
            JsonArray items = data.getAsJsonArray("items");

            if (items == null || items.size() == 0) {
                logger.warn("No items in response");
                return dataList;
            }

            // 解析每一行数据
            for (int i = 0; i < items.size(); i++) {
                JsonArray row = items.get(i).getAsJsonArray();
                StockData stockData = parseRow(fields, row);
                if (stockData != null) {
                    dataList.add(stockData);
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing response", e);
        }

        return dataList;
    }

    /**
     * 解析单行数据
     */
    private StockData parseRow(JsonArray fields, JsonArray row) {
        try {
            StockData data = new StockData();

            for (int i = 0; i < fields.size(); i++) {
                String fieldName = fields.get(i).getAsString();

                switch (fieldName) {
                    case "ts_code":
                        data.setTsCode(getStringValue(row, i));
                        break;
                    case "trade_date":
                        data.setTradeDate(getStringValue(row, i));
                        break;
                    case "open":
                        data.setOpen(getDoubleValue(row, i));
                        break;
                    case "high":
                        data.setHigh(getDoubleValue(row, i));
                        break;
                    case "low":
                        data.setLow(getDoubleValue(row, i));
                        break;
                    case "close":
                        data.setClose(getDoubleValue(row, i));
                        break;
                    case "pre_close":
                        data.setPreClose(getDoubleValue(row, i));
                        break;
                    case "change":
                        data.setChange(getDoubleValue(row, i));
                        break;
                    case "pct_chg":
                        data.setPctChg(getDoubleValue(row, i));
                        break;
                    case "vol":
                        data.setVol(getDoubleValue(row, i));
                        break;
                    case "amount":
                        data.setAmount(getDoubleValue(row, i));
                        break;
                }
            }

            return data;

        } catch (Exception e) {
            logger.error("Error parsing row", e);
            return null;
        }
    }

    /**
     * 获取字符串值
     */
    private String getStringValue(JsonArray row, int index) {
        if (row.get(index).isJsonNull()) {
            return "";
        }
        return row.get(index).getAsString();
    }

    /**
     * 获取 double 值
     */
    private double getDoubleValue(JsonArray row, int index) {
        if (row.get(index).isJsonNull()) {
            return 0.0;
        }
        return row.get(index).getAsDouble();
    }
}
