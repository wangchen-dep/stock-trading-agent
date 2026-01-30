package com.stocktrading.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 风险管理器
 * 负责风险控制、仓位管理和资金管理
 */
public class RiskManager {

    private static final Logger logger = LoggerFactory.getLogger(RiskManager.class);

    // 风险参数
    private double maxPositionSize = 0.3; // 单个持仓最大比例：30%
    private double maxTotalPosition = 0.95; // 总持仓最大比例：95%
    private double minCashReserve = 0.05; // 最小现金储备：5%
    private double maxSingleOrderValue = 0.2; // 单笔订单最大金额比例：20%
    private double maxDailyLoss = 0.05; // 单日最大亏损：5%
    private double stopLossPercent = 0.08; // 止损比例：8%
    private double takeProfitPercent = 0.15; // 止盈比例：15%

    private double dailyStartAssets; // 今日开始资产
    private boolean tradingEnabled = true; // 是否允许交易

    public RiskManager(double initialAssets) {
        this.dailyStartAssets = initialAssets;
    }

    /**
     * 检查订单是否符合风险控制要求
     */
    public RiskCheckResult checkOrder(Order order, Account account) {
        RiskCheckResult result = new RiskCheckResult();

        // 检查交易是否启用
        if (!tradingEnabled) {
            result.setPass(false);
            result.setReason("交易已被禁用（触发风险控制）");
            return result;
        }

        // 对于买入订单进行风险检查
        if (order.getOrderType() == Order.OrderType.BUY) {
            return checkBuyOrder(order, account);
        } else {
            return checkSellOrder(order, account);
        }
    }

    /**
     * 检查买入订单
     */
    private RiskCheckResult checkBuyOrder(Order order, Account account) {
        RiskCheckResult result = new RiskCheckResult();
        result.setPass(true);

        double orderValue = order.getQuantity() * order.getPrice();
        double totalAssets = account.getTotalAssets();

        // 1. 检查单笔订单金额
        if (orderValue > totalAssets * maxSingleOrderValue) {
            result.setPass(false);
            result.setReason(String.format("单笔订单金额超限：%.2f > %.2f (%.1f%%)",
                    orderValue, totalAssets * maxSingleOrderValue, maxSingleOrderValue * 100));
            logger.warn("风险检查失败：{}", result.getReason());
            return result;
        }

        // 2. 检查可用资金
        if (account.getCash() < orderValue) {
            result.setPass(false);
            result.setReason(String.format("可用资金不足：%.2f < %.2f", account.getCash(), orderValue));
            logger.warn("风险检查失败：{}", result.getReason());
            return result;
        }

        // 3. 检查单个持仓比例
        Position existingPosition = account.getPositions().get(order.getSymbol());
        double currentPositionValue = existingPosition != null ? existingPosition.getMarketValue() : 0;
        double newPositionValue = currentPositionValue + orderValue;

        if (newPositionValue > totalAssets * maxPositionSize) {
            result.setPass(false);
            result.setReason(String.format("单个持仓比例超限：%.2f > %.2f (%.1f%%)",
                    newPositionValue, totalAssets * maxPositionSize, maxPositionSize * 100));
            logger.warn("风险检查失败：{}", result.getReason());
            return result;
        }

        // 4. 检查总持仓比例
        double totalPositionValue = account.getMarketValue() + orderValue;
        if (totalPositionValue > totalAssets * maxTotalPosition) {
            result.setPass(false);
            result.setReason(String.format("总持仓比例超限：%.2f > %.2f (%.1f%%)",
                    totalPositionValue, totalAssets * maxTotalPosition, maxTotalPosition * 100));
            logger.warn("风险检查失败：{}", result.getReason());
            return result;
        }

        // 5. 检查最小现金储备
        double remainingCash = account.getCash() - orderValue;
        if (remainingCash < totalAssets * minCashReserve) {
            result.setPass(false);
            result.setReason(String.format("现金储备不足：%.2f < %.2f (%.1f%%)",
                    remainingCash, totalAssets * minCashReserve, minCashReserve * 100));
            logger.warn("风险检查失败：{}", result.getReason());
            return result;
        }

        logger.info("买入订单风险检查通过：{}", order.getSymbol());
        return result;
    }

