package com.stocktrading.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.functions.SMO;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.File;

/**
 * Weka 模型训练器
 * 负责加载数据、训练模型并保存
 */
public class WekaModelTrainer {

    private static final Logger log = LoggerFactory.getLogger(WekaModelTrainer.class);

    /**
     * 训练模型并保存
     *
     * @param dataPath  特征数据文件路径
     * @param modelPath 模型保存路径
     * @return 是否成功
     */
    public boolean trainAndSave(String dataPath, String modelPath) {
        return trainAndSave(dataPath, modelPath, "RandomForest");
    }

    /**
     * 训练指定类型的模型并保存
     *
     * @param dataPath  特征数据文件路径
     * @param modelPath 模型保存路径
     * @param modelType 模型类型（RandomForest, SVM, NaiveBayes, J48, MLP）
     * @return 是否成功
     */
    public boolean trainAndSave(String dataPath, String modelPath, String modelType) {
        try {
            log.info("Loading training data from {}", dataPath);
            Instances data = loadData(dataPath);

            log.info("Training {} model with {} instances", modelType, data.numInstances());
            Classifier classifier = buildClassifier(modelType);
            classifier.buildClassifier(data);

            log.info("Saving model to {}", modelPath);
            ModelPersistence.saveModel(classifier, data, modelPath);

            log.info("Model training completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error training model", e);
            return false;
        }
    }

    /**
     * 从CSV文件加载数据并转换为Weka Instances
     */
    private Instances loadData(String filePath) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(filePath));
        Instances data = loader.getDataSet();

        // 移除日期列和未来收益列（只保留特征和标签）
        Remove remove = new Remove();
        remove.setAttributeIndices("1,23"); // Date列和Future_Return列
        remove.setInputFormat(data);
        data = Filter.useFilter(data, remove);

        // 设置最后一列（Label）为类别属性
        data.setClassIndex(data.numAttributes() - 1);

        log.info("Loaded {} instances with {} attributes",
                data.numInstances(), data.numAttributes());

        return data;
    }

    /**
     * 根据类型构建分类器
     */
    private Classifier buildClassifier(String modelType) throws Exception {
        Classifier classifier;

        switch (modelType.toUpperCase()) {
            case "RANDOMFOREST":
                RandomForest rf = new RandomForest();
                rf.setNumIterations(100); // 树的数量
                rf.setMaxDepth(10); // 最大深度
                rf.setNumFeatures(0); // 0表示使用sqrt(特征数)
                classifier = rf;
                log.info("Created Random Forest with 100 trees, max depth 10");
                break;

            case "SVM":
                SMO svm = new SMO();
                svm.setBuildCalibrationModels(true); // 输出概率
                classifier = svm;
                log.info("Created SVM classifier");
                break;

            case "NAIVEBAYES":
                classifier = new NaiveBayes();
                log.info("Created Naive Bayes classifier");
                break;

            case "J48":
                J48 j48 = new J48();
                j48.setUnpruned(false); // 剪枝
                j48.setMinNumObj(2); // 最小叶节点数
                classifier = j48;
                log.info("Created J48 decision tree");
                break;

            case "MLP":
                MultilayerPerceptron mlp = new MultilayerPerceptron();
                mlp.setLearningRate(0.1);
                mlp.setMomentum(0.2);
                mlp.setTrainingTime(500);
                mlp.setHiddenLayers("a"); // 自动设置隐藏层
                classifier = mlp;
                log.info("Created Multilayer Perceptron");
                break;

            default:
                throw new IllegalArgumentException("Unknown model type: " + modelType);
        }

        return classifier;
    }

    /**
     * 训练并评估模型
     *
     * @param dataPath       特征数据文件路径
     * @param modelPath      模型保存路径
     * @param modelType      模型类型
     * @param trainTestSplit 训练集比例（例如0.8表示80%训练，20%测试）
     * @return 评估结果
     */
    public ModelEvaluator.EvaluationResult trainAndEvaluate(String dataPath, String modelPath,
                                                            String modelType, double trainTestSplit) {
        try {
            log.info("Loading data for training and evaluation");
            Instances data = loadData(dataPath);

            // 随机化数据
            data.randomize(new java.util.Random(42));

            // 分割训练集和测试集
            int trainSize = (int) Math.round(data.numInstances() * trainTestSplit);
            Instances trainData = new Instances(data, 0, trainSize);
            Instances testData = new Instances(data, trainSize, data.numInstances() - trainSize);

            log.info("Training with {} instances, testing with {} instances",
                    trainSize, data.numInstances() - trainSize);

            // 训练模型
            Classifier classifier = buildClassifier(modelType);
            classifier.buildClassifier(trainData);

            // 保存模型
            ModelPersistence.saveModel(classifier, trainData, modelPath);

            // 评估模型
            ModelEvaluator evaluator = new ModelEvaluator();
            ModelEvaluator.EvaluationResult result = evaluator.evaluate(classifier, testData);

            log.info("Evaluation completed - Accuracy: {}%", result.accuracy);

            return result;

        } catch (Exception e) {
            log.error("Error training and evaluating model", e);
            return null;
        }
    }
}
