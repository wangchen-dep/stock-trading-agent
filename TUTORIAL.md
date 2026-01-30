# Stock Trading Agent - 使用指南

## 目录
- [快速开始](#快速开始)
- [详细教程](#详细教程)
- [配置说明](#配置说明)
- [示例代码](#示例代码)
- [常见问题](#常见问题)

## 快速开始

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 运行主程序

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.Main"
```

这将执行完整的流程：
1. 从 Yahoo Finance 抓取 AAPL 股票数据
2. 计算技术指标特征
3. 训练随机森林模型
4. 运行回测并生成买卖信号
5. 输出性能指标

### 3. 查看结果

- **原始数据**: `data/raw/AAPL.csv`
- **特征数据**: `data/features/AAPL_features.csv`
- **训练模型**: `models/AAPL_model.model`
- **回测结果**: `results/AAPL_backtest.csv`

## 详细教程

### 步骤 1: 数据抓取

```java
import com.stocktrading.data.StockDataFetcher;

StockDataFetcher fetcher = new StockDataFetcher();
fetcher.fetchAndSave("AAPL", "2020-01-01", "2024-01-01", "data/raw/AAPL.csv");
```

支持的功能：
- 从 Yahoo Finance 获取历史数据
- 自动处理数据清洗（去除缺失值）
- 批量下载多个股票
- 自动重试机制

### 步骤 2: 特征工程

```java
import com.stocktrading.features.FeatureEngineer;

FeatureEngineer engineer = new FeatureEngineer();
engineer.processAndSave("data/raw/AAPL.csv", "data/features/AAPL_features.csv");
```

自动计算的技术指标：

**趋势指标**
- MA5, MA10, MA20, MA50: 移动平均线
- EMA12, EMA26: 指数移动平均线

**动量指标**
- RSI(14): 相对强弱指标
- MACD: MACD线、信号线、柱状图
- ROC(10): 变化率
- Momentum(10): 动量

**波动率指标**
- Bollinger Bands: 上轨、中轨、下轨
- ATR(14): 平均真实范围

**成交量指标**
- Volume MA: 成交量移动平均
- Volume Ratio: 成交量比率

### 步骤 3: 模型训练

```java
import com.stocktrading.model.WekaModelTrainer;

WekaModelTrainer trainer = new WekaModelTrainer();

// 训练并评估
var result = trainer.trainAndEvaluate(
    "data/features/AAPL_features.csv",
    "models/AAPL_model.model",
    "RandomForest",  // 或 SVM, NaiveBayes, J48, MLP
    0.8  // 80% 训练集, 20% 测试集
);

System.out.println("Accuracy: " + result.accuracy + "%");
```

支持的模型：
- **RandomForest**: 随机森林（推荐）
- **SVM**: 支持向量机
- **NaiveBayes**: 朴素贝叶斯
- **J48**: 决策树
- **MLP**: 多层感知器（神经网络）

### 步骤 4: 回测

```java
import com.stocktrading.backtest.BacktestEngine;

BacktestEngine backtest = new BacktestEngine();

// 配置回测参数
backtest.setInitialCapital(100000.0);  // 初始资金
backtest.setCommission(0.001);         // 手续费 0.1%
backtest.setSlippage(0.0005);          // 滑点 0.05%
backtest.setStopLoss(0.05);            // 止损 5%
backtest.setTakeProfit(0.15);          // 止盈 15%
backtest.setBuyThreshold(0.6);         // 买入阈值
backtest.setSellThreshold(0.6);        // 卖出阈值

// 运行回测
BacktestEngine.BacktestResult result = backtest.runBacktest(
    "data/features/AAPL_features.csv",
    "models/AAPL_model.model"
);

// 保存结果
backtest.saveResults(result, "results/AAPL_backtest.csv");

// 打印性能指标
System.out.println(result.metrics.toString());
```

## 配置说明

编辑 `config/config.properties`:

```properties
# 数据配置
data.symbols=AAPL,GOOGL,MSFT
data.start.date=2020-01-01
data.end.date=2024-01-01

# 模型配置
model.type=RandomForest
model.trees=100
model.max.depth=10
model.train.test.split=0.8

# 回测配置
backtest.initial.capital=100000.0
backtest.commission.rate=0.001
backtest.slippage=0.0005
backtest.stop.loss=0.05
backtest.take.profit=0.15
backtest.buy.threshold=0.6
backtest.sell.threshold=0.6
```

## 示例代码

### 示例 1: 快速开始

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.examples.QuickStartExample"
```

完整流程演示，最简单的使用方式。

### 示例 2: 技术指标计算

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.examples.TechnicalIndicatorsExample"
```

展示如何使用 `TechnicalIndicators` 类计算各种技术指标。

### 示例 3: 模型比较

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.examples.ModelComparisonExample"
```

训练并比较多种模型的性能。

## 性能指标说明

回测结束后会输出以下指标：

- **Total Return**: 总收益率
- **Annualized Return**: 年化收益率
- **Sharpe Ratio**: 夏普比率（风险调整后收益）
- **Max Drawdown**: 最大回撤
- **Volatility**: 波动率
- **Win Rate**: 胜率
- **Profit/Loss Ratio**: 盈亏比

## 常见问题

### Q1: 如何添加新的数据源？

实现 `DataSource` 接口：

```java
public class CustomDataSource implements DataSource {
    @Override
    public List<StockData> fetchData(String symbol, LocalDate startDate, LocalDate endDate) {
        // 实现你的数据获取逻辑
    }
}
```

### Q2: 如何添加新的技术指标？

在 `TechnicalIndicators` 类中添加静态方法：

```java
public static List<Double> calculateCustomIndicator(List<Double> prices, int period) {
    // 实现你的指标计算逻辑
}
```

然后在 `FeatureEngineer` 中调用它。

### Q3: 如何优化模型参数？

修改 `WekaModelTrainer.buildClassifier()` 方法中的模型配置：

```java
RandomForest rf = new RandomForest();
rf.setNumIterations(200);  // 增加树的数量
rf.setMaxDepth(15);        // 增加深度
```

### Q4: 回测结果不理想怎么办？

尝试以下优化：

1. **增加数据量**: 使用更长时间跨度的数据
2. **调整特征**: 添加或移除某些技术指标
3. **更换模型**: 尝试不同的机器学习算法
4. **参数调优**: 调整买卖阈值、止损止盈等参数
5. **交叉验证**: 使用多个股票进行验证

### Q5: 如何在实盘中使用？

**警告**: 这是一个学习项目，不应直接用于实盘交易！

如果要用于实盘，需要：
1. 接入实时数据源
2. 实现交易 API 对接
3. 添加风险管理模块
4. 进行充分的回测和模拟交易
5. 考虑市场冲击和流动性

## 下一步

- 尝试不同的股票和时间段
- 实验不同的模型和参数组合
- 添加更多技术指标
- 实现更复杂的交易策略
- 进行组合优化和风险管理

## 支持

遇到问题？查看：
- [Weka 文档](https://www.cs.waikato.ac.nz/ml/weka/documentation.html)
- [技术分析指标说明](https://www.investopedia.com/terms/t/technicalindicator.asp)
