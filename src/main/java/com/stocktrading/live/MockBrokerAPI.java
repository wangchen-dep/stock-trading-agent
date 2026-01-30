package com.stocktrading.live;

import com.stocktrading.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟券商API实现
 * 用于测试和模拟交易，不连接真实券商系统
 */
public class MockBrokerAPI implements BrokerAPI {

    private static final Logger logger = LoggerFactory.getLogger(MockBrokerAPI.class);

    private boolean connected;
    private Account account;
    private Map<String, Order> orders;
    private AtomicLong orderIdGenerator;
    private DataSource dataSource;
    private List<OrderUpdateListener> orderListeners;
    private List<PositionUpdateListener> positionListeners;

    // 模拟交易参数
    private double slippage = 0.0001; // 滑点：0.01%
    private double commission = 0.0003; // 手续费：0.03%
    private double minCommission = 5.0; // 最低手续费

    public MockBrokerAPI(String accountId, double initialCash, DataSource dataSource) {
        this.account = new Account(accountId, initialCash);
        this.orders = new ConcurrentHashMap<>();
        this.orderIdGenerator = new AtomicLong(1000);
        this.dataSource = dataSource;
        this.orderListeners = new ArrayList<>();
        this.positionListeners = new ArrayList<>();
        this.connected = false;
    }

    @Override
    public boolean connect() {
        logger.info("连接到模拟券商API...");
        connected = true;
        logger.info("模拟券商API连接成功");
        return true;
    }

    @Override
    public void disconnect() {
        logger.info("断开模拟券商API连接");
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String submitOrder(Order order) {
        if (!connected) {
            logger.error("未连接到券商API");
            return null;
        }

        // 生成订单ID
        String orderId = "MOCK" + orderIdGenerator.getAndIncrement();
        order.setOrderId(orderId);

        // 验证订单
        if (!validateOrder(order)) {
            order.setStatus(Order.OrderStatus.REJECTED);
            notifyOrderUpdate(order);
            return null;
        }

        // 对于买入订单，冻结资金
        if (order.getOrderType() == Order.OrderType.BUY) {
            double requiredCash = calculateOrderCost(order);
            if (!account.freezeCash(requiredCash)) {
                order.setStatus(Order.OrderStatus.REJECTED);
                order.setErrorMessage("可用资金不足");
                logger.warn("订单被拒绝：{} - 可用资金不足", orderId);
                notifyOrderUpdate(order);
                return null;
            }
        }

        // 对于卖出订单，验证持仓
        if (order.getOrderType() == Order.OrderType.SELL) {
            if (!account.hasEnoughPosition(order.getSymbol(), order.getQuantity())) {
                order.setStatus(Order.OrderStatus.REJECTED);
                order.setErrorMessage("持仓不足");
                logger.warn("订单被拒绝：{} - 持仓不足", orderId);
                notifyOrderUpdate(order);
                return null;
            }
        }

        order.setStatus(Order.OrderStatus.SUBMITTED);
        orders.put(orderId, order);
        logger.info("订单已提交：{}", order);
        notifyOrderUpdate(order);

        // 模拟成交（简化版：立即成交）
        simulateFill(order);

        return orderId;
    }

    /**
     * 模拟订单成交
     */
    private void simulateFill(Order order) {
        try {
            // 获取当前价格
            double currentPrice = getCurrentPrice(order.getSymbol());

            // 计算成交价格（考虑滑点）
            double fillPrice;
            if (order.getPriceType() == Order.PriceType.MARKET) {
                // 市价单：加上滑点
                if (order.getOrderType() == Order.OrderType.BUY) {
                    fillPrice = currentPrice * (1 + slippage);
                } else {
                    fillPrice = currentPrice * (1 - slippage);
                }
            } else {
                // 限价单：使用限价
                fillPrice = order.getPrice();
                // 检查是否可以成交
                if (order.getOrderType() == Order.OrderType.BUY && fillPrice < currentPrice) {
                    logger.info("限价买单价格低于市价，暂不成交");
                    return;
                }
                if (order.getOrderType() == Order.OrderType.SELL && fillPrice > currentPrice) {
                    logger.info("限价卖单价格高于市价，暂不成交");
                    return;
                }
            }

            // 更新订单状态
            order.setFilledQuantity(order.getQuantity());
            order.setAvgFillPrice(fillPrice);
            order.setStatus(Order.OrderStatus.FILLED);

            // 更新账户
            if (order.getOrderType() == Order.OrderType.BUY) {
                account.onBuyFilled(order.getSymbol(), order.getQuantity(), fillPrice);
            } else {
                account.onSellFilled(order.getSymbol(), order.getQuantity(), fillPrice);
            }

            logger.info("订单已成交：{} @ {}", order.getOrderId(), fillPrice);
            notifyOrderUpdate(order);

            // 通知持仓更新
            Position position = account.getPositions().get(order.getSymbol());
            if (position != null) {
                notifyPositionUpdate(position);
            }

        } catch (Exception e) {
            logger.error("模拟成交失败：{}", order.getOrderId(), e);
            order.setStatus(Order.OrderStatus.REJECTED);
            order.setErrorMessage("成交失败：" + e.getMessage());
            notifyOrderUpdate(order);
        }
    }

    /**
     * 验证订单
     */
    private boolean validateOrder(Order order) {
        if (order.getSymbol() == null || order.getSymbol().isEmpty()) {
            order.setErrorMessage("股票代码不能为空");
            logger.warn("订单验证失败：股票代码为空");
            return false;
        }

        if (order.getQuantity() <= 0) {
            order.setErrorMessage("订单数量必须大于0");
            logger.warn("订单验证失败：数量无效 {}", order.getQuantity());
            return false;
        }

        if (order.getQuantity() % 100 != 0) {
            order.setErrorMessage("股票交易数量必须是100的整数倍");
            logger.warn("订单验证失败：数量不是100的整数倍 {}", order.getQuantity());
            return false;
        }

        if (order.getPriceType() == Order.PriceType.LIMIT && order.getPrice() <= 0) {
            order.setErrorMessage("限价单价格必须大于0");
            logger.warn("订单验证失败：限价无效 {}", order.getPrice());
            return false;
        }

        return true;
    }

    /**
     * 计算订单成本（含手续费）
     */
    private double calculateOrderCost(Order order) {
        double amount = order.getQuantity() * order.getPrice();
        double commissionFee = Math.max(amount * commission, minCommission);
        return amount + commissionFee;
    }

    @Override
    public boolean cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            logger.warn("订单不存在：{}", orderId);
            return false;
        }

