package com.stocktrading.example;

import com.stocktrading.backtest.BacktestEngine;
import com.stocktrading.data.DataSource;
import com.stocktrading.data.StockDataFetcher;
import com.stocktrading.features.FeatureEngineer;
import com.stocktrading.model.WekaModelTrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tushare 数据源示例
 * 演示如何使用 Tushare API 获取 A 股数据并进行交易
 */
public class TushareExample {

    private static final Logger logger = LoggerFactory.getLogger(TushareExample.class);

    public static void main(String[] args) {
        logger.info("=== Tushare Data Source Example ===");

        // Tushare 配置
        String tushareToken = "bdceda825b1fd502a038e8ecb743c68f6e900049587972c07d18aa3f";
        String symbol = "002716.SZ"; // 格式：股票代码.交易所（SZ=深圳，SH=上海）
        String startDate = "2022-01-01";
        String endDate = "2024-01-01";

        try {
            // 1. 创建 Tushare 数据源
            logger.info("Step 1: Creating Tushare data source");
            DataSource tushareSource = StockDataFetcher.createDataSource("tushare", tushareToken);
            StockDataFetcher fetcher = new StockDataFetcher(tushareSource);

            // 2. 抓取数据
            logger.info("Step 2: Fetching data for {}", symbol);
            String rawDataPath = "data/raw/" + symbol.replace(".", "_") + ".csv";
            boolean success = fetcher.fetchAndSave(symbol, startDate, endDate, rawDataPath);

            if (!success) {
                logger.error("Failed to fetch data");
                return;
            }

            // 3. 特征工程
            logger.info("Step 3: Creating features");
            FeatureEngineer engineer = new FeatureEngineer();
            String featuresPath = "data/features/" + symbol.replace(".", "_") + "_features.csv";
            success = engineer.processAndSave(rawDataPath, featuresPath);

            if (!success) {
                logger.error("Failed to create features");
                return;
            }

            // 4. 训练模型
            logger.info("Step 4: Training model");
            WekaModelTrainer trainer = new WekaModelTrainer();
            String modelPath = "models/" + symbol.replace(".", "_") + "_model.model";

            var result = trainer.trainAndEvaluate(
                    featuresPath,
                    modelPath,
                    "RandomForest",
                    0.8);

            if (result != null) {
                logger.info("\nModel Evaluation:");
                logger.info(result.toString());
            }

            // 5. 回测
            logger.info("Step 5: Running backtest");
            BacktestEngine backtest = new BacktestEngine();
            backtest.setInitialCapital(100000.0);

            BacktestEngine.BacktestResult backtestResult = backtest.runBacktest(featuresPath, modelPath);

            if (backtestResult != null) {
                logger.info(backtestResult.metrics.toString());

                // 保存结果
                String resultsPath = "results/" + symbol.replace(".", "_") + "_backtest.csv";
                backtest.saveResults(backtestResult, resultsPath);
                logger.info("Results saved to: {}", resultsPath);
            }

            logger.info("\n=== Tushare Example Completed ===");

        } catch (Exception e) {
            logger.error("Error in Tushare example", e);
        }
    }
}
