package com.stocktrading.features;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.*;

/**
 * 特征工程处理器
 * 读取原始股票数据，计算技术指标，生成特征数据集
 */
public class FeatureEngineer {

    private static final Logger log = LoggerFactory.getLogger(FeatureEngineer.class);

    /**
     * 处理原始数据并生成特征
     * 
     * @param inputPath  输入CSV文件路径
     * @param outputPath 输出CSV文件路径
     * @return 是否成功
     */
    public boolean processAndSave(String inputPath, String outputPath) {
        try {
            log.info("Loading data from {}", inputPath);
            List<StockRecord> records = loadData(inputPath);

            if (records.size() < 50) {
                log.error("Not enough data points (need at least 50, got {})", records.size());
                return false;
            }

            log.info("Calculating features for {} records", records.size());
            List<FeatureRecord> features = calculateFeatures(records);

            log.info("Saving {} feature records to {}", features.size(), outputPath);
            saveFeatures(features, outputPath);

            log.info("Feature engineering completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error processing features", e);
            return false;
        }
    }

    /**
     * 从CSV文件加载股票数据
     * CSV格式：ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount
     */
    private List<StockRecord> loadData(String filePath) throws Exception {
        List<StockRecord> records = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] header = reader.readNext(); // Skip header
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 11)
                    continue;

                try {
                    // 解析 trade_date: YYYYMMDD 格式
                    String tradeDateStr = line[1];
                    LocalDate date;
                    if (tradeDateStr.length() == 8) {
                        int year = Integer.parseInt(tradeDateStr.substring(0, 4));
                        int month = Integer.parseInt(tradeDateStr.substring(4, 6));
                        int day = Integer.parseInt(tradeDateStr.substring(6, 8));
                        date = LocalDate.of(year, month, day);
                    } else {
                        continue; // 跳过无效日期
                    }

                    String tsCode = line[0];
                    double open = Double.parseDouble(line[2]);
                    double high = Double.parseDouble(line[3]);
                    double low = Double.parseDouble(line[4]);
                    double close = Double.parseDouble(line[5]);
                    double preClose = Double.parseDouble(line[6]);
                    double change = Double.parseDouble(line[7]);
                    double pctChg = Double.parseDouble(line[8]);
                    double vol = Double.parseDouble(line[9]); // 成交量（手）
                    double amount = Double.parseDouble(line[10]); // 成交额（千元）

                    records.add(new StockRecord(tsCode, date, open, high, low, close,
                            preClose, change, pctChg, vol, amount));
                } catch (Exception e) {
                    log.warn("Error parsing line: {}", String.join(",", line));
                    continue;
                }
            }
        }

        return records;
    }

    /**
     * 计算所有技术指标特征
     */
    private List<FeatureRecord> calculateFeatures(List<StockRecord> records) {
        // 提取价格和成交量数据
        List<Double> closes = new ArrayList<>();
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();

        for (StockRecord record : records) {
            closes.add(record.close);
            highs.add(record.high);
            lows.add(record.low);
            volumes.add(record.vol); // 使用 vol（手）
            amounts.add(record.amount); // 使用 amount（千元）
        }

        // 计算技术指标
        List<Double> ma5 = TechnicalIndicators.calculateSMA(closes, 5);
        List<Double> ma10 = TechnicalIndicators.calculateSMA(closes, 10);
        List<Double> ma20 = TechnicalIndicators.calculateSMA(closes, 20);
        List<Double> ma50 = TechnicalIndicators.calculateSMA(closes, 50);

        List<Double> ema12 = TechnicalIndicators.calculateEMA(closes, 12);
        List<Double> ema26 = TechnicalIndicators.calculateEMA(closes, 26);

        List<Double> rsi = TechnicalIndicators.calculateRSI(closes, 14);

        TechnicalIndicators.MACDResult macd = TechnicalIndicators.calculateMACD(closes, 12, 26, 9);

        TechnicalIndicators.BollingerBandsResult bb = TechnicalIndicators.calculateBollingerBands(closes, 20, 2.0);

        List<Double> roc = TechnicalIndicators.calculateROC(closes, 10);
        List<Double> momentum = TechnicalIndicators.calculateMomentum(closes, 10);
        List<Double> atr = TechnicalIndicators.calculateATR(highs, lows, closes, 14);

        List<Double> volumeMA = TechnicalIndicators.calculateSMA(volumes, 20);

        // 创建特征记录
        List<FeatureRecord> features = new ArrayList<>();

        for (int i = 50; i < records.size() - 1; i++) { // 从50开始以确保所有指标都有值
            StockRecord current = records.get(i);

            // 计算未来收益（标签）
            double futureReturn = (records.get(i + 1).close - current.close) / current.close;
            String label = futureReturn > 0.02 ? "UP" : (futureReturn < -0.02 ? "DOWN" : "HOLD");

            FeatureRecord feature = new FeatureRecord();
            feature.date = current.date;
            feature.close = current.close;

            // 移动平均线
            feature.ma5 = ma5.get(i);
            feature.ma10 = ma10.get(i);
            feature.ma20 = ma20.get(i);
            feature.ma50 = ma50.get(i);

            // EMA
            feature.ema12 = ema12.get(i);
            feature.ema26 = ema26.get(i);

            // RSI
            feature.rsi = rsi.get(i);

            // MACD
            feature.macd = macd.macdLine.get(i);
            feature.macdSignal = macd.signalLine.get(i);
            feature.macdHist = macd.histogram.get(i);

            // 布林带
            feature.bbUpper = bb.upperBand.get(i);
            feature.bbMiddle = bb.middleBand.get(i);
            feature.bbLower = bb.lowerBand.get(i);

            // 其他指标
            feature.roc = roc.get(i);
            feature.momentum = momentum.get(i);
            feature.atr = atr.get(i);

            // 成交量和成交额
            feature.volume = current.vol;
            feature.amount = current.amount;
            feature.volumeMA = volumeMA.get(i);
            feature.volumeRatio = volumes.get(i) / volumeMA.get(i);

            // 涨跌相关
            feature.pctChg = current.pctChg;
            feature.change = current.change;

            // 价格位置（相对于布林带）
            feature.pricePosition = (current.close - bb.lowerBand.get(i)) /
                    (bb.upperBand.get(i) - bb.lowerBand.get(i));

            // 标签
            feature.label = label;
            feature.futureReturn = futureReturn;

            features.add(feature);
        }

        return features;
    }

    /**
     * 保存特征到CSV文件
     */
    private void saveFeatures(List<FeatureRecord> features, String filePath) throws Exception {
        // 创建目录
        java.io.File file = new java.io.File(filePath);
        file.getParentFile().mkdirs();

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // 写入表头
            writer.writeNext(FeatureRecord.getHeader());

            // 写入数据
            for (FeatureRecord feature : features) {
                writer.writeNext(feature.toArray());
            }
        }
    }

    // 内部类：股票记录
    private static class StockRecord {
        String tsCode; // 股票代码
        LocalDate date; // 交易日期
        double open; // 开盘价
        double high; // 最高价
        double low; // 最低价
        double close; // 收盘价
        double preClose; // 昨收价
        double change; // 涨跌额
        double pctChg; // 涨跌幅（%）
        double vol; // 成交量（手）
        double amount; // 成交额（千元）

        StockRecord(String tsCode, LocalDate date, double open, double high, double low,
                double close, double preClose, double change, double pctChg,
                double vol, double amount) {
            this.tsCode = tsCode;
            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.preClose = preClose;
            this.change = change;
            this.pctChg = pctChg;
            this.vol = vol;
            this.amount = amount;
        }
    }

    // 内部类：特征记录
    private static class FeatureRecord {
        LocalDate date;
        double close;
        double ma5, ma10, ma20, ma50;
        double ema12, ema26;
        double rsi;
        double macd, macdSignal, macdHist;
        double bbUpper, bbMiddle, bbLower;
        double roc, momentum, atr;
        double volume; // 成交量（手）
        double amount; // 成交额（千元）
        double volumeMA, volumeRatio;
        double pctChg; // 涨跌幅（%）
        double change; // 涨跌额
        double pricePosition;
        String label;
        double futureReturn;

        static String[] getHeader() {
            return new String[] {
                    "Date", "Close", "MA5", "MA10", "MA20", "MA50",
                    "EMA12", "EMA26", "RSI", "MACD", "MACD_Signal", "MACD_Hist",
                    "BB_Upper", "BB_Middle", "BB_Lower", "ROC", "Momentum", "ATR",
                    "Volume", "Amount", "Volume_MA", "Volume_Ratio",
                    "Pct_Chg", "Change", "Price_Position",
                    "Future_Return", "Label"
            };
        }

        String[] toArray() {
            return new String[] {
                    date.toString(),
                    String.format("%.4f", close),
                    String.format("%.4f", ma5),
                    String.format("%.4f", ma10),
                    String.format("%.4f", ma20),
                    String.format("%.4f", ma50),
                    String.format("%.4f", ema12),
                    String.format("%.4f", ema26),
                    String.format("%.4f", rsi),
                    String.format("%.4f", macd),
                    String.format("%.4f", macdSignal),
                    String.format("%.4f", macdHist),
                    String.format("%.4f", bbUpper),
                    String.format("%.4f", bbMiddle),
                    String.format("%.4f", bbLower),
                    String.format("%.4f", roc),
                    String.format("%.4f", momentum),
                    String.format("%.4f", atr),
                    String.format("%.2f", volume),
                    String.format("%.2f", amount),
                    String.format("%.4f", volumeMA),
                    String.format("%.4f", volumeRatio),
                    String.format("%.4f", pctChg),
                    String.format("%.4f", change),
                    String.format("%.4f", pricePosition),
                    String.format("%.6f", futureReturn),
                    label
            };
        }
    }
}
