package com.stocktrading.live;

import java.time.LocalDateTime;

/**
 * 持仓实体类
 * 表示持有的股票仓位信息
 */
public class Position {

    private String symbol; // 股票代码
    private int quantity; // 持仓数量
    private double avgCost; // 平均成本价
    private double currentPrice; // 当前价格
    private double marketValue; // 市值
    private double unrealizedPnL; // 浮动盈亏
    private double unrealizedPnLPct; // 浮动盈亏百分比
    private LocalDateTime updateTime; // 更新时间

    public Position() {
        this.updateTime = LocalDateTime.now();
    }

    public Position(String symbol, int quantity, double avgCost) {
        this();
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgCost = avgCost;
    }

    /**
     * 更新当前价格并重新计算盈亏
     */
    public void updatePrice(double currentPrice) {
        this.currentPrice = currentPrice;
        this.marketValue = quantity * currentPrice;
        this.unrealizedPnL = (currentPrice - avgCost) * quantity;
        this.unrealizedPnLPct = ((currentPrice - avgCost) / avgCost) * 100;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 增加持仓
     */
    public void addPosition(int addQuantity, double price) {
        double totalCost = (quantity * avgCost) + (addQuantity * price);
        quantity += addQuantity;
        avgCost = totalCost / quantity;
        updatePrice(price);
    }

    /**
     * 减少持仓
     */
    public void reducePosition(int reduceQuantity, double price) {
        if (reduceQuantity > quantity) {
            throw new IllegalArgumentException("减少数量不能大于持仓数量");
        }
        quantity -= reduceQuantity;
        if (quantity > 0) {
            updatePrice(price);
        } else {
            // 清仓
            avgCost = 0;
            currentPrice = 0;
            marketValue = 0;
            unrealizedPnL = 0;
            unrealizedPnLPct = 0;
        }
    }

    // Getters and Setters

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(double avgCost) {
        this.avgCost = avgCost;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    public double getUnrealizedPnL() {
        return unrealizedPnL;
    }

    public void setUnrealizedPnL(double unrealizedPnL) {
        this.unrealizedPnL = unrealizedPnL;
    }

    public double getUnrealizedPnLPct() {
        return unrealizedPnLPct;
    }

    public void setUnrealizedPnLPct(double unrealizedPnLPct) {
        this.unrealizedPnLPct = unrealizedPnLPct;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return String.format(
                "Position[symbol=%s, qty=%d, avgCost=%.2f, current=%.2f, marketValue=%.2f, PnL=%.2f(%.2f%%)]",
                symbol, quantity, avgCost, currentPrice, marketValue, unrealizedPnL, unrealizedPnLPct);
    }
}