    /**
     * 检查卖出订单
     */
    private RiskCheckResult checkSellOrder(Order order, Account account) {
        RiskCheckResult result = new RiskCheckResult();
        result.setPass(true);

        // 检查持仓是否足够
        if (!account.hasEnoughPosition(order.getSymbol(), order.getQuantity())) {
            result.setPass(false);
            result.setReason(String.format("持仓不足：需要 %d，实际 %d",
                    order.getQuantity(), account.getPositionQuantity(order.getSymbol())));
            logger.warn("风险检查失败：{}", result.getReason());
            return result;
        }

        logger.info("卖出订单风险检查通过：{}", order.getSymbol());
        return result;
    }

    /**
     * 检查是否需要止损
     */
    public boolean shouldStopLoss(Position position) {
        if (position.getUnrealizedPnLPct() <= -stopLossPercent * 100) {
            logger.warn("触发止损：{} 亏损 {:.2f}%", position.getSymbol(), position.getUnrealizedPnLPct());
            return true;
        }
        return false;
    }

    /**
     * 检查是否需要止盈
     */
    public boolean shouldTakeProfit(Position position) {
        if (position.getUnrealizedPnLPct() >= takeProfitPercent * 100) {
            logger.info("触发止盈：{} 盈利 {:.2f}%", position.getSymbol(), position.getUnrealizedPnLPct());
            return true;
        }
        return false;
    }

    /**
     * 检查每日亏损
     */
    public void checkDailyLoss(Account account) {
        double currentAssets = account.getTotalAssets();
        double dailyLoss = (dailyStartAssets - currentAssets) / dailyStartAssets;

        if (dailyLoss >= maxDailyLoss) {
            logger.error("触发每日最大亏损限制：{:.2f}%，暂停交易", dailyLoss * 100);
            tradingEnabled = false;
        }
    }

    /**
     * 重置每日统计
     */
    public void resetDailyStats(Account account) {
        dailyStartAssets = account.getTotalAssets();
        tradingEnabled = true;
        logger.info("每日风控统计已重置，起始资产：{:.2f}", dailyStartAssets);
    }

    /**
     * 计算建议的买入数量
     * 
     * @param symbol  股票代码
     * @param price   价格
     * @param account 账户
     * @return 建议数量（手）
     */
    public int calculateSuggestedQuantity(String symbol, double price, Account account) {
        double totalAssets = account.getTotalAssets();

        // 计算该股票还可以买入的最大金额
        Position existingPosition = account.getPositions().get(symbol);
        double currentPositionValue = existingPosition != null ? existingPosition.getMarketValue() : 0;
        double maxAllowedValue = totalAssets * maxPositionSize - currentPositionValue;

        // 同时考虑可用资金和单笔订单限制
        double availableCash = account.getCash();
        double maxOrderValue = totalAssets * maxSingleOrderValue;

        double maxValue = Math.min(Math.min(maxAllowedValue, availableCash), maxOrderValue);

        if (maxValue <= 0) {
            return 0;
        }

        // 计算数量（取整到100股）
        int quantity = (int) (maxValue / price / 100) * 100;

        return Math.max(0, quantity);
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

    public double getMaxSingleOrderValue() {
        return maxSingleOrderValue;
    }

    public void setMaxSingleOrderValue(double maxSingleOrderValue) {
        this.maxSingleOrderValue = maxSingleOrderValue;
    }

    public double getMaxDailyLoss() {
        return maxDailyLoss;
    }

    public void setMaxDailyLoss(double maxDailyLoss) {
        this.maxDailyLoss = maxDailyLoss;
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

    public boolean isTradingEnabled() {
        return tradingEnabled;
    }

    public void setTradingEnabled(boolean tradingEnabled) {
        this.tradingEnabled = tradingEnabled;
    }

    /**
     * 风险检查结果类
     */
    public static class RiskCheckResult {
        private boolean pass;
        private String reason;

        public RiskCheckResult() {
            this.pass = true;
        }

        public boolean isPass() {
            return pass;
        }

        public void setPass(boolean pass) {
            this.pass = pass;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return pass ? "PASS" : "FAIL: " + reason;
        }
    }
}
