package com.stocktrading.live;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 账户信息类
 * 包含资金和持仓信息
 */
public class Account {

    private String accountId; // 账户ID
    private double cash; // 可用资金
    private double frozenCash; // 冻结资金
    private double totalAssets; // 总资产
    private double marketValue; // 市值
    private double totalPnL; // 总盈亏
    private double totalPnLPct; // 总盈亏百分比
    private Map<String, Position> positions; // 持仓
    private LocalDateTime updateTime; // 更新时间

    public Account(String accountId, double initialCash) {
        this.accountId = accountId;
        this.cash = initialCash;
        this.frozenCash = 0.0;
        this.positions = new HashMap<>();
        this.updateTime = LocalDateTime.now();
        updateTotalAssets();
    }

    /**
     * 更新总资产
     */
    public void updateTotalAssets() {
        marketValue = positions.values().stream()
                .mapToDouble(Position::getMarketValue)
                .sum();
        totalAssets = cash + frozenCash + marketValue;
        updateTime = LocalDateTime.now();
    }

    /**
     * 冻结资金（买入订单提交时）
     */
    public boolean freezeCash(double amount) {
        if (cash >= amount) {
            cash -= amount;
            frozenCash += amount;
            updateTotalAssets();
            return true;
        }
        return false;
    }

    /**
     * 解冻资金（订单取消时）
     */
    public void unfreezeCash(double amount) {
        frozenCash -= amount;
        cash += amount;
        updateTotalAssets();
    }

    /**
     * 买入成交后更新
     */
    public void onBuyFilled(String symbol, int quantity, double price) {
        double cost = quantity * price;
        frozenCash -= cost;

        Position position = positions.get(symbol);
        if (position == null) {
            position = new Position(symbol, quantity, price);
            positions.put(symbol, position);
        } else {
            position.addPosition(quantity, price);
        }

        updateTotalAssets();
    }

    /**
     * 卖出成交后更新
     */
    public void onSellFilled(String symbol, int quantity, double price) {
        double proceeds = quantity * price;
        cash += proceeds;

        Position position = positions.get(symbol);
        if (position != null) {
            position.reducePosition(quantity, price);
            if (position.getQuantity() == 0) {
                positions.remove(symbol);
            }
        }

        updateTotalAssets();
    }

    /**
     * 更新持仓价格
     */
    public void updatePositionPrices(Map<String, Double> prices) {
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            Position position = positions.get(entry.getKey());
            if (position != null) {
                position.updatePrice(entry.getValue());
            }
        }
        updateTotalAssets();
    }

    /**
     * 获取指定股票的持仓数量
     */
    public int getPositionQuantity(String symbol) {
        Position position = positions.get(symbol);
        return position != null ? position.getQuantity() : 0;
    }

    /**
     * 检查是否有足够的持仓可卖
     */
    public boolean hasEnoughPosition(String symbol, int quantity) {
        return getPositionQuantity(symbol) >= quantity;
    }

    // Getters and Setters

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public double getFrozenCash() {
        return frozenCash;
    }

    public void setFrozenCash(double frozenCash) {
        this.frozenCash = frozenCash;
    }

    public double getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(double totalAssets) {
        this.totalAssets = totalAssets;
    }

    public double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    public double getTotalPnL() {
        return totalPnL;
    }

    public void setTotalPnL(double totalPnL) {
        this.totalPnL = totalPnL;
    }

    public double getTotalPnLPct() {
        return totalPnLPct;
    }

    public void setTotalPnLPct(double totalPnLPct) {
        this.totalPnLPct = totalPnLPct;
    }

    public Map<String, Position> getPositions() {
        return positions;
    }

    public void setPositions(Map<String, Position> positions) {
        this.positions = positions;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return String.format("Account[id=%s, cash=%.2f, frozen=%.2f, marketValue=%.2f, totalAssets=%.2f, positions=%d]",
                accountId, cash, frozenCash, marketValue, totalAssets, positions.size());
    }
}
