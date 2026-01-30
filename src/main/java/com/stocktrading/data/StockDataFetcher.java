package com.stocktrading.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 股票数据抓取器
 * 负责从数据源获取数据并保存到文件
 */
public class StockDataFetcher {

    private static final Logger log = LoggerFactory.getLogger(StockDataFetcher.class);
    private final DataSource dataSource;

    public StockDataFetcher() {
        this(new TushareDataSource("bdceda825b1fd502a038e8ecb743c68f6e900049587972c07d18aa3f"));
    }

    public StockDataFetcher(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 创建指定类型的数据源
     * 
     * @param sourceType   数据源类型（"yahoo" 或 "tushare"）
     * @param tushareToken Tushare Token（仅当 sourceType 为 "tushare" 时需要）
     * @return 数据源实例
     */
    public static DataSource createDataSource(String sourceType, String tushareToken) {
        if ("tushare".equalsIgnoreCase(sourceType)) {
            if (tushareToken == null || tushareToken.isEmpty()) {
                throw new IllegalArgumentException("Tushare token is required for tushare data source");
            }
            return new TushareDataSource(tushareToken);
        } else {
            return new YahooFinanceDataSource();
        }
    }

    /**
     * 抓取股票数据并保存到 CSV 文件
     * 
     * @param symbol     股票代码
     * @param startDate  开始日期（格式：yyyy-MM-dd）
     * @param endDate    结束日期（格式：yyyy-MM-dd）
     * @param outputPath 输出文件路径
     * @return 是否成功
     */
    public boolean fetchAndSave(String symbol, String startDate, String endDate, String outputPath) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            log.info("Fetching stock data for {} from {} to {}", symbol, startDate, endDate);
            List<DataSource.StockData> data = dataSource.fetchData(symbol, start, end);

            if (data == null || data.isEmpty()) {
                log.error("No data retrieved for {}", symbol);
                return false;
            }

            log.info("Retrieved {} data points, saving to {}", data.size(), outputPath);
            saveToCSV(data, outputPath);
            log.info("Data successfully saved to {}", outputPath);

            return true;
        } catch (Exception e) {
            log.error("Error fetching data for {}", symbol, e);
            return false;
        }
    }

    /**
     * 保存数据到 CSV 文件
     */
    private void saveToCSV(List<DataSource.StockData> data, String filePath) throws IOException {
        // 创建目录（如果不存在）
        java.io.File file = new java.io.File(filePath);
        file.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // 写入表头
            writer.write(DataSource.StockData.getCsvHeader());
            writer.newLine();

            // 写入数据
            for (DataSource.StockData stockData : data) {
                writer.write(stockData.toCsvRow());
                writer.newLine();
            }
        }
    }

    /**
     * 批量抓取多个股票的数据
     */
    public void fetchMultipleSymbols(String[] symbols, String startDate, String endDate, String outputDir) {
        log.info("Fetching data for {} symbols", symbols.length);

        for (String symbol : symbols) {
            String outputPath = String.format("%s/%s.csv", outputDir, symbol.replace(".", "_"));
            boolean success = fetchAndSave(symbol, startDate, endDate, outputPath);

            if (success) {
                log.info("Successfully fetched data for {}", symbol);
            } else {
                log.error("Failed to fetch data for {}", symbol);
            }

            // 添加延迟以避免请求过于频繁
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted while sleeping", e);
            }
        }

        log.info("Completed fetching data for all symbols");
    }
}
