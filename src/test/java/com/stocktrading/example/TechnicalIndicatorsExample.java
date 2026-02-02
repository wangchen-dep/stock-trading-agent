package com.stocktrading.example;

import com.stocktrading.features.TechnicalIndicators;

import java.util.Arrays;
import java.util.List;

/**
 * 技术指标计算示例
 * 展示如何使用 TechnicalIndicators 类计算各种技术指标
 */
public class TechnicalIndicatorsExample {

    public static void main(String[] args) {
        System.out.println("=== Technical Indicators Example ===\n");

        // 示例价格数据
        List<Double> prices = Arrays.asList(
                100.0, 102.0, 101.0, 103.0, 105.0, 104.0, 106.0, 108.0, 107.0, 109.0,
                111.0, 110.0, 112.0, 114.0, 113.0, 115.0, 117.0, 116.0, 118.0, 120.0,
                119.0, 121.0, 123.0, 122.0, 124.0, 126.0, 125.0, 127.0, 129.0, 128.0);

        // 计算 SMA
        System.out.println("1. Simple Moving Average (5-period)");
        List<Double> sma5 = TechnicalIndicators.calculateSMA(prices, 5);
        printLastValues("SMA(5)", sma5, 5);

        // 计算 EMA
        System.out.println("\n2. Exponential Moving Average (5-period)");
        List<Double> ema5 = TechnicalIndicators.calculateEMA(prices, 5);
        printLastValues("EMA(5)", ema5, 5);

        // 计算 RSI
        System.out.println("\n3. Relative Strength Index (14-period)");
        List<Double> rsi = TechnicalIndicators.calculateRSI(prices, 14);
        printLastValues("RSI(14)", rsi, 5);

        // 计算 MACD
        System.out.println("\n4. MACD (12, 26, 9)");
        TechnicalIndicators.MACDResult macd = TechnicalIndicators.calculateMACD(prices, 12, 26, 9);
        System.out.println("Last 5 MACD values:");
        for (int i = Math.max(0, macd.macdLine.size() - 5); i < macd.macdLine.size(); i++) {
            System.out.printf("  [%d] MACD: %.4f, Signal: %.4f, Histogram: %.4f\n",
                    i, macd.macdLine.get(i), macd.signalLine.get(i), macd.histogram.get(i));
        }

        // 计算布林带
        System.out.println("\n5. Bollinger Bands (20-period, 2 std)");
        TechnicalIndicators.BollingerBandsResult bb = TechnicalIndicators.calculateBollingerBands(prices, 20, 2.0);
        System.out.println("Last 5 Bollinger Bands values:");
        for (int i = Math.max(0, bb.upperBand.size() - 5); i < bb.upperBand.size(); i++) {
            System.out.printf("  [%d] Upper: %.4f, Middle: %.4f, Lower: %.4f\n",
                    i, bb.upperBand.get(i), bb.middleBand.get(i), bb.lowerBand.get(i));
        }

        // 计算 ROC
        System.out.println("\n6. Rate of Change (10-period)");
        List<Double> roc = TechnicalIndicators.calculateROC(prices, 10);
        printLastValues("ROC(10)", roc, 5);

        // 计算动量
        System.out.println("\n7. Momentum (10-period)");
        List<Double> momentum = TechnicalIndicators.calculateMomentum(prices, 10);
        printLastValues("Momentum(10)", momentum, 5);

        // 计算 ATR
        System.out.println("\n8. Average True Range (14-period)");
        List<Double> highs = prices.stream().map(p -> p + 2).toList();
        List<Double> lows = prices.stream().map(p -> p - 2).toList();
        List<Double> atr = TechnicalIndicators.calculateATR(highs, lows, prices, 14);
        printLastValues("ATR(14)", atr, 5);

        System.out.println("\n=== Example Completed ===");
    }

    private static void printLastValues(String name, List<Double> values, int count) {
        System.out.println("Last " + count + " " + name + " values:");
        for (int i = Math.max(0, values.size() - count); i < values.size(); i++) {
            double value = values.get(i);
            if (Double.isNaN(value)) {
                System.out.printf("  [%d] NaN\n", i);
            } else {
                System.out.printf("  [%d] %.4f\n", i, value);
            }
        }
    }
}
