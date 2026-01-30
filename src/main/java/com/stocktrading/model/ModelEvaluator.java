package com.stocktrading.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.util.Random;

/**
 * 模型评估器
 * 评估模型性能并生成各种指标
 */
public class ModelEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ModelEvaluator.class);

    /**
     * 评估分类器性能
     * 
     * @param classifier 训练好的分类器
     * @param testData   测试数据集
     * @return 评估结果
     */
    public EvaluationResult evaluate(Classifier classifier, Instances testData) {
        try {
            Evaluation eval = new Evaluation(testData);
            eval.evaluateModel(classifier, testData);

            EvaluationResult result = new EvaluationResult();
            result.accuracy = eval.pctCorrect();
            result.precision = eval.weightedPrecision();
            result.recall = eval.weightedRecall();
            result.fMeasure = eval.weightedFMeasure();
            result.confusionMatrix = eval.confusionMatrix();
            result.summary = eval.toSummaryString("\n=== Evaluation Results ===\n", false);
            result.detailsByClass = eval.toClassDetailsString();
            result.confusionMatrixString = eval.toMatrixString();

            log.info("Model Evaluation:");
            log.info("Accuracy: {:.2f}%", result.accuracy);
            log.info("Precision: {:.4f}", result.precision);
            log.info("Recall: {:.4f}", result.recall);
            log.info("F-Measure: {:.4f}", result.fMeasure);

            return result;

        } catch (Exception e) {
            log.error("Error evaluating model", e);
            return null;
        }
    }

    /**
     * 使用交叉验证评估模型
     * 
     * @param classifier 分类器
     * @param data       完整数据集
     * @param folds      折数（默认10）
     * @return 评估结果
     */
    public EvaluationResult crossValidate(Classifier classifier, Instances data, int folds) {
        try {
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(classifier, data, folds, new Random(42));

            EvaluationResult result = new EvaluationResult();
            result.accuracy = eval.pctCorrect();
            result.precision = eval.weightedPrecision();
            result.recall = eval.weightedRecall();
            result.fMeasure = eval.weightedFMeasure();
            result.confusionMatrix = eval.confusionMatrix();
            result.summary = eval.toSummaryString("\n=== " + folds + "-Fold Cross-Validation Results ===\n", false);
            result.detailsByClass = eval.toClassDetailsString();
            result.confusionMatrixString = eval.toMatrixString();

            log.info("{}-Fold Cross-Validation Results:", folds);
            log.info("Accuracy: {:.2f}%", result.accuracy);
            log.info("Precision: {:.4f}", result.precision);
            log.info("Recall: {:.4f}", result.recall);
            log.info("F-Measure: {:.4f}", result.fMeasure);

            return result;

        } catch (Exception e) {
            log.error("Error in cross-validation", e);
            return null;
        }
    }

    /**
     * 打印详细的评估结果
     */
    public void printDetailedResults(EvaluationResult result) {
        if (result == null)
            return;

        log.info("\n" + result.summary);
        log.info("\n" + result.detailsByClass);
        log.info("\n" + result.confusionMatrixString);
    }

    /**
     * 评估结果类
     */
    public static class EvaluationResult {
        public double accuracy;
        public double precision;
        public double recall;
        public double fMeasure;
        public double[][] confusionMatrix;
        public String summary;
        public String detailsByClass;
        public String confusionMatrixString;

        @Override
        public String toString() {
            return String.format(
                    "Accuracy: %.2f%%, Precision: %.4f, Recall: %.4f, F-Measure: %.4f",
                    accuracy, precision, recall, fMeasure);
        }
    }
}
