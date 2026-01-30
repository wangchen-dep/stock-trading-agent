package com.stocktrading.live;

import java.time.LocalDateTime;

/**
 * 订单实体类
 * 表示一个股票交易订单
 */
public class Order {

    /**
     * 订单类型枚举
     */
    public enum OrderType {
        BUY, // 买入
        SELL // 卖出
    }

    /**
     * 订单状态枚举
     */
    public enum OrderStatus {
        PENDING, // 待提交
        SUBMITTED, // 已提交
        PARTIAL, // 部分成交
        FILLED, // 已成交
        CANCELLED, // 已取消
        REJECTED // 已拒绝
    }

    /**
     * 价格类型枚举
     */
    public enum PriceType {
        MARKET, // 市价单
        LIMIT // 限价单
    }

    private String orderId; // 订单ID
    private String symbol; // 股票代码
    private OrderType orderType; // 订单类型
    private PriceType priceType; // 价格类型
    private double price; // 订单价格（限价单）
    private int quantity; // 订单数量
    private int filledQuantity; // 已成交数量
    private double avgFillPrice; // 平均成交价格
    private OrderStatus status; // 订单状态
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String errorMessage; // 错误信息

    public Order() {
        this.status = OrderStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.filledQuantity = 0;
        this.avgFillPrice = 0.0;
    }

    public Order(String symbol, OrderType orderType, PriceType priceType, double price, int quantity) {
        this();
        this.symbol = symbol;
        this.orderType = orderType;
        this.priceType = priceType;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(int filledQuantity) {
        this.filledQuantity = filledQuantity;
        this.updateTime = LocalDateTime.now();
    }

    public double getAvgFillPrice() {
        return avgFillPrice;
    }

    public void setAvgFillPrice(double avgFillPrice) {
        this.avgFillPrice = avgFillPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 判断订单是否已完成（成交、取消或拒绝）
     */
    public boolean isFinished() {
        return status == OrderStatus.FILLED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REJECTED;
    }

    /**
     * 判断订单是否可以取消
     */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING ||
                status == OrderStatus.SUBMITTED ||
                status == OrderStatus.PARTIAL;
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, symbol=%s, type=%s, priceType=%s, price=%.2f, qty=%d, filled=%d, status=%s]",
                orderId, symbol, orderType, priceType, price, quantity, filledQuantity, status);
    }
}
