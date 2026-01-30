package com.stocktrading.live;

import com.stocktrading.backtest.SignalGenerator;
import com.stocktrading.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * 实盘交易引擎
 * 负责实盘交易的整体流程控制
 */
public class LiveTradingEngine {

    private static final Logger logger = LoggerFactory.getLogger(LiveTradingEngine.class);

    private BrokerAPI brokerAPI;
    private SignalGenerator signalGenerator;
    private DataSource dataSource;
    private RiskManager riskManager;
    private OrderExecutor orderExecutor;

    private ScheduledExecutorService scheduler;
    private List<String> watchList; // 监控股票列表
    private boolean running;
    private TradingConfig config;

    public LiveTradingEngine(BrokerAPI brokerAPI,
            SignalGenerator signalGenerator,
            DataSource dataSource,
            TradingConfig config) {
        this.brokerAPI = brokerAPI;
        this.signalGenerator = signalGenerator;
        this.dataSource = dataSource;
        this.config = config;

        // 初始化组件
        Account account = brokerAPI.getAccount();
        this.riskManager = new RiskManager(account.getTotalAssets());
        this.orderExecutor = new OrderExecutor(brokerAPI, riskManager);
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.watchList = new ArrayList<>();
        this.running = false;

        // 应用配置
        applyConfig(config);
    }

    /**
     * 应用配置
     */
    private void applyConfig(TradingConfig config) {
        riskManager.setMaxPositionSize(config.getMaxPositionSize());
        riskManager.setMaxTotalPosition(config.getMaxTotalPosition());
        riskManager.setMinCashReserve(config.getMinCashReserve());
        riskManager.setStopLossPercent(config.getStopLossPercent());
        riskManager.setTakeProfitPercent(config.getTakeProfitPercent());
        riskManager.setMaxDailyLoss(config.getMaxDailyLoss());
    }

    /**
     * 启动交易引擎
     */
    public void start() {
        if (running) {
            logger.warn("交易引擎已在运行中");
            return;
        }

        logger.info("启动实盘交易引擎...");

        // 连接券商API
        if (!brokerAPI.connect()) {
            logger.error("连接券商API失败，无法启动");
            return;
        }

        running = true;

        // 启动订单监控
        orderExecutor.startOrderMonitoring();

        // 启动止损止盈监控
        orderExecutor.startStopLossMonitoring();

        // 启动定时任务
        startScheduledTasks();

        logger.info("交易引擎已启动，监控股票数：{}", watchList.size());
    }

    /**
     * 停止交易引擎
     */
    public void stop() {
        if (!running) {
            logger.warn("交易引擎未在运行");
            return;
        }

        logger.info("停止实盘交易引擎...");
        running = false;

        // 关闭定时任务
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        // 关闭订单执行器
        orderExecutor.shutdown();

        // 断开券商API
        brokerAPI.disconnect();

        logger.info("交易引擎已停止");
    }

    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        // 1. 每日开盘前任务（9:00）
        scheduleDaily(9, 0, this::onMarketOpen);

        // 2. 交易时段扫描任务（9:30-15:00，每分钟）
        scheduleTradingHours(this::scanAndTrade);

