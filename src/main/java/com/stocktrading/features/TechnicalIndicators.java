package com.stocktrading.features;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * 技术指标计算器
 * 计算各种股票技术分析指标
 */
public class TechnicalIndicators {

    /**
     * 计算简单移动平均线 (Simple Moving Average)
     *
     * @param prices 价格数据
     * @param period 周期
     * @return MA值列表
     */
    public static List<Double> calculateSMA(List<Double> prices, int period) {
        List<Double> sma = new ArrayList<>();

        for (int i = 0; i < prices.size(); i++) {
            if (i < period - 1) {
                sma.add(Double.NaN);
            } else {
                double sum = 0.0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += prices.get(j);
                }
                sma.add(sum / period);
            }
        }

        return sma;
    }

    /**
     * 计算指数移动平均线 (Exponential Moving Average)
     *
     * @param prices 价格数据
     * @param period 周期
     * @return EMA值列表
     */
    public static List<Double> calculateEMA(List<Double> prices, int period) {
        List<Double> ema = new ArrayList<>(prices.size());
        double multiplier = 2.0 / (period + 1);

        double sum = 0.0;
        double prevEma = 0d;

        for (int i = 0; i < prices.size(); i++) {
            double price = prices.get(i);

            if (i < period - 1) {
                sum += price;
                ema.add(Double.NaN);
            } else if (i == period - 1) {
                sum += price;
                prevEma = sum / period; // SMA 作为第一个 EMA
                ema.add(prevEma);
            } else {
                prevEma = (price - prevEma) * multiplier + prevEma;
                ema.add(prevEma);
            }
        }

        return ema;
    }

    /**
     * 计算相对强弱指标 (Relative Strength Index)
     *
     * @param prices 价格数据
     * @param period 周期（通常为14）
     * @return RSI值列表
     */
    public static List<Double> calculateRSI(List<Double> prices, int period) {
        List<Double> rsi = new ArrayList<>();
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        // 计算价格变化
        for (int i = 0; i < prices.size(); i++) {
            if (i == 0) {
                rsi.add(Double.NaN);
                gains.add(0.0);
                losses.add(0.0);
            } else {
                double change = prices.get(i) - prices.get(i - 1);
                gains.add(Math.max(change, 0));
                losses.add(Math.max(-change, 0));

                if (i < period) {
                    rsi.add(Double.NaN);
                } else {
                    double avgGain = 0.0;
                    double avgLoss = 0.0;

                    for (int j = i - period + 1; j <= i; j++) {
                        avgGain += gains.get(j);
                        avgLoss += losses.get(j);
                    }

                    avgGain /= period;
                    avgLoss /= period;

                    if (avgLoss == 0) {
                        rsi.add(100.0);
                    } else {
                        double rs = avgGain / avgLoss;
                        rsi.add(100.0 - (100.0 / (1.0 + rs)));
                    }
                }
            }
        }

        return rsi;
    }

    /**
     * 计算 MACD (Moving Average Convergence Divergence)
     *
     * @param prices       价格数据
     * @param fastPeriod   快线周期（默认12）
     * @param slowPeriod   慢线周期（默认26）
     * @param signalPeriod 信号线周期（默认9）
     * @return MACD结果 [MACD线, 信号线, 柱状图]
     */
    public static MACDResult calculateMACD(List<Double> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        List<Double> fastEMA = calculateEMA(prices, fastPeriod);
        List<Double> slowEMA = calculateEMA(prices, slowPeriod);

        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            macdLine.add(fastEMA.get(i) - slowEMA.get(i));
        }

        List<Double> signalLine = calculateEMA(macdLine, signalPeriod);

        List<Double> histogram = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            histogram.add(macdLine.get(i) - signalLine.get(i));
        }

        return new MACDResult(macdLine, signalLine, histogram);
    }

    /**
     * 计算布林带 (Bollinger Bands)
     *
     * @param prices    价格数据
     * @param period    周期（默认20）
     * @param numStdDev 标准差倍数（默认2）
     * @return 布林带结果 [上轨, 中轨, 下轨]
     */
    public static BollingerBandsResult calculateBollingerBands(List<Double> prices, int period, double numStdDev) {
        List<Double> middleBand = calculateSMA(prices, period);
        List<Double> upperBand = new ArrayList<>();
        List<Double> lowerBand = new ArrayList<>();

        for (int i = 0; i < prices.size(); i++) {
            if (i < period - 1) {
                upperBand.add(Double.NaN);
                lowerBand.add(Double.NaN);
            } else {
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (int j = i - period + 1; j <= i; j++) {
                    stats.addValue(prices.get(j));
                }

                double stdDev = stats.getStandardDeviation();
                double middle = middleBand.get(i);

                upperBand.add(middle + numStdDev * stdDev);
                lowerBand.add(middle - numStdDev * stdDev);
            }
        }

        return new BollingerBandsResult(upperBand, middleBand, lowerBand);
    }

    /**
     * 计算变化率 (Rate of Change)
     *
     * @param prices 价格数据
     * @param period 周期
     * @return ROC值列表
     */
    public static List<Double> calculateROC(List<Double> prices, int period) {
        List<Double> roc = new ArrayList<>();

        for (int i = 0; i < prices.size(); i++) {
            if (i < period) {
                roc.add(Double.NaN);
            } else {
                double change = ((prices.get(i) - prices.get(i - period)) / prices.get(i - period)) * 100;
                roc.add(change);
            }
        }

        return roc;
    }

    /**
     * 计算平均真实范围 (Average True Range)
     *
     * @param highs  最高价
     * @param lows   最低价
     * @param closes 收盘价
     * @param period 周期（默认14）
     * @return ATR值列表
     */
    public static List<Double> calculateATR(List<Double> highs, List<Double> lows,
                                            List<Double> closes, int period) {
        List<Double> trueRanges = new ArrayList<>();
        List<Double> atr = new ArrayList<>();

        for (int i = 0; i < closes.size(); i++) {
            if (i == 0) {
                trueRanges.add(highs.get(i) - lows.get(i));
            } else {
                double tr1 = highs.get(i) - lows.get(i);
                double tr2 = Math.abs(highs.get(i) - closes.get(i - 1));
                double tr3 = Math.abs(lows.get(i) - closes.get(i - 1));
                trueRanges.add(Math.max(tr1, Math.max(tr2, tr3)));
            }
        }

        // 计算 ATR (使用 SMA)
        for (int i = 0; i < trueRanges.size(); i++) {
            if (i < period - 1) {
                atr.add(Double.NaN);
            } else {
                double sum = 0.0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += trueRanges.get(j);
                }
                atr.add(sum / period);
            }
        }

        return atr;
    }

    /**
     * 计算动量 (Momentum)
     *
     * @param prices 价格数据
     * @param period 周期
     * @return 动量值列表
     */
    public static List<Double> calculateMomentum(List<Double> prices, int period) {
        List<Double> momentum = new ArrayList<>();

        for (int i = 0; i < prices.size(); i++) {
            if (i < period) {
                momentum.add(Double.NaN);
            } else {
                momentum.add(prices.get(i) - prices.get(i - period));
            }
        }

        return momentum;
    }

    // 内部类：MACD 结果
    public static class MACDResult {
        public final List<Double> macdLine;
        public final List<Double> signalLine;
        public final List<Double> histogram;

        public MACDResult(List<Double> macdLine, List<Double> signalLine, List<Double> histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
    }

    // 内部类：布林带结果
    public static class BollingerBandsResult {
        public final List<Double> upperBand;
        public final List<Double> middleBand;
        public final List<Double> lowerBand;

        public BollingerBandsResult(List<Double> upperBand, List<Double> middleBand, List<Double> lowerBand) {
            this.upperBand = upperBand;
            this.middleBand = middleBand;
            this.lowerBand = lowerBand;
        }
    }
}
