package com.stocktrading.backtest;

import java.util.ArrayList;
import java.util.List;

/**
 * 投资组合管理器
 * 跟踪持仓、现金和交易历史
 */
public class Portfolio {

    private double cash; // 现金
    private double initialCash; // 初始资金
    private int shares; // 持股数量
    private double avgBuyPrice; // 平均买入价格
    private List<Trade> trades; // 交易记录
    private double commission; // 手续费率
    private double slippage; // 滑点

    /**
     * 构造函数
     * 
     * @param initialCash 初始资金
     * @param commission  手续费率（例如0.001表示0.1%）
     * @param slippage    滑点（例如0.0005表示0.05%）
     */
    public Portfolio(double initialCash, double commission, double slippage) {
        this.cash = initialCash;
        this.initialCash = initialCash;
        this.shares = 0;
        this.avgBuyPrice = 0.0;
        this.trades = new ArrayList<>();
        this.commission = commission;
        this.slippage = slippage;
    }

    /**
     * 买入股票
     * 
     * @param price 当前价格
     * @param date  交易日期
     * @return 是否成功
     */
    public boolean buy(double price, String date) {
        // 计算实际买入价格（加上滑点）
        double actualPrice = price * (1 + slippage);

        // 计算可以买入的股数（使用所有现金）
        int sharesToBuy = (int) (cash / (actualPrice * (1 + commission)));

        if (sharesToBuy <= 0) {
            return false;
        }

        // 计算总成本
        double cost = sharesToBuy * actualPrice * (1 + commission);

        // 更新持仓
        double totalValue = shares * avgBuyPrice + sharesToBuy * actualPrice;
        shares += sharesToBuy;
        avgBuyPrice = totalValue / shares;
        cash -= cost;

        // 记录交易
        trades.add(new Trade(date, TradeType.BUY, sharesToBuy, actualPrice, cost));

        return true;
    }

    /**
     * 卖出股票
     * 
     * @param price 当前价格
     * @param date  交易日期
     * @return 是否成功
     */
    public boolean sell(double price, String date) {
        if (shares <= 0) {
            return false;
        }

        // 计算实际卖出价格（减去滑点）
        double actualPrice = price * (1 - slippage);

        // 卖出所有持仓
        int sharesToSell = shares;
        double revenue = sharesToSell * actualPrice * (1 - commission);

        // 更新持仓
        cash += revenue;
        shares = 0;

        // 记录交易
        trades.add(new Trade(date, TradeType.SELL, sharesToSell, actualPrice, revenue));

        return true;
    }

    /**
     * 获取当前总资产价值
     * 
     * @param currentPrice 当前股价
     * @return 总资产价值
     */
    public double getTotalValue(double currentPrice) {
        return cash + shares * currentPrice;
    }

    /**
     * 获取当前收益率
     * 
     * @param currentPrice 当前股价
     * @return 收益率
     */
    public double getReturn(double currentPrice) {
        return (getTotalValue(currentPrice) - initialCash) / initialCash;
    }

    /**
     * 是否持有股票
     */
    public boolean hasPosition() {
        return shares > 0;
    }

    // Getters
    public double getCash() {
        return cash;
    }

    public int getShares() {
        return shares;
    }

    public double getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public double getInitialCash() {
        return initialCash;
    }

    /**
     * 交易类型
     */
    public enum TradeType {
        BUY, SELL
    }

    /**
     * 交易记录
     */
    public static class Trade {
        public final String date;
        public final TradeType type;
        public final int shares;
        public final double price;
        public final double amount;

        public Trade(String date, TradeType type, int shares, double price, double amount) {
            this.date = date;
            this.type = type;
            this.shares = shares;
            this.price = price;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return String.format("%s: %s %d shares @ %.2f (amount: %.2f)",
                    date, type, shares, price, amount);
        }
    }
}
