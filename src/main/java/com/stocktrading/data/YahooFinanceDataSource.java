package com.stocktrading.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Yahoo Finance 数据源实现
 * 使用 Yahoo Finance API v8 获取历史股票数据
 */
public class YahooFinanceDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceDataSource.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final OkHttpClient client;

    public YahooFinanceDataSource() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @Override
    public List<StockData> fetchData(String symbol, LocalDate startDate, LocalDate endDate) throws Exception {
        logger.info("Fetching data for {} from {} to {}", symbol, startDate, endDate);

        long period1 = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long period2 = endDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

        String url = String.format(
                "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=history",
                symbol, period1, period2);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch data: " + response);
            }

            String responseBody = response.body().string();
            return parseCSV(responseBody, symbol);
        }
    }

    /**
     * 解析 CSV 格式的响应数据
     * Yahoo Finance 返回格式：Date,Open,High,Low,Close,Adj Close,Volume
     */
    private List<StockData> parseCSV(String csv, String symbol) {
        List<StockData> dataList = new ArrayList<>();
        String[] lines = csv.split("\n");

        Double previousClose = null;

        // Skip header
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;

            try {
                String[] parts = line.split(",");
                if (parts.length < 7)
                    continue;

                LocalDate date = LocalDate.parse(parts[0]);
                double open = parseDouble(parts[1]);
                double high = parseDouble(parts[2]);
                double low = parseDouble(parts[3]);
                double close = parseDouble(parts[4]);
                double adjClose = parseDouble(parts[5]);
                double volume = parseDouble(parts[6]);

                // 跳过包含 null 值的数据
                if (open > 0 && high > 0 && low > 0 && close > 0 && adjClose > 0) {
                    StockData data = new StockData();
                    data.setTsCode(symbol);
                    data.setTradeDate(date.format(DATE_FORMATTER));
                    data.setOpen(open);
                    data.setHigh(high);
                    data.setLow(low);
                    data.setClose(adjClose); // 使用复权收盘价

                    // 计算昨收价、涨跌额和涨跌幅
                    if (previousClose != null) {
                        data.setPreClose(previousClose);
                        data.setChange(adjClose - previousClose);
                        data.setPctChg((adjClose - previousClose) / previousClose * 100);
                    } else {
                        data.setPreClose(adjClose);
                        data.setChange(0.0);
                        data.setPctChg(0.0);
                    }

                    data.setVol(volume / 100.0); // 转换为手（1手=100股）
                    data.setAmount(0.0); // Yahoo Finance 不提供成交额

                    dataList.add(data);
                    previousClose = adjClose;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse line: {}", line, e);
            }
        }

        logger.info("Successfully parsed {} data points", dataList.size());
        return dataList;
    }

    private double parseDouble(String value) {
        if (value == null || value.equalsIgnoreCase("null")) {
            return 0.0;
        }
        return Double.parseDouble(value);
    }
}
