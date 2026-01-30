package com.stocktrading.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 订单执行器
 * 负责订单的提交、监控和执行管理
 */
public class OrderExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OrderExecutor.class);

    private BrokerAPI brokerAPI;
    private RiskManager riskManager;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    private ConcurrentHashMap<String, Order> pendingOrders;

    public OrderExecutor(BrokerAPI brokerAPI, RiskManager riskManager) {
        this.brokerAPI = brokerAPI;
        this.riskManager = riskManager;
        this.executorService = Executors.newFixedThreadPool(5);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.pendingOrders = new ConcurrentHashMap<>();
    }

    /**
     * 执行买入订单
     */
    public CompletableFuture<OrderResult> executeBuy(String symbol, double price, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("准备买入：{} @ {} x {}", symbol, price, quantity);

                // 创建订单
                Order order = new Order(symbol, Order.OrderType.BUY,
                        Order.PriceType.LIMIT, price, quantity);

                // 风险检查
                Account account = brokerAPI.getAccount();
                RiskManager.RiskCheckResult riskCheck = riskManager.checkOrder(order, account);

                if (!riskCheck.isPass()) {
                    logger.warn("买入订单未通过风险检查：{}", riskCheck.getReason());
                    return new OrderResult(false, null, riskCheck.getReason());
                }

                // 提交订单
                String orderId = brokerAPI.submitOrder(order);

                if (orderId != null) {
                    pendingOrders.put(orderId, order);
                    logger.info("买入订单已提交：{}", orderId);
                    return new OrderResult(true, orderId, "订单已提交");
                } else {
                    logger.error("买入订单提交失败：{}", symbol);
                    return new OrderResult(false, null, "订单提交失败");
                }

            } catch (Exception e) {
                logger.error("执行买入订单异常：{}", symbol, e);
                return new OrderResult(false, null, "执行异常：" + e.getMessage());
            }
        }, executorService);
    }

    /**
     * 执行卖出订单
     */
    public CompletableFuture<OrderResult> executeSell(String symbol, double price, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("准备卖出：{} @ {} x {}", symbol, price, quantity);

                // 创建订单
                Order order = new Order(symbol, Order.OrderType.SELL,
                        Order.PriceType.LIMIT, price, quantity);

                // 风险检查
                Account account = brokerAPI.getAccount();
                RiskManager.RiskCheckResult riskCheck = riskManager.checkOrder(order, account);

                if (!riskCheck.isPass()) {
                    logger.warn("卖出订单未通过风险检查：{}", riskCheck.getReason());
                    return new OrderResult(false, null, riskCheck.getReason());
                }

                // 提交订单
                String orderId = brokerAPI.submitOrder(order);

                if (orderId != null) {
                    pendingOrders.put(orderId, order);
                    logger.info("卖出订单已提交：{}", orderId);
                    return new OrderResult(true, orderId, "订单已提交");
                } else {
                    logger.error("卖出订单提交失败：{}", symbol);
                    return new OrderResult(false, null, "订单提交失败");
                }

            } catch (Exception e) {
                logger.error("执行卖出订单异常：{}", symbol, e);
                return new OrderResult(false, null, "执行异常：" + e.getMessage());
            }
        }, executorService);
    }

    /**
     * 执行市价买入
     */
    public CompletableFuture<OrderResult> executeMarketBuy(String symbol, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取当前价格
                double currentPrice = brokerAPI.getCurrentPrice(symbol);

                // 创建市价订单
                Order order = new Order(symbol, Order.OrderType.BUY,
                        Order.PriceType.MARKET, currentPrice, quantity);

                // 风险检查
                Account account = brokerAPI.getAccount();
                RiskManager.RiskCheckResult riskCheck = riskManager.checkOrder(order, account);

                if (!riskCheck.isPass()) {
                    return new OrderResult(false, null, riskCheck.getReason());
                }

                // 提交订单
                String orderId = brokerAPI.submitOrder(order);

                if (orderId != null) {
                    pendingOrders.put(orderId, order);
                    return new OrderResult(true, orderId, "市价买入订单已提交");
                } else {
                    return new OrderResult(false, null, "订单提交失败");
                }

            } catch (Exception e) {
                logger.error("执行市价买入异常", e);
                return new OrderResult(false, null, "执行异常：" + e.getMessage());
            }
        }, executorService);
    }

    /**
     * 执行市价卖出
     */
    public CompletableFuture<OrderResult> executeMarketSell(String symbol, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取当前价格
                double currentPrice = brokerAPI.getCurrentPrice(symbol);

                // 创建市价订单
                Order order = new Order(symbol, Order.OrderType.SELL,
                        Order.PriceType.MARKET, currentPrice, quantity);

                // 风险检查
                Account account = brokerAPI.getAccount();
                RiskManager.RiskCheckResult riskCheck = riskManager.checkOrder(order, account);

                if (!riskCheck.isPass()) {
                    return new OrderResult(false, null, riskCheck.getReason());
                }

                // 提交订单
                String orderId = brokerAPI.submitOrder(order);

                if (orderId != null) {
                    pendingOrders.put(orderId, order);
                    return new OrderResult(true, orderId, "市价卖出订单已提交");
                } else {
                    return new OrderResult(false, null, "订单提交失败");
                }

            } catch (Exception e) {
                logger.error("执行市价卖出异常", e);
                return new OrderResult(false, null, "执行异常：" + e.getMessage());
            }
        }, executorService);
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(String orderId) {
        Order order = pendingOrders.get(orderId);
        if (order != null && order.isCancellable()) {
            boolean success = brokerAPI.cancelOrder(orderId);
            if (success) {
                pendingOrders.remove(orderId);
                logger.info("订单已取消：{}", orderId);
            }
            return success;
        }
        return false;
    }

    /**
     * 启动订单监控
     * 定期检查挂单状态
     */
    public void startOrderMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                for (String orderId : pendingOrders.keySet()) {
                    Order order = brokerAPI.getOrder(orderId);
                    if (order != null && order.isFinished()) {
                        pendingOrders.remove(orderId);
                        logger.info("订单完成：{} 状态：{}", orderId, order.getStatus());
                    }
                }
            } catch (Exception e) {
                logger.error("订单监控异常", e);
            }
        }, 1, 5, TimeUnit.SECONDS);

        logger.info("订单监控已启动");
    }

    /**
     * 启动止损止盈监控
     */
    public void startStopLossMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                Account account = brokerAPI.getAccount();

                for (Position position : account.getPositions().values()) {
                    // 检查止损
                    if (riskManager.shouldStopLoss(position)) {
                        logger.warn("触发止损：{}", position.getSymbol());
                        executeMarketSell(position.getSymbol(), position.getQuantity());
                    }

                    // 检查止盈
                    if (riskManager.shouldTakeProfit(position)) {
                        logger.info("触发止盈：{}", position.getSymbol());
                        executeMarketSell(position.getSymbol(), position.getQuantity());
                    }
                }

                // 检查每日亏损
                riskManager.checkDailyLoss(account);

            } catch (Exception e) {
                logger.error("止损止盈监控异常", e);
            }
        }, 5, 10, TimeUnit.SECONDS);

        logger.info("止损止盈监控已启动");
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        logger.info("关闭订单执行器...");
        executorService.shutdown();
        scheduledExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
        }
        logger.info("订单执行器已关闭");
    }

    /**
     * 订单执行结果类
     */
    public static class OrderResult {
        private boolean success;
        private String orderId;
        private String message;

        public OrderResult(boolean success, String orderId, String message) {
            this.success = success;
            this.orderId = orderId;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("OrderResult[success=%s, orderId=%s, message=%s]",
                    success, orderId, message);
        }
    }
}