        if (!order.isCancellable()) {
            logger.warn("订单不可取消：{} 状态：{}", orderId, order.getStatus());
            return false;
        }

        // 解冻资金（如果是买入订单）
        if (order.getOrderType() == Order.OrderType.BUY && order.getStatus() == Order.OrderStatus.SUBMITTED) {
            double frozenAmount = calculateOrderCost(order);
            account.unfreezeCash(frozenAmount);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        logger.info("订单已取消：{}", orderId);
        notifyOrderUpdate(order);

        return true;
    }

    @Override
    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }

    @Override
    public List<Order> getActiveOrders() {
        List<Order> activeOrders = new ArrayList<>();
        for (Order order : orders.values()) {
            if (!order.isFinished()) {
                activeOrders.add(order);
            }
        }
        return activeOrders;
    }

    @Override
    public List<Order> getHistoricalOrders(String startDate, String endDate) {
        List<Order> historicalOrders = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (Order order : orders.values()) {
            String orderDate = order.getCreateTime().format(formatter);
            if (orderDate.compareTo(startDate) >= 0 && orderDate.compareTo(endDate) <= 0) {
                historicalOrders.add(order);
            }
        }

        return historicalOrders;
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public Map<String, Position> getPositions() {
        return account.getPositions();
    }

    @Override
    public Position getPosition(String symbol) {
        return account.getPositions().get(symbol);
    }

    @Override
    public double getCurrentPrice(String symbol) {
        try {
            // 获取最新数据
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 12, 31);
            List<DataSource.StockData> data = dataSource.fetchData(symbol, startDate, endDate);
            if (data != null && !data.isEmpty()) {
                return data.get(data.size() - 1).getClose();
            }
        } catch (Exception e) {
            logger.error("获取价格失败：{}", symbol, e);
        }
        return 0.0;
    }

    @Override
    public Map<String, Double> getCurrentPrices(List<String> symbols) {
        Map<String, Double> prices = new HashMap<>();
        for (String symbol : symbols) {
            prices.put(symbol, getCurrentPrice(symbol));
        }
        return prices;
    }

    @Override
    public double getAvailableCash() {
        return account.getCash();
    }

    @Override
    public double getTotalAssets() {
        return account.getTotalAssets();
    }

    @Override
    public void subscribeOrderUpdates(OrderUpdateListener listener) {
        orderListeners.add(listener);
    }

    @Override
    public void subscribePositionUpdates(PositionUpdateListener listener) {
        positionListeners.add(listener);
    }

    private void notifyOrderUpdate(Order order) {
        for (OrderUpdateListener listener : orderListeners) {
            try {
                listener.onOrderUpdate(order);
            } catch (Exception e) {
                logger.error("通知订单更新失败", e);
            }
        }
    }

    private void notifyPositionUpdate(Position position) {
        for (PositionUpdateListener listener : positionListeners) {
            try {
                listener.onPositionUpdate(position);
            } catch (Exception e) {
                logger.error("通知持仓更新失败", e);
            }
        }
    }

    // 配置方法

    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    public void setCommission(double commission) {
        this.commission = commission;
    }

    public void setMinCommission(double minCommission) {
        this.minCommission = minCommission;
    }
}
