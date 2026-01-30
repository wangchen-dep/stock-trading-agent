# 数据源使用指南

本项目支持两种数据源：**Yahoo Finance** 和 **Tushare**。

## 📊 数据源对比

| 特性 | Yahoo Finance | Tushare |
|------|--------------|---------|
| **支持市场** | 全球市场（美股、港股等） | 中国 A 股 |
| **数据格式** | 英文股票代码（如 AAPL） | Tushare 格式（如 002716.SZ） |
| **Token 要求** | ❌ 不需要 | ✅ 需要注册获取 |
| **数据字段** | Date, Open, High, Low, Close, Adj Close, Volume | ts_code, trade_date, open, high, low, close, pre_close, change, pct_chg, vol, amount |
| **API 限制** | 较少限制 | 有频率限制（需控制请求速度） |
| **数据质量** | 复权价格 | 原始价格 + 涨跌幅 |

## 🔧 配置方法

编辑 `config/config.properties`：

### 使用 Yahoo Finance（默认）

```properties
# 数据源设置
data.source=yahoo
data.symbols=AAPL,GOOGL,MSFT
data.start.date=2020-01-01
data.end.date=2024-01-01
```

**股票代码格式**：
- 美股：`AAPL`, `GOOGL`, `MSFT`, `TSLA`
- 港股：`0700.HK`, `9988.HK`

### 使用 Tushare

```properties
# 数据源设置
data.source=tushare
data.tushare.token=你的Tushare_Token
data.symbols=002716.SZ,000001.SZ,600000.SH
data.start.date=2020-01-01
data.end.date=2024-01-01
```

**股票代码格式**：
- 深圳股票：`股票代码.SZ`（如 `002716.SZ`）
- 上海股票：`股票代码.SH`（如 `600000.SH`）

**获取 Tushare Token**：
1. 访问 https://tushare.pro/register
2. 注册账号
3. 在个人中心获取 Token

## 🚀 使用示例

### 示例 1: 使用 Yahoo Finance（美股）

```bash
# 修改 config.properties
data.source=yahoo
data.symbols=AAPL

# 运行
mvn exec:java -Dexec.mainClass="com.stocktrading.Main"
```

### 示例 2: 使用 Tushare（A股）

```bash
# 修改 config.properties
data.source=tushare
data.tushare.token=你的Token
data.symbols=002716.SZ

# 运行
mvn exec:java -Dexec.mainClass="com.stocktrading.Main"
```

### 示例 3: 运行 Tushare 专用示例

```bash
mvn exec:java -Dexec.mainClass="com.stocktrading.examples.TushareExample"
```

## 💻 代码中使用

### 创建不同的数据源

```java
// 创建 Yahoo Finance 数据源
DataSource yahooSource = StockDataFetcher.createDataSource("yahoo", null);

// 创建 Tushare 数据源
String tushareToken = "你的Token";
DataSource tushareSource = StockDataFetcher.createDataSource("tushare", tushareToken);

// 使用数据源
StockDataFetcher fetcher = new StockDataFetcher(tushareSource);
fetcher.fetchAndSave("002716.SZ", "2022-01-01", "2024-01-01", "data/raw/002716_SZ.csv");
```

## 📋 数据字段说明

### 统一的数据结构

两种数据源都会转换为统一的 `StockData` 结构：

```java
class StockData {
    String tsCode;       // 股票代码
    String tradeDate;    // 交易日期 (yyyyMMdd)
    double open;         // 开盘价
    double high;         // 最高价
    double low;          // 最低价
    double close;        // 收盘价
    double preClose;     // 昨收价
    double change;       // 涨跌额
    double pctChg;       // 涨跌幅(%)
    double vol;          // 成交量(手)
    double amount;       // 成交额(千元)
}
```

### 字段来源对比

| 字段 | Yahoo Finance | Tushare |
|------|--------------|---------|
| tsCode | 使用输入的 symbol | API 返回 |
| tradeDate | 转换自 Date | API 返回 (yyyyMMdd) |
| close | 使用 Adj Close（复权价） | API 返回（原始价） |
| preClose | 自动计算 | API 返回 |
| change | 自动计算 | API 返回 |
| pctChg | 自动计算 | API 返回 |
| amount | 不提供（设为0） | API 返回 |

## ⚠️ 注意事项

### Yahoo Finance
- ✅ 无需注册，免费使用
- ✅ 数据已复权，适合技术分析
- ⚠️ 国内访问可能不稳定
- ⚠️ 不提供成交额数据

### Tushare
- ✅ 数据完整，字段丰富
- ✅ 专注 A 股，数据准确
- ✅ 提供成交额、涨跌幅等详细数据
- ⚠️ 需要注册获取 Token
- ⚠️ 有 API 调用频率限制
- ⚠️ 代码中已添加延迟机制（每批次间隔 300ms）

## 🔍 故障排查

### Yahoo Finance 无法访问
- 检查网络连接
- 尝试使用代理
- 确认股票代码格式正确

### Tushare 调用失败
- 检查 Token 是否正确
- 确认股票代码格式（必须包含 .SZ 或 .SH）
- 检查是否触发频率限制
- 查看日志中的错误信息

### 数据为空
- 确认日期范围内有交易日
- 检查股票代码是否存在
- 查看日志中的详细错误

## 📚 相关文档

- **Yahoo Finance**: https://finance.yahoo.com/
- **Tushare 文档**: https://tushare.pro/document/2
- **Tushare API**: https://tushare.pro/document/2?doc_id=27
