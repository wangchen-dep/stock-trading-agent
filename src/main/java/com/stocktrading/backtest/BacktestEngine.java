package com.stocktrading.backtest;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.stocktrading.model.ModelPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 回测引擎
 * 使用历史数据和训练好的模型进行回测，生成买卖信号并计算性能指标
 */
public class BacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(BacktestEngine.class);

    private double initialCapital = 100000.0;
    private double commission = 0.001;
    private double slippage = 0.0005;
    private double stopLoss = 0.05;
    private double takeProfit = 0.15;
    private double buyThreshold = 0.6;
    private double sellThreshold = 0.6;

    /**
     * 运行回测
     * 
     * @param dataPath  特征数据文件路径
     * @param modelPath 模型文件路径
     * @return 回测结果
     */
    public BacktestResult runBacktest(String dataPath, String modelPath) {
        try {
            log.info("Loading model from {}", modelPath);
            Classifier classifier = ModelPersistence.loadModel(modelPath);
            Instances header = ModelPersistence.loadTrainingHeader(modelPath);

            log.info("Loading backtest data from {}", dataPath);
            List<DataPoint> data = loadBacktestData(dataPath);

            log.info("Running backtest with {} data points", data.size());
            BacktestResult result = runBacktest(classifier, header, data);

            log.info("Backtest completed successfully");
            log.info(result.metrics.toString());

            return result;

        } catch (Exception e) {
            log.error("Error running backtest", e);
            return null;
        }
    }

    /**
     * 执行回测逻辑
     */
    private BacktestResult runBacktest(Classifier classifier, Instances header, List<DataPoint> data) {
        Portfolio portfolio = new Portfolio(initialCapital, commission, slippage);
        SignalGenerator signalGen = new SignalGenerator(classifier, header, buyThreshold, sellThreshold);

        List<SignalRecord> signals = new ArrayList<>();
        List<Double> dailyReturns = new ArrayList<>();
        double previousValue = initialCapital;

        for (DataPoint point : data) {
            // 生成信号
            SignalGenerator.Signal signal = signalGen.generateSignal(point.features);

            boolean traded = false;
            String action = "HOLD";

            // 检查止损和止盈
            if (portfolio.hasPosition()) {
                double currentReturn = (point.close - portfolio.getAvgBuyPrice()) / portfolio.getAvgBuyPrice();

                if (currentReturn <= -stopLoss) {
                    // 止损
                    portfolio.sell(point.close, point.date);
                    action = "SELL (Stop Loss)";
                    traded = true;
                    log.debug("{}: Stop loss triggered at {}", point.date, point.close);
                } else if (currentReturn >= takeProfit) {
                    // 止盈
                    portfolio.sell(point.close, point.date);
                    action = "SELL (Take Profit)";
                    traded = true;
                    log.debug("{}: Take profit triggered at {}", point.date, point.close);
                }
            }

            // 根据信号执行交易
            if (!traded) {
                if (signal.action == SignalGenerator.SignalAction.BUY && !portfolio.hasPosition()) {
                    if (portfolio.buy(point.close, point.date)) {
                        action = "BUY";
                        log.debug("{}: Buy signal at {} (confidence: {:.2f})",
                                point.date, point.close, signal.confidence);
                    }
                } else if (signal.action == SignalGenerator.SignalAction.SELL && portfolio.hasPosition()) {
                    if (portfolio.sell(point.close, point.date)) {
                        action = "SELL";
                        log.debug("{}: Sell signal at {} (confidence: {:.2f})",
                                point.date, point.close, signal.confidence);
                    }
                }
            }

            // 记录信号和状态
            double totalValue = portfolio.getTotalValue(point.close);
            double dailyReturn = (totalValue - previousValue) / previousValue;
            dailyReturns.add(dailyReturn);
            previousValue = totalValue;

            signals.add(new SignalRecord(
                    point.date,
                    point.close,
                    signal.action.toString(),
                    action,
                    signal.confidence,
                    portfolio.getShares(),
                    portfolio.getCash(),
                    totalValue,
                    portfolio.getReturn(point.close)));
        }

        // 计算性能指标
        double finalPrice = data.get(data.size() - 1).close;
        PerformanceMetrics.MetricsResult metrics = PerformanceMetrics.calculate(portfolio, dailyReturns, finalPrice);

        return new BacktestResult(signals, portfolio.getTrades(), metrics);
    }

    /**
     * 从CSV文件加载回测数据
     */
    private List<DataPoint> loadBacktestData(String filePath) throws Exception {
        List<DataPoint> data = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] header = reader.readNext();
            String[] line;

            while ((line = reader.readNext()) != null) {
                // 新格式有27列：Date, Close, 24个特征, Future_Return, Label
                if (line.length < 27)
                    continue;

                String date = line[0];
                double close = Double.parseDouble(line[1]);

                // 提取特征（跳过 Date[0], Close[1], Future_Return[25], Label[26]）
                // 特征列：[2-25] 共24个特征
                double[] features = new double[24];
                for (int i = 0; i < 24; i++) {
                    features[i] = Double.parseDouble(line[i + 2]);
                }

                data.add(new DataPoint(date, close, features));
            }
        }

        // 按日期升序排序，确保按时间从旧到新进行回测
        data.sort((a, b) -> a.date.compareTo(b.date));

        if (!data.isEmpty()) {
            log.info("Data sorted chronologically: {} to {}",
                    data.get(0).date, data.get(data.size() - 1).date);
        }

        return data;
    }

    /**
     * 保存回测结果到CSV
     */
    public void saveResults(BacktestResult result, String outputPath) {
        try {
            // 创建目录
            java.io.File file = new java.io.File(outputPath);
            file.getParentFile().mkdirs();

            try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
                // 写入表头
                writer.writeNext(new String[] {
                        "Date", "Price", "Predicted_Signal", "Actual_Action", "Confidence",
                        "Shares", "Cash", "Total_Value", "Return"
                });

                // 写入数据
                for (SignalRecord record : result.signals) {
                    writer.writeNext(record.toArray());
                }
            }

            log.info("Backtest results saved to {}", outputPath);

        } catch (Exception e) {
            log.error("Error saving results", e);
        }
    }

    // Setters for configuration
    public void setInitialCapital(double initialCapital) {
        this.initialCapital = initialCapital;
    }

    public void setCommission(double commission) {
        this.commission = commission;
    }

    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public void setTakeProfit(double takeProfit) {
        this.takeProfit = takeProfit;
    }

    public void setBuyThreshold(double buyThreshold) {
        this.buyThreshold = buyThreshold;
    }

    public void setSellThreshold(double sellThreshold) {
        this.sellThreshold = sellThreshold;
    }

    // 内部类：数据点
    private static class DataPoint {
        String date;
        double close;
        double[] features;

        DataPoint(String date, double close, double[] features) {
            this.date = date;
            this.close = close;
            this.features = features;
        }
    }

    // 内部类：信号记录
    private static class SignalRecord {
        /**
         * 交易日期
         * 格式: YYYY-MM-DD (例如: "2025-02-14")
         * 用途: 标识这条记录对应的交易日
         */
        String date;

        /**
         * 当日股票收盘价
         * 单位: 元/股
         * 用途: 记录当天的股价，用于计算持仓市值和收益
         */
        double price;

        /**
         * AI模型预测的交易信号
         * 可能值: "BUY"(建议买入) / "SELL"(建议卖出) / "HOLD"(建议持有)
         * 说明: 这是模型的建议，不一定被执行
         * 示例: 如果模型认为股价会上涨，则输出 "BUY"
         */
        String predictedSignal;

        /**
         * 实际执行的交易动作
         * 可能值:
         * - "BUY": 实际买入了股票
         * - "SELL": 按模型信号卖出
         * - "SELL (Stop Loss)": 触发止损强制卖出（亏损达到止损线）
         * - "SELL (Take Profit)": 触发止盈自动卖出（盈利达到止盈线）
         * - "HOLD": 没有任何操作
         * 说明: 由于持仓状态、资金限制等原因，实际动作可能与预测信号不同
         * 示例: 预测是"SELL"但当前无持仓，则实际动作为"HOLD"
         */
        String actualAction;

        /**
         * 模型预测的置信度/把握程度
         * 范围: 0.0 ~ 1.0 (对应 0% ~ 100%)
         * 用途: 表示模型对预测结果的信心大小
         * 阈值: 只有当置信度超过设定阈值(默认60%)时才会执行交易
         * 示例: 0.73 表示模型有73%的把握，0.55表示只有55%把握(不太确定)
         */
        double confidence;

        /**
         * 当前持有的股票数量
         * 单位: 股
         * 状态: 0=空仓(没有股票), >0=持仓(有股票)
         * 用途: 追踪持仓状态，判断是否可以买入或卖出
         * 示例: 26555 表示持有26555股
         */
        int shares;

        /**
         * 账户中剩余的现金余额
         * 单位: 元
         * 变化: 买入股票→现金减少, 卖出股票→现金增加, 手续费→现金减少
         * 用途: 记录可用资金，判断是否有足够现金买入
         * 示例: 3.38 表示账户里还有3.38元现金
         */
        double cash;

        /**
         * 账户总资产价值
         * 计算公式: totalValue = cash + (shares × price)
         * = 现金余额 + 持仓市值
         * 用途: 反映当前账户的总价值，用于计算收益
         * 示例: 如果现金3.38元，持有26555股×3.76元/股，则总资产=99850.18元
         */
        double totalValue;

        /**
         * 累计收益率（从初始到当前）
         * 计算公式: returnPct = (totalValue - initialCapital) / initialCapital
         * 范围: 正值=盈利, 负值=亏损, 0=不赚不亏
         * 单位: 小数形式（转为百分比需×100）
         * 用途: 衡量策略的盈利能力
         * 示例:
         * -0.001498 = -0.1498% (亏损0.1498%)
         * 0.1520 = +15.20% (盈利15.20%)
         */
        double returnPct;

        SignalRecord(String date, double price, String predictedSignal, String actualAction,
                double confidence, int shares, double cash, double totalValue, double returnPct) {
            this.date = date;
            this.price = price;
            this.predictedSignal = predictedSignal;
            this.actualAction = actualAction;
            this.confidence = confidence;
            this.shares = shares;
            this.cash = cash;
            this.totalValue = totalValue;
            this.returnPct = returnPct;
        }

        String[] toArray() {
            return new String[] {
                    date,
                    String.format("%.2f", price),
                    predictedSignal,
                    actualAction,
                    String.format("%.4f", confidence),
                    String.valueOf(shares),
                    String.format("%.2f", cash),
                    String.format("%.2f", totalValue),
                    String.format("%.4f", returnPct * 100)
            };
        }
    }

    /**
     * 回测结果类
     */
    public static class BacktestResult {
        public final List<SignalRecord> signals;
        public final List<Portfolio.Trade> trades;
        public final PerformanceMetrics.MetricsResult metrics;

        public BacktestResult(List<SignalRecord> signals, List<Portfolio.Trade> trades,
                PerformanceMetrics.MetricsResult metrics) {
            this.signals = signals;
            this.trades = trades;
            this.metrics = metrics;
        }
    }
}
