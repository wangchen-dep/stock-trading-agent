package com.stocktrading.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * 交易信号生成器
 * 使用训练好的模型预测并生成买卖信号
 */
public class SignalGenerator {

    private static final Logger log = LoggerFactory.getLogger(SignalGenerator.class);

    private final Classifier classifier;
    private final Instances header;
    private final double buyThreshold;
    private final double sellThreshold;

    /**
     * 构造函数
     * 
     * @param classifier    训练好的分类器
     * @param header        训练数据结构
     * @param buyThreshold  买入阈值（预测上涨概率需大于此值）
     * @param sellThreshold 卖出阈值（预测下跌概率需大于此值）
     */
    public SignalGenerator(Classifier classifier, Instances header,
            double buyThreshold, double sellThreshold) {
        this.classifier = classifier;
        this.header = header;
        this.buyThreshold = buyThreshold;
        this.sellThreshold = sellThreshold;
    }

    /**
     * 生成交易信号
     * 
     * @param features 特征数组（不包括日期、未来收益和标签）
     * @return 信号类型
     */
    public Signal generateSignal(double[] features) {
        try {
            // 创建实例（特征数 + 1 个类别列）
            Instance instance = new DenseInstance(header.numAttributes());
            instance.setDataset(header);

            // 设置特征值（所有属性除了最后一个类别列）
            for (int i = 0; i < features.length && i < header.numAttributes() - 1; i++) {
                instance.setValue(i, features[i]);
            }

            // 类别列会自动设置为缺失值（用于预测）

            // 预测类别分布
            double[] distribution = classifier.distributionForInstance(instance);

            // 获取预测的类别
            double prediction = classifier.classifyInstance(instance);
            String predictedClass = header.classAttribute().value((int) prediction);

            // 根据概率和阈值生成信号
            Signal signal = new Signal();
            signal.predictedClass = predictedClass;
            signal.probabilities = distribution;

            // 假设类别顺序为：DOWN, HOLD, UP
            double upProb = distribution[2]; // UP的概率
            double downProb = distribution[0]; // DOWN的概率

            if (upProb > buyThreshold) {
                signal.action = SignalAction.BUY;
                signal.confidence = upProb;
            } else if (downProb > sellThreshold) {
                signal.action = SignalAction.SELL;
                signal.confidence = downProb;
            } else {
                signal.action = SignalAction.HOLD;
                signal.confidence = Math.max(upProb, downProb);
            }

            return signal;

        } catch (Exception e) {
            log.error("Error generating signal", e);
            return new Signal(SignalAction.HOLD, "HOLD", new double[] { 0, 1, 0 }, 0.0);
        }
    }

    /**
     * 信号动作枚举
     */
    public enum SignalAction {
        BUY, // 买入
        SELL, // 卖出
        HOLD // 持有
    }

    /**
     * 交易信号类
     */
    public static class Signal {
        public SignalAction action;
        public String predictedClass;
        public double[] probabilities;
        public double confidence;

        public Signal() {
        }

        public Signal(SignalAction action, String predictedClass,
                double[] probabilities, double confidence) {
            this.action = action;
            this.predictedClass = predictedClass;
            this.probabilities = probabilities;
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return String.format("Signal{action=%s, predicted=%s, confidence=%.4f}",
                    action, predictedClass, confidence);
        }
    }
}