        // 3. 每日收盘后任务（15:30）
        scheduleDaily(15, 30, this::onMarketClose);
    }

    /**
     * 开盘前任务
     */
    private void onMarketOpen() {
        try {
            logger.info("========== 开盘前准备 ==========");

            // 重置每日风控统计
            Account account = brokerAPI.getAccount();
            riskManager.resetDailyStats(account);

            // 同步账户信息
            syncAccountInfo();

            // 输出账户状态
            printAccountStatus();

            logger.info("========== 开盘前准备完成 ==========");

        } catch (Exception e) {
            logger.error("开盘前任务执行异常", e);
        }
    }

    /**
     * 扫描并交易
     */
    private void scanAndTrade() {
        if (!running || !isMarketOpen()) {
            return;
        }

        try {
            logger.debug("开始扫描交易信号...");

            for (String symbol : watchList) {
                try {
                    processSymbol(symbol);
                } catch (Exception e) {
                    logger.error("处理股票异常：{}", symbol, e);
                }
            }

        } catch (Exception e) {
            logger.error("扫描交易异常", e);
        }
    }

    /**
     * 处理单个股票
     */
    private void processSymbol(String symbol) {
        try {
            // 1. 获取历史数据
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = LocalDate.now().minusYears(1);

            List<DataSource.StockData> historicalData = dataSource.fetchData(symbol, startDate, endDate);
            if (historicalData == null || historicalData.isEmpty()) {
                logger.warn("无法获取股票数据：{}", symbol);
                return;
            }

            // 2. 转换数据格式为特征工程所需格式
            // 注意：这里需要先将数据转换为FeatureEngineer期望的格式
            // 实际使用时可以选择：
            // a) 先将数据保存为CSV，再用FeatureEngineer处理
            // b) 扩展FeatureEngineer，添加直接接收List<StockData>的方法
            // c) 简化版本：直接用最近的几个价格计算简单特征

            // 这里使用简化版本：提取最近的价格数据作为特征
            int dataSize = Math.min(50, historicalData.size());
            double[] features = extractSimpleFeatures(historicalData, dataSize);

            // 3. 生成交易信号
            SignalGenerator.Signal signal = signalGenerator.generateSignal(features);

            // 4. 获取当前价格和持仓
            double currentPrice = brokerAPI.getCurrentPrice(symbol);
            Position position = brokerAPI.getPosition(symbol);
            int currentQuantity = position != null ? position.getQuantity() : 0;

            logger.info("股票：{} 价格：{} 信号：{} 置信度：{:.2f} 持仓：{}",
                    symbol, currentPrice, signal.action, signal.confidence, currentQuantity);

            // 5. 执行交易决策
            executeTradeDecision(symbol, signal, currentPrice, currentQuantity);

        } catch (Exception e) {
            logger.error("处理股票失败：{}", symbol, e);
        }
    }

    /**
     * 从历史数据提取简单特征
     * 这是一个简化版本，实际使用时应该使用完整的FeatureEngineer
     */
    private double[] extractSimpleFeatures(List<DataSource.StockData> data, int size) {
        // 提取最近size天的数据，计算简单的技术指标作为特征
        List<Double> closes = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();

        int start = Math.max(0, data.size() - size);
        for (int i = start; i < data.size(); i++) {
            closes.add(data.get(i).getClose());
            volumes.add(data.get(i).getVol());
        }

        // 计算简单特征：价格变化率、成交量变化率等
        // 注意：这里应该与训练模型时使用的特征保持一致
        // 实际部署时需要使用完整的FeatureEngineer计算所有24个特征
        double[] features = new double[24]; // 根据训练模型的特征数量

        if (closes.size() >= 5) {
            // MA5
            features[0] = closes.subList(closes.size() - 5, closes.size()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            // 更多特征计算...
            // 这里省略，实际使用时应该调用TechnicalIndicators类
        }

        return features;
    }

    /**
     * 执行交易决策
     */
    private void executeTradeDecision(String symbol, SignalGenerator.Signal signal,
            double currentPrice, int currentQuantity) {

        // 只在高置信度时交易
        if (signal.confidence < config.getMinConfidence()) {
            logger.debug("信号置信度不足：{} < {}", signal.confidence, config.getMinConfidence());
            return;
        }

        Account account = brokerAPI.getAccount();

        if (signal.action == SignalGenerator.SignalAction.BUY) {
            // 买入信号
            if (currentQuantity >= config.getMaxHoldings()) {
                logger.info("持仓已满，不再买入：{}", symbol);
                return;
            }

            // 计算建议买入数量
            int suggestedQty = riskManager.calculateSuggestedQuantity(symbol, currentPrice, account);

            if (suggestedQty >= 100) {
                logger.info("执行买入：{} x {} @ {}", symbol, suggestedQty, currentPrice);
                orderExecutor.executeBuy(symbol, currentPrice, suggestedQty)
                        .thenAccept(result -> {
                            if (result.isSuccess()) {
                                logger.info("买入订单提交成功：{}", result.getOrderId());
                            } else {
                                logger.warn("买入订单提交失败：{}", result.getMessage());
                            }
                        });
            } else {
                logger.debug("建议买入数量不足100股：{}", suggestedQty);
            }

        } else if (signal.action == SignalGenerator.SignalAction.SELL) {
            // 卖出信号
            if (currentQuantity > 0) {
                // 全部卖出
                logger.info("执行卖出：{} x {} @ {}", symbol, currentQuantity, currentPrice);
                orderExecutor.executeSell(symbol, currentPrice, currentQuantity)
                        .thenAccept(result -> {
                            if (result.isSuccess()) {
                                logger.info("卖出订单提交成功：{}", result.getOrderId());
                            } else {
                                logger.warn("卖出订单提交失败：{}", result.getMessage());
                            }
                        });
            }
        }
    }

    /**
     * 收盘后任务
     */
    private void onMarketClose() {
        try {
            logger.info("========== 收盘后统计 ==========");

            // 同步账户信息
            syncAccountInfo();

            // 输出账户状态
            printAccountStatus();

            // 输出持仓明细
            printPositions();

            // 输出今日交易记录
            printTodayTrades();

            logger.info("========== 收盘后统计完成 ==========");

        } catch (Exception e) {
            logger.error("收盘后任务执行异常", e);
        }
    }

    /**
     * 同步账户信息
     */
    private void syncAccountInfo() {
        Account account = brokerAPI.getAccount();

        // 更新所有持仓的当前价格
        List<String> symbols = new ArrayList<>(account.getPositions().keySet());
        if (!symbols.isEmpty()) {
            Map<String, Double> prices = brokerAPI.getCurrentPrices(symbols);
            account.updatePositionPrices(prices);
        }
    }

    /**
     * 输出账户状态
     */
    private void printAccountStatus() {
        Account account = brokerAPI.getAccount();
        logger.info("账户ID：{}", account.getAccountId());
        logger.info("可用资金：{:.2f}", account.getCash());
        logger.info("冻结资金：{:.2f}", account.getFrozenCash());
        logger.info("持仓市值：{:.2f}", account.getMarketValue());
        logger.info("总资产：{:.2f}", account.getTotalAssets());
        logger.info("持仓数量：{}", account.getPositions().size());
    }

    /**
     * 输出持仓明细
     */
    private void printPositions() {
        Account account = brokerAPI.getAccount();
        Map<String, Position> positions = account.getPositions();

        if (positions.isEmpty()) {
            logger.info("当前无持仓");
            return;
        }

        logger.info("========== 持仓明细 ==========");
        for (Position position : positions.values()) {
            logger.info("{}", position);
        }
    }

    /**
     * 输出今日交易记录
     */
    private void printTodayTrades() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<Order> todayOrders = brokerAPI.getHistoricalOrders(today, today);

        if (todayOrders.isEmpty()) {
            logger.info("今日无交易");
            return;
        }

        logger.info("========== 今日交易 ==========");
        for (Order order : todayOrders) {
            logger.info("{}", order);
        }
    }

    /**
     * 判断当前是否交易时段
     */
    private boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int time = hour * 100 + minute;

        // 上午：9:30-11:30
        // 下午：13:00-15:00
        return (time >= 930 && time <= 1130) || (time >= 1300 && time <= 1500);
    }

    /**
     * 安排每日定时任务
     */
    private void scheduleDaily(int hour, int minute, Runnable task) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(minute).withSecond(0);

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        long initialDelay = java.time.Duration.between(now, nextRun).getSeconds();

        scheduler.scheduleAtFixedRate(task, initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);

        logger.info("已安排每日任务：{}:{} 首次执行：{}", hour, minute, nextRun);
    }

    /**
     * 安排交易时段任务
     */
    private void scheduleTradingHours(Runnable task) {
        scheduler.scheduleAtFixedRate(task, 0, config.getScanInterval(), TimeUnit.SECONDS);
        logger.info("已安排交易扫描任务：每{}秒执行一次", config.getScanInterval());
    }

    // 管理方法

    /**
     * 添加监控股票
     */
    public void addToWatchList(String symbol) {
        if (!watchList.contains(symbol)) {
            watchList.add(symbol);
            logger.info("已添加到监控列表：{}", symbol);
        }
    }

    /**
     * 移除监控股票
     */
    public void removeFromWatchList(String symbol) {
        watchList.remove(symbol);
        logger.info("已从监控列表移除：{}", symbol);
    }

    /**
     * 设置监控列表
     */
    public void setWatchList(List<String> symbols) {
        this.watchList = new ArrayList<>(symbols);
        logger.info("已设置监控列表，股票数：{}", symbols.size());
    }

    /**
     * 获取账户信息
     */
    public Account getAccount() {
        return brokerAPI.getAccount();
    }

    /**
     * 手动执行买入
     */
    public CompletableFuture<OrderExecutor.OrderResult> manualBuy(String symbol, double price, int quantity) {
        return orderExecutor.executeBuy(symbol, price, quantity);
    }

    /**
     * 手动执行卖出
     */
    public CompletableFuture<OrderExecutor.OrderResult> manualSell(String symbol, double price, int quantity) {
        return orderExecutor.executeSell(symbol, price, quantity);
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(String orderId) {
        return orderExecutor.cancelOrder(orderId);
    }

    public boolean isRunning() {
        return running;
    }

    public List<String> getWatchList() {
        return new ArrayList<>(watchList);
    }

    public RiskManager getRiskManager() {
        return riskManager;
    }
}
