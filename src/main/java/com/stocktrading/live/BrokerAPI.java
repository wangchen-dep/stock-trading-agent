package com.stocktrading.live;

import java.util.List;
import java.util.Map;

/**
 * 券商API接口
 * 定义与券商交易系统交互的标准接口
 * 支持模拟交易和实盘交易的统一抽象
 */
public interface BrokerAPI {

    /**
     * 初始化连接
     * 
     * @return 是否成功
     */
    boolean connect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 检查连接状态
     * 
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 提交订单
     * 
     * @param order 订单对象
     * @return 订单ID，失败返回null
     */
    String submitOrder(Order order);

    /**
     * 取消订单
     * 
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean cancelOrder(String orderId);

    /**
     * 查询订单状态
     * 
     * @param orderId 订单ID
     * @return 订单对象
     */
    Order getOrder(String orderId);

    /**
     * 查询所有活跃订单
     * 
     * @return 订单列表
     */
    List<Order> getActiveOrders();

    /**
     * 查询历史订单
     * 
     * @param startDate 开始日期（格式：yyyyMMdd）
     * @param endDate   结束日期（格式：yyyyMMdd）
     * @return 订单列表
     */
    List<Order> getHistoricalOrders(String startDate, String endDate);

    /**
     * 获取账户信息
     * 
     * @return 账户对象
     */
    Account getAccount();

    /**
     * 获取持仓信息
     * 
     * @return 持仓映射（股票代码 -> 持仓对象）
     */
    Map<String, Position> getPositions();

    /**
     * 获取指定股票的持仓
     * 
     * @param symbol 股票代码
     * @return 持仓对象，不存在返回null
     */
    Position getPosition(String symbol);

    /**
     * 获取实时行情
     * 
     * @param symbol 股票代码
     * @return 当前价格
     */
    double getCurrentPrice(String symbol);

    /**
     * 批量获取实时行情
     * 
     * @param symbols 股票代码列表
     * @return 价格映射（股票代码 -> 价格）
     */
    Map<String, Double> getCurrentPrices(List<String> symbols);

    /**
     * 获取可用资金
     * 
     * @return 可用现金
     */
    double getAvailableCash();

    /**
     * 获取总资产
     * 
     * @return 总资产（现金+持仓市值）
     */
    double getTotalAssets();

    /**
     * 订阅订单状态更新
     * 
     * @param listener 监听器
     */
    void subscribeOrderUpdates(OrderUpdateListener listener);

    /**
     * 订阅持仓更新
     * 
     * @param listener 监听器
     */
    void subscribePositionUpdates(PositionUpdateListener listener);

    /**
     * 订单更新监听器接口
     */
    interface OrderUpdateListener {
        void onOrderUpdate(Order order);
    }

    /**
     * 持仓更新监听器接口
     */
    interface PositionUpdateListener {
        void onPositionUpdate(Position position);
    }
}
