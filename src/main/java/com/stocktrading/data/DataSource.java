package com.stocktrading.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 股票数据源接口
 */
public interface DataSource {

    /**
     * 获取股票历史数据
     *
     * @param symbol    股票代码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 股票数据列表
     */
    List<StockData> fetchData(String symbol, LocalDate startDate, LocalDate endDate) throws Exception;

    /**
     * 股票数据模型
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class StockData {
        private String tsCode; // 股票代码
        private String tradeDate; // 交易日期 (格式: YYYYMMDD)
        private double open; // 开盘价
        private double high; // 最高价
        private double low; // 最低价
        private double close; // 收盘价
        private double preClose; // 昨收价【除权价】
        private double change; // 涨跌额
        private double pctChg; // 涨跌幅（%）
        private double vol; // 成交量（手）
        private double amount; // 成交额（千元）

        /**
         * CSV 表头
         */
        public static String getCsvHeader() {
            return "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount";
        }

        /**
         * 转换为CSV行
         */
        public String toCsvRow() {
            return String.format("%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f",
                    tsCode, tradeDate, open, high, low, close, preClose, change, pctChg, vol, amount);
        }

        /**
         * 获取 LocalDate 格式的日期
         */
        public LocalDate getLocalDate() {
            // 将 YYYYMMDD 格式转换为 LocalDate
            if (tradeDate != null && tradeDate.length() == 8) {
                int year = Integer.parseInt(tradeDate.substring(0, 4));
                int month = Integer.parseInt(tradeDate.substring(4, 6));
                int day = Integer.parseInt(tradeDate.substring(6, 8));
                return LocalDate.of(year, month, day);
            }
            return null;
        }
    }
}
