package com.stocktrading.backtest;

import java.util.ArrayList;
import java.util.List;

/**
 * 回测性能指标计算器
 */
public class PerformanceMetrics {

    /**
     * 计算回测性能指标
     * 
     * @param portfolio    投资组合
     * @param dailyReturns 每日收益率列表
     * @param finalPrice   最终价格
     * @return 性能指标结果
     */
    public static MetricsResult calculate(Portfolio portfolio, List<Double> dailyReturns, double finalPrice) {
        MetricsResult result = new MetricsResult();

        // 总收益
        double totalValue = portfolio.getTotalValue(finalPrice);
        result.totalReturn = (totalValue - portfolio.getInitialCash()) / portfolio.getInitialCash();
        result.finalValue = totalValue;

        // 交易统计
        List<Portfolio.Trade> trades = portfolio.getTrades();
        result.totalTrades = trades.size();

        // 计算盈利和亏损交易
        int wins = 0;
        int losses = 0;
        double totalProfit = 0.0;
        double totalLoss = 0.0;

        for (int i = 0; i < trades.size(); i++) {
            Portfolio.Trade trade = trades.get(i);
            if (trade.type == Portfolio.TradeType.SELL && i > 0) {
                // 找到对应的买入交易
                Portfolio.Trade buyTrade = trades.get(i - 1);
                if (buyTrade.type == Portfolio.TradeType.BUY) {
                    double profitLoss = (trade.price - buyTrade.price) * trade.shares;
                    if (profitLoss > 0) {
                        wins++;
                        totalProfit += profitLoss;
                    } else {
                        losses++;
                        totalLoss += Math.abs(profitLoss);
                    }
                }
            }
        }

        result.winningTrades = wins;
        result.losingTrades = losses;
        result.winRate = wins + losses > 0 ? (double) wins / (wins + losses) : 0.0;
        result.profitLossRatio = totalLoss > 0 ? totalProfit / totalLoss : 0.0;

        // 年化收益率（假设交易日为252天）
        int tradingDays = dailyReturns.size();
        double years = tradingDays / 252.0;
        result.annualizedReturn = years > 0 ? Math.pow(1 + result.totalReturn, 1.0 / years) - 1 : 0.0;

        // 夏普比率（假设无风险利率为0）
        if (!dailyReturns.isEmpty()) {
            double avgReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = dailyReturns.stream()
                    .mapToDouble(r -> Math.pow(r - avgReturn, 2))
                    .average()
                    .orElse(0.0);
            double stdDev = Math.sqrt(variance);

            result.sharpeRatio = stdDev > 0 ? (avgReturn * Math.sqrt(252)) / (stdDev * Math.sqrt(252)) : 0.0;
            result.volatility = stdDev * Math.sqrt(252);
        }

        // 最大回撤
        result.maxDrawdown = calculateMaxDrawdown(dailyReturns);

        return result;
    }

    /**
     * 计算最大回撤
     */
    private static double calculateMaxDrawdown(List<Double> returns) {
        if (returns.isEmpty())
            return 0.0;

        double peak = 0.0;
        double maxDrawdown = 0.0;
        double cumReturn = 0.0;

        for (double ret : returns) {
            cumReturn += ret;
            peak = Math.max(peak, cumReturn);
            double drawdown = (peak - cumReturn) / (1 + peak);
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }

        return maxDrawdown;
    }

    /**
     * 性能指标结果类
     */
    public static class MetricsResult {
        public double totalReturn; // 总收益率
        public double annualizedReturn; // 年化收益率
        public double sharpeRatio; // 夏普比率
        public double maxDrawdown; // 最大回撤
        public double volatility; // 波动率
        public int totalTrades; // 总交易次数
        public int winningTrades; // 盈利交易次数
        public int losingTrades; // 亏损交易次数
        public double winRate; // 胜率
        public double profitLossRatio; // 盈亏比
        public double finalValue; // 最终资产价值

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== Backtest Performance Metrics ===\n");
            sb.append(String.format("Final Value: $%.2f\n", finalValue));
            sb.append(String.format("Total Return: %.2f%%\n", totalReturn * 100));
            sb.append(String.format("Annualized Return: %.2f%%\n", annualizedReturn * 100));
            sb.append(String.format("Sharpe Ratio: %.4f\n", sharpeRatio));
            sb.append(String.format("Max Drawdown: %.2f%%\n", maxDrawdown * 100));
            sb.append(String.format("Volatility: %.2f%%\n", volatility * 100));
            sb.append(String.format("Total Trades: %d\n", totalTrades));
            sb.append(String.format("Winning Trades: %d\n", winningTrades));
            sb.append(String.format("Losing Trades: %d\n", losingTrades));
            sb.append(String.format("Win Rate: %.2f%%\n", winRate * 100));
            sb.append(String.format("Profit/Loss Ratio: %.2f\n", profitLossRatio));
            return sb.toString();
        }
    }
}
