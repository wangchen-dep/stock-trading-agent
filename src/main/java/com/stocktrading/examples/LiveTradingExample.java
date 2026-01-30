package com.stocktrading.examples;

import com.stocktrading.backtest.SignalGenerator;
import com.stocktrading.data.DataSource;
import com.stocktrading.data.TushareDataSource;
import com.stocktrading.live.*;
import com.stocktrading.model.ModelPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 实盘交易示例
 * 演示如何使用实盘交易模块
 * 
 * ⚠️ 警告：这是模拟交易示例，不会连接真实券商
 * ⚠️ 在使用真实券商API前，请确保：
 * 1. 已经过充分的回测和模拟交易验证
 * 2. 理解所有风险并做好风险控制
 * 3. 从小资金开始，逐步增加
 * 4. 持续监控和优化策略
 */
public class LiveTradingExample {

    private static final Logger logger = LoggerFactory.getLogger(LiveTradingExample.class);

    public static void main(String[] args) {
        try {
            logger.info("========== 实盘交易示例 ==========");

            // 1. 加载训练好的模型
            String modelPath = "/Users/wangchen/openSourceCode/stock-trading-agent/models/002716.SZ_model.model";
            String headerPath = "/Users/wangchen/openSourceCode/stock-trading-agent/models/002716.SZ_model_header.arff";

            Classifier classifier = ModelPersistence.loadModel(modelPath);
            Instances header = ModelPersistence.loadTrainingHeader(headerPath);

            logger.info("模型加载成功");

            // 2. 初始化数据源
            String tushareToken = "bdceda825b1fd502a038e8ecb743c68f6e900049587972c07d18aa3f";
            DataSource dataSource = new TushareDataSource(tushareToken);

            // 3. 初始化信号生成器
            SignalGenerator signalGenerator = new SignalGenerator(classifier, header, 0.6, 0.6);

            // 5. 创建模拟券商API
            double initialCash = 100000.0; // 初始资金10万
            MockBrokerAPI brokerAPI = new MockBrokerAPI("TEST_ACCOUNT", initialCash, dataSource);

            // 设置交易成本参数
            brokerAPI.setSlippage(0.0001); // 滑点0.01%
            brokerAPI.setCommission(0.0003); // 手续费0.03%
            brokerAPI.setMinCommission(5.0); // 最低手续费5元

            // 6. 创建交易配置（使用保守配置）
            TradingConfig config = TradingConfig.createConservative();
            logger.info("交易配置：{}", config);

            // 7. 创建实盘交易引擎
            LiveTradingEngine engine = new LiveTradingEngine(
                    brokerAPI,
                    signalGenerator,
                    dataSource,
                    config);

            // 8. 设置监控股票列表
            engine.setWatchList(Arrays.asList(
                    "002716.SZ"
            ));

            // 9. 启动交易引擎
            engine.start();

            logger.info("交易引擎已启动，模拟运行中...");
            logger.info("按 Ctrl+C 停止");

            // 10. 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("收到停止信号，正在关闭交易引擎...");
                engine.stop();
                logger.info("交易引擎已关闭");
            }));

            // 11. 演示手动交易
            demonstrateManualTrading(engine, brokerAPI);

            // 12. 持续运行
            while (engine.isRunning()) {
                Thread.sleep(60000); // 每分钟检查一次

                // 定期输出账户状态
                printAccountStatus(engine);
            }

        } catch (Exception e) {
            logger.error("实盘交易示例执行失败", e);
        }
    }

    /**
     * 演示手动交易
     */
    private static void demonstrateManualTrading(LiveTradingEngine engine, BrokerAPI brokerAPI) {
        try {
            logger.info("\n========== 手动交易演示 ==========");

            String symbol = "600519.SH";
            double price = brokerAPI.getCurrentPrice(symbol);

            // 手动买入
            logger.info("手动买入：{} @ {} x 100", symbol, price);
            engine.manualBuy(symbol, price, 100)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            logger.info("✓ 手动买入成功：{}", result.getOrderId());
                        } else {
                            logger.warn("✗ 手动买入失败：{}", result.getMessage());
                        }
                    })
                    .get(5, TimeUnit.SECONDS);

            // 等待一段时间
            Thread.sleep(2000);

            // 检查持仓
            Position position = brokerAPI.getPosition(symbol);
            if (position != null && position.getQuantity() > 0) {
                logger.info("当前持仓：{}", position);

                // 手动卖出
                Thread.sleep(2000);
                logger.info("手动卖出：{} @ {} x {}", symbol, price, position.getQuantity());
                engine.manualSell(symbol, price, position.getQuantity())
                        .thenAccept(result -> {
                            if (result.isSuccess()) {
                                logger.info("✓ 手动卖出成功：{}", result.getOrderId());
                            } else {
                                logger.warn("✗ 手动卖出失败：{}", result.getMessage());
                            }
                        })
                        .get(5, TimeUnit.SECONDS);
            }

            logger.info("========== 手动交易演示完成 ==========\n");

        } catch (Exception e) {
            logger.error("手动交易演示失败", e);
        }
    }

    /**
     * 输出账户状态
     */
    private static void printAccountStatus(LiveTradingEngine engine) {
        try {
            Account account = engine.getAccount();

            logger.info("\n========== 账户状态 ==========");
            logger.info("可用资金：￥{:.2f}", account.getCash());
            logger.info("持仓市值：￥{:.2f}", account.getMarketValue());
            logger.info("总资产：￥{:.2f}", account.getTotalAssets());
            logger.info("持仓数量：{}", account.getPositions().size());

            if (!account.getPositions().isEmpty()) {
                logger.info("持仓明细：");
                for (Position position : account.getPositions().values()) {
                    logger.info("  {}", position);
                }
            }

            logger.info("==============================\n");

        } catch (Exception e) {
            logger.error("获取账户状态失败", e);
        }
    }
}
