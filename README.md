# Stock Trading Agent - Java + Weka

一个完整的股票交易系统，使用 Java 和 Weka 机器学习库实现。

## 功能特性

- 📊 **数据抓取**: 从多种数据源获取历史股票数据
- 🔧 **特征工程**: 计算技术指标（MA, EMA, RSI, MACD, Bollinger Bands等）
- 🤖 **机器学习**: 使用 Weka 训练多种模型（随机森林、SVM、神经网络等）
- 📈 **回测系统**: 完整的回测引擎，生成买卖信号
- 📉 **性能评估**: 计算收益率、夏普比率、最大回撤等指标

## 项目结构

```
stock-trading-agent/
├── src/main/java/com/stocktrading/
│   ├── data/              # 数据抓取模块
│   │   ├── StockDataFetcher.java
│   │   ├── YahooFinanceDataSource.java
│   │   └── DataSource.java
│   ├── features/          # 特征工程模块
│   │   ├── FeatureEngineer.java
│   │   ├── TechnicalIndicators.java
│   │   └── FeatureNormalizer.java
│   ├── model/             # 模型训练模块
│   │   ├── WekaModelTrainer.java
│   │   ├── ModelEvaluator.java
│   │   └── ModelPersistence.java
│   ├── backtest/          # 回测模块
│   │   ├── BacktestEngine.java
│   │   ├── SignalGenerator.java
│   │   ├── Portfolio.java
│   │   └── PerformanceMetrics.java
│   ├── utils/             # 工具类
│   │   ├── DateUtils.java
│   │   └── CSVWriter.java
│   └── Main.java          # 主程序
├── data/
│   ├── raw/               # 原始数据
│   ├── processed/         # 处理后数据
│   └── features/          # 特征数据
├── models/                # 训练模型存储
├── results/               # 回测结果
└── config/
    └── config.properties  # 配置文件
```

## 快速开始

### 1. 安装依赖

```bash
mvn clean install
```

### 2. 运行完整流程

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.Main"
```

### 3. 单独运行各个模块

```java
// 数据抓取
StockDataFetcher fetcher = new StockDataFetcher();
fetcher.fetchAndSave("AAPL", "2020-01-01", "2024-01-01", "data/raw/AAPL.csv");

// 特征工程
FeatureEngineer engineer = new FeatureEngineer();
engineer.processAndSave("data/raw/AAPL.csv", "data/features/AAPL_features.csv");

// 模型训练
WekaModelTrainer trainer = new WekaModelTrainer();
trainer.trainAndSave("data/features/AAPL_features.csv", "models/random_forest.model");

// 回测
BacktestEngine backtest = new BacktestEngine();
backtest.runBacktest("data/features/AAPL_features.csv", "models/random_forest.model");
```

## 核心概念

### 特征工程

系统自动计算以下技术指标作为特征：

- **移动平均线**: MA5, MA10, MA20, MA50
- **指数移动平均线**: EMA12, EMA26
- **相对强弱指标**: RSI(14)
- **MACD**: MACD线, 信号线, 柱状图
- **布林带**: 上轨, 中轨, 下轨
- **成交量指标**: 成交量MA, 成交量变化率
- **价格动量**: ROC, Momentum
- **波动率**: ATR, 历史波动率

### 模型训练

支持多种 Weka 分类器：

- Random Forest (随机森林)
- SVM (支持向量机)
- Naive Bayes (朴素贝叶斯)
- J48 (决策树)
- Multilayer Perceptron (神经网络)

### 交易策略

- **买入信号**: 模型预测上涨概率 > 0.6
- **卖出信号**: 模型预测下跌概率 > 0.6 或止损
- **止损**: 下跌超过 5%
- **止盈**: 上涨超过 15%

### 回测指标

- 总收益率
- 年化收益率
- 夏普比率
- 最大回撤
- 胜率
- 盈亏比
- 交易次数

## 配置

编辑 `config/config.properties`:

```properties
# 数据源配置
data.source=yahoo
data.start.date=2020-01-01
data.end.date=2024-01-01

# 模型配置
model.type=RandomForest
model.trees=100
model.max.depth=10

# 回测配置
backtest.initial.capital=100000
backtest.commission=0.001
backtest.stop.loss=0.05
backtest.take.profit=0.15
```

## 注意事项

1. **数据质量**: 确保数据完整性，处理缺失值和异常值
2. **过拟合**: 使用交叉验证，避免在训练集上过度优化
3. **市场变化**: 定期重新训练模型以适应市场变化
4. **风险管理**: 设置合理的止损和仓位管理
5. **回测偏差**: 注意未来数据泄露和幸存者偏差

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License
