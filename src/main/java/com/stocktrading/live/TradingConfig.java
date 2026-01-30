package com.stocktrading.live;

/**
 * 交易配置类
 * 包含实盘交易的各种配置参数
 */
public class TradingConfig {

    // 风险控制参数
    private double maxPositionSize = 0.3; // 单个持仓最大比例
    private double maxTotalPosition = 0.95; // 总持仓最大比例
    private double minCashReserve = 0.05; // 最小现金储备
    private double stopLossPercent = 0.08; // 止损比例
    private double takeProfitPercent = 0.15; // 止盈比例
    private double maxDailyLoss = 0.05; // 单日最大亏损

    // 交易参数
    private double minConfidence = 0.6; // 最小信号置信度
    private int maxHoldings = 10; // 最大持仓只数
    private int scanInterval = 60; // 扫描间隔（秒）

    // 模拟交易参数
    private double slippage = 0.0001; // 滑点
    private double commission = 0.0003; // 手续费率
    private double minCommission = 5.0; // 最低手续费

    public TradingConfig() {
    }

    /**
     * 创建默认配置
     */
    public static TradingConfig createDefault() {
        return new TradingConfig();
    }

    /**
     * 创建保守配置
     */
    public static TradingConfig createConservative() {
        TradingConfig config = new TradingConfig();
        config.setMaxPositionSize(0.2);
        config.setMaxTotalPosition(0.8);
        config.setMinCashReserve(0.2);
        config.setStopLossPercent(0.05);
        config.setTakeProfitPercent(0.1);
        config.setMinConfidence(0.7);
        config.setMaxHoldings(5);
        return config;
    }

    /**
     * 创建激进配置
     */
    public static TradingConfig createAggressive() {
        TradingConfig config = new TradingConfig();
        config.setMaxPositionSize(0.4);
        config.setMaxTotalPosition(1.0);
        config.setMinCashReserve(0.0);
        config.setStopLossPercent(0.1);
        config.setTakeProfitPercent(0.2);
        config.setMinConfidence(0.5);
        config.setMaxHoldings(15);
        return config;
    }

    // Getters and Setters

    public double getMaxPositionSize() {
        return maxPositionSize;
    }

    public void setMaxPositionSize(double maxPositionSize) {
        this.maxPositionSize = maxPositionSize;
    }

    public double getMaxTotalPosition() {
        return maxTotalPosition;
    }

    public void setMaxTotalPosition(double maxTotalPosition) {
        this.maxTotalPosition = maxTotalPosition;
    }

    public double getMinCashReserve() {
        return minCashReserve;
    }

    public void setMinCashReserve(double minCashReserve) {
        this.minCashReserve = minCashReserve;
    }

    public double getStopLossPercent() {
        return stopLossPercent;
    }

    public void setStopLossPercent(double stopLossPercent) {
        this.stopLossPercent = stopLossPercent;
    }

    public double getTakeProfitPercent() {
        return takeProfitPercent;
    }

    public void setTakeProfitPercent(double takeProfitPercent) {
        this.takeProfitPercent = takeProfitPercent;
    }

    public double getMaxDailyLoss() {
        return maxDailyLoss;
    }

    public void setMaxDailyLoss(double maxDailyLoss) {
        this.maxDailyLoss = maxDailyLoss;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public int getMaxHoldings() {
        return maxHoldings;
    }

    public void setMaxHoldings(int maxHoldings) {
        this.maxHoldings = maxHoldings;
    }

    public int getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }

    public double getSlippage() {
        return slippage;
    }

    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    public double getCommission() {
        return commission;
    }

    public void setCommission(double commission) {
        this.commission = commission;
    }

    public double getMinCommission() {
        return minCommission;
    }

    public void setMinCommission(double minCommission) {
        this.minCommission = minCommission;
    }

    @Override
    public String toString() {
        return String.format(
                "TradingConfig[maxPosition=%.1f%%, stopLoss=%.1f%%, takeProfit=%.1f%%, minConfidence=%.1f%%]",
                maxPositionSize * 100, stopLossPercent * 100, takeProfitPercent * 100, minConfidence * 100);
    }
}
