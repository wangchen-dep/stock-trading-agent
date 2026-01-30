package com.stocktrading;

import com.stocktrading.backtest.BacktestEngine;
import com.stocktrading.data.StockDataFetcher;
import com.stocktrading.features.FeatureEngineer;
import com.stocktrading.model.WekaModelTrainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class FetchAndSaveTest {

    /**
     * 主测试方法 - 完整的股票交易系统测试
     * <p>
     * 测试流程说明：
     * Step 1: 数据抓取 - 从 Yahoo Finance 或 Tushare 获取历史股票数据
     * Step 2: 特征工程 - 计算技术指标（MA、RSI、MACD等）作为模型输入
     * Step 3: 模型训练 - 使用 Weka 训练机器学习模型
     * Step 4: 回测验证 - 在历史数据上模拟交易，评估策略表现
     */
    @Test
    public void fetchAndSaveTest() {
        log.info("=== Stock Trading Agent Starting ===");

        try {

            // ==================== 参数提取 ====================
            // 从配置中提取关键参数，如果配置文件中没有则使用默认值

            // 股票代码：从配置的多个股票中取第一个（例如：AAPL、002716.SZ）
            String symbol = "002716.SZ";

            // 数据时间范围：定义要获取的历史数据起止日期
            String startDate = "2025-01-01";
            String endDate = "2026-01-29";

            // 文件路径配置：定义各个阶段数据的存储位置
            // rawDataPath: 原始股票数据（OHLCV - 开高低收成交量）
            String rawDataPath = String.format("%s/%s.csv", "data/raw", symbol);

            // ==================== Step 1: 数据抓取 ====================
            log.info("\n=== Step 1: Fetching Stock Data ===");
            log.info("目标：从数据源获取 {} 从 {} 到 {} 的历史数据", symbol, startDate, endDate);

            // 创建数据抓取器（默认使用 Yahoo Finance，也可配置为 Tushare）
            StockDataFetcher fetcher = new StockDataFetcher();

            // 执行数据抓取并保存到 CSV 文件
            // 返回值：true 表示成功，false 表示失败
            boolean dataFetched = fetcher.fetchAndSave(symbol, startDate, endDate, rawDataPath);

            // 检查数据抓取是否成功
            if (!dataFetched) {
                log.error("数据抓取失败！可能原因：网络问题、股票代码错误、日期范围无数据");
                return;
            }
            log.info("✓ 数据抓取成功，已保存到: {}", rawDataPath);

        } catch (Exception e) {
            log.error("程序执行过程中发生错误", e);
            log.error("错误原因可能是：配置文件错误、文件权限问题、网络连接失败等");
            System.exit(1);
        }
    }
}
