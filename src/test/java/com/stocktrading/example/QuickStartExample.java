package com.stocktrading.example;

import com.stocktrading.backtest.BacktestEngine;
import com.stocktrading.data.StockDataFetcher;
import com.stocktrading.features.FeatureEngineer;
import com.stocktrading.model.WekaModelTrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 快速开始示例
 * 演示如何快速运行整个交易系统
 */
public class QuickStartExample {

    private static final Logger log = LoggerFactory.getLogger(QuickStartExample.class);

    public static void main(String[] args) {
        log.info("=== Quick Start Example ===");

        // 配置参数
        String symbol = "AAPL";
        String startDate = "2022-01-01";
        String endDate = "2024-01-01";

        // 1. 抓取数据
        log.info("Step 1: Fetching data for {}", symbol);
        StockDataFetcher fetcher = new StockDataFetcher();
        fetcher.fetchAndSave(symbol, startDate, endDate, "data/raw/" + symbol + ".csv");

        // 2. 特征工程
        log.info("Step 2: Creating features");
        FeatureEngineer engineer = new FeatureEngineer();
        engineer.processAndSave(
                "data/raw/" + symbol + ".csv",
                "data/features/" + symbol + "_features.csv");

        // 3. 训练模型
        log.info("Step 3: Training model");
        WekaModelTrainer trainer = new WekaModelTrainer();
        trainer.trainAndSave(
                "data/features/" + symbol + "_features.csv",
                "models/" + symbol + "_model.model",
                "RandomForest");

        // 4. 回测
        log.info("Step 4: Running backtest");
        BacktestEngine backtest = new BacktestEngine();
        BacktestEngine.BacktestResult result = backtest.runBacktest(
                "data/features/" + symbol + "_features.csv",
                "models/" + symbol + "_model.model");

        // 5. 保存结果
        backtest.saveResults(result, "results/" + symbol + "_backtest.csv");

        log.info("\n" + result.metrics.toString());
        log.info("\n=== Quick Start Example Completed ===");
    }
}
