package com.stocktrading.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.File;

/**
 * 模型持久化工具
 * 负责保存和加载训练好的模型
 */
public class ModelPersistence {

    private static final Logger log = LoggerFactory.getLogger(ModelPersistence.class);

    /**
     * 保存模型到文件
     * 
     * @param classifier   训练好的分类器
     * @param trainingData 训练数据的结构（用于后续预测）
     * @param modelPath    模型保存路径
     */
    public static void saveModel(Classifier classifier, Instances trainingData, String modelPath) throws Exception {
        // 创建目录
        File file = new File(modelPath);
        file.getParentFile().mkdirs();

        // 保存模型
        SerializationHelper.write(modelPath, classifier);

        // 保存训练数据结构（用于后续预测时的数据格式对齐）
        String headerPath = modelPath.replace(".model", "_header.arff");
        weka.core.converters.ArffSaver saver = new weka.core.converters.ArffSaver();
        saver.setInstances(new Instances(trainingData, 0)); // 只保存结构，不保存数据
        saver.setFile(new File(headerPath));
        saver.writeBatch();

        log.info("Model saved to {}", modelPath);
        log.info("Training data header saved to {}", headerPath);
    }

    /**
     * 从文件加载模型
     * 
     * @param modelPath 模型文件路径
     * @return 加载的分类器
     */
    public static Classifier loadModel(String modelPath) throws Exception {
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + modelPath);
        }

        Classifier classifier = (Classifier) SerializationHelper.read(modelPath);
        log.info("Model loaded from {}", modelPath);

        return classifier;
    }

    /**
     * 加载训练数据的结构
     * 
     * @param modelPath 模型文件路径
     * @return 训练数据结构
     */
    public static Instances loadTrainingHeader(String modelPath) throws Exception {
        String headerPath = modelPath.replace(".model", "_header.arff");
        File file = new File(headerPath);

        if (!file.exists()) {
            throw new IllegalArgumentException("Training header file does not exist: " + headerPath);
        }

        weka.core.converters.ArffLoader loader = new weka.core.converters.ArffLoader();
        loader.setFile(file);
        Instances header = loader.getStructure();

        // 设置类别索引为最后一列（Label列）
        if (header.classIndex() < 0) {
            header.setClassIndex(header.numAttributes() - 1);
            log.debug("Class index set to {} (last attribute)", header.numAttributes() - 1);
        }

        log.info("Training header loaded from {}", headerPath);
        return header;
    }

    /**
     * 检查模型文件是否存在
     */
    public static boolean modelExists(String modelPath) {
        return new File(modelPath).exists();
    }

    /**
     * 删除模型文件
     */
    public static boolean deleteModel(String modelPath) {
        boolean deleted = new File(modelPath).delete();
        String headerPath = modelPath.replace(".model", "_header.arff");
        deleted &= new File(headerPath).delete();

        if (deleted) {
            log.info("Model deleted: {}", modelPath);
        }

        return deleted;
    }
}
