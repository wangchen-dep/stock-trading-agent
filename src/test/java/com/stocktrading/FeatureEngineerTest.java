package com.stocktrading;

import com.stocktrading.backtest.BacktestEngine;
import com.stocktrading.features.FeatureEngineer;
import com.stocktrading.model.WekaModelTrainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class FeatureEngineerTest {
    @Test
    public void featureEngineerTest() {
        // 股票代码：从配置的多个股票中取第一个（例如：AAPL、002716.SZ）
        String symbol = "002716.SZ";

        // 文件路径配置：定义各个阶段数据的存储位置
        // rawDataPath: 原始股票数据（OHLCV - 开高低收成交量）
        String rawDataPath = String.format("%s/%s.csv", "data/raw", symbol);

        // featuresPath: 处理后的特征数据（包含技术指标）
        String featuresPath = String.format("%s/%s_features.csv", "data/features", symbol);

        // modelPath: 训练好的机器学习模型文件
        String modelPath = String.format("%s/%s_model.model", "models", symbol);

        // resultsPath: 回测结果输出文件
        String resultsPath = String.format("%s/%s_backtest.csv", "results", symbol);
        try {
            // 创建特征工程处理器
            FeatureEngineer engineer = new FeatureEngineer();

            // 读取原始数据，计算技术指标，生成特征数据集
            boolean featuresCreated = engineer.processAndSave(rawDataPath, featuresPath);

            // 检查特征工程是否成功
            if (!featuresCreated) {
                log.error("特征工程失败！可能原因：数据量不足（需要至少50个交易日）、数据格式错误");
                return;
            }
            log.info("✓ 特征工程完成，特征数据已保存到: {}", featuresPath);

            // ==================== Step 3: 模型训练 ====================
            log.info("\n=== Step 3: Training Model ===");
            log.info("目标：使用机器学习算法训练预测模型");

            // 创建模型训练器
            WekaModelTrainer trainer = new WekaModelTrainer();

            // 获取模型配置
            // modelType: 机器学习算法类型（RandomForest、SVM、NaiveBayes等）
            String modelType = "RandomForest";
            log.info("选择的模型类型: {}", modelType);

            // trainTestSplit: 训练集/测试集划分比例（0.8 表示 80% 用于训练，20% 用于测试）
            double trainTestSplit = 0.8d;
            log.info("训练集/测试集划分比例: {}/{}", trainTestSplit * 100, (1 - trainTestSplit) * 100);

            // 执行模型训练和评估
            // 过程：读取特征数据 -> 分割训练/测试集 -> 训练模型 -> 在测试集上评估性能
            var evaluationResult = trainer.trainAndEvaluate(
                    featuresPath, modelPath, modelType, trainTestSplit);

            // 检查模型训练是否成功
            if (evaluationResult == null) {
                log.error("模型训练失败！可能原因：数据格式错误、特征包含无效值");
                return;
            }

            // 打印模型评估结果
            log.info("\n✓ 模型训练完成！");
            log.info("模型性能指标：");
            log.info(evaluationResult.toString());
            log.info("模型已保存到: {}", modelPath);

            // ==================== Step 4: 回测 ====================
            log.info("\n=== Step 4: Running Backtest ===");
            log.info("目标：使用训练好的模型在历史数据上模拟交易");
            log.info("回测流程：读取历史数据 -> 模型预测涨跌 -> 生成买卖信号 -> 模拟交易 -> 计算收益");

            // 创建回测引擎
            BacktestEngine backtest = new BacktestEngine();

            // ==================== 配置回测参数 ====================

            // 初始资金：交易账户的起始资金（单位：元）
            double initialCapital = 100000d;
            backtest.setInitialCapital(initialCapital);
            log.info("初始资金: ¥{}", initialCapital);

            // 手续费率：每笔交易的手续费比例（0.001 = 0.1%）
            double commission = 0.001d;
            backtest.setCommission(commission);
            log.info("手续费率: {}%", commission * 100);

            // 滑点：实际成交价与预期价格的偏差（0.0005 = 0.05%）
            double slippage = 0.0005d;
            backtest.setSlippage(slippage);
            log.info("滑点: {}%", slippage * 100);

            // 止损：当亏损达到此比例时强制卖出（0.05 = 5%）
            double stopLoss = 0.05d;
            backtest.setStopLoss(stopLoss);
            log.info("止损线: -{}%", stopLoss * 100);

            // 止盈：当盈利达到此比例时获利了结（0.15 = 15%）
            double takeProfit = 0.15d;
            backtest.setTakeProfit(takeProfit);
            log.info("止盈线: +{}%", takeProfit * 100);

            // 买入阈值：模型预测上涨概率超过此值才买入（0.6 = 60%）
            double buyThreshold = 0.6d;
            backtest.setBuyThreshold(buyThreshold);
            log.info("买入阈值（预测上涨概率）: {}%", buyThreshold * 100);

            // 卖出阈值：模型预测下跌概率超过此值才卖出（0.6 = 60%）
            double sellThreshold = 0.6d;
            backtest.setSellThreshold(sellThreshold);
            log.info("卖出阈值（预测下跌概率）: {}%", sellThreshold * 100);

            // 执行回测：加载模型 -> 读取数据 -> 逐日预测 -> 生成信号 -> 模拟交易
            BacktestEngine.BacktestResult result = backtest.runBacktest(featuresPath, modelPath);

            // 检查回测是否成功
            if (result == null) {
                log.error("回测失败！可能原因：模型加载失败、数据格式不匹配");
                return;
            }

            // 保存详细的回测结果到 CSV 文件
            // 包含每日的价格、信号、持仓、资金等详细记录
            backtest.saveResults(result, resultsPath);
            log.info("✓ 回测完成，详细结果已保存到: {}", resultsPath);

            // ==================== 打印性能指标 ====================
            log.info("\n========== 回测性能总结 ==========");
            log.info(result.metrics.toString());
            log.info("\n性能指标说明：");
            log.info("- Total Return（总收益率）: 整个回测期间的累计收益率");
            log.info("- Annualized Return（年化收益率）: 折算成年收益率");
            log.info("- Sharpe Ratio（夏普比率）: 风险调整后的收益，越高越好");
            log.info("- Max Drawdown（最大回撤）: 从峰值到谷底的最大跌幅");
            log.info("- Win Rate（胜率）: 盈利交易占总交易的比例");
            log.info("- Profit/Loss Ratio（盈亏比）: 平均盈利与平均亏损的比值");

            // ==================== 打印交易记录 ====================
            log.info("\n========== 交易历史记录 ==========");
            log.info("共进行了 {} 笔交易", result.trades.size());
            for (var trade : result.trades) {
                log.info(trade.toString());
            }

            // ==================== 完成 ====================
            log.info("\n=== Stock Trading Agent Completed Successfully ===");
            log.info("完整报告已生成！");
            log.info("回测结果文件: {}", resultsPath);
            log.info("\n下一步建议：");
            log.info("1. 查看 {} 分析每日交易信号和资金变化", resultsPath);
            log.info("2. 尝试调整模型参数（model.type, model.trees 等）提升准确率");
            log.info("3. 优化回测参数（止损、止盈、买卖阈值）改善收益");
            log.info("4. 测试不同的股票和时间段验证策略的普适性");

        } catch (Exception e) {
            log.error("程序执行过程中发生错误", e);
            log.error("错误原因可能是：配置文件错误、文件权限问题、网络连接失败等");
            System.exit(1);
        }
    }
}
