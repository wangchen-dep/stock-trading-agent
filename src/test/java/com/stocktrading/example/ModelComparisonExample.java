package com.stocktrading.example;

import com.stocktrading.model.WekaModelTrainer;
import com.stocktrading.model.ModelEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型训练和评估示例
 * 演示如何训练和比较不同的机器学习模型
 */
public class ModelComparisonExample {

    private static final Logger log = LoggerFactory.getLogger(ModelComparisonExample.class);

    public static void main(String[] args) {
        log.info("=== Model Comparison Example ===\n");

        String dataPath = "data/features/AAPL_features.csv";
        String[] modelTypes = { "RandomForest", "SVM", "NaiveBayes", "J48" };

        WekaModelTrainer trainer = new WekaModelTrainer();

        log.info("Training and evaluating different models...\n");

        for (String modelType : modelTypes) {
            log.info("\n--- Training {} ---", modelType);

            String modelPath = String.format("models/AAPL_%s.model", modelType);

            ModelEvaluator.EvaluationResult result = trainer.trainAndEvaluate(
                    dataPath,
                    modelPath,
                    modelType,
                    0.8 // 80% training, 20% testing
            );

            if (result != null) {
                log.info("\n{} Results:", modelType);
                log.info(result.toString());
                log.info("\nDetailed Metrics:");
                log.info(result.summary);
            } else {
                log.error("Failed to train {}", modelType);
            }
        }

        log.info("\n=== Model Comparison Completed ===");
        log.info("Models saved in models/ directory");
    }
}
