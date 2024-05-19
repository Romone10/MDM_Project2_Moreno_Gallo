package ch.zhaw.deeplearningjava.biomes;

import ai.djl.Model;
import ai.djl.basicdataset.cv.classification.ImageFolder;
import ai.djl.basicmodelzoo.cv.classification.ResNetV1;
import ai.djl.metric.Metrics;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.TrainingResult;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.translate.TranslateException;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public final class BiomesTrainer {

    private static final int BATCH_SIZE = 32;
    private static final int NUM_EPOCHS = 2;
    private static final int NUM_OF_OUTPUT = 6;
    private static final int IMAGE_HEIGHT = 150;
    private static final int IMAGE_WIDTH = 150;
    private static final String MODEL_NAME = "biomeclassifier";

    public static void main(String[] args) throws IOException, TranslateException {

        Path modelDirectory = Paths.get("models");

        ImageFolder imageDataset = createDataset("images");

        RandomAccessDataset[] datasetSplits = imageDataset.randomSplit(8, 2);

        Loss trainingLoss = Loss.softmaxCrossEntropyLoss();

        TrainingConfig trainingConfig = configureTraining(trainingLoss);

        Model neuralModel = initializeModel();
        Trainer trainingManager = neuralModel.newTrainer(trainingConfig);

        trainingManager.setMetrics(new Metrics());

        Shape inputShape = new Shape(1, 3, IMAGE_HEIGHT, IMAGE_WIDTH);

        trainingManager.initialize(inputShape);

        EasyTrain.fit(trainingManager, NUM_EPOCHS, datasetSplits[0], datasetSplits[1]);

        saveTrainingResults(neuralModel, trainingManager, modelDirectory, imageDataset);

    }

    private static ImageFolder createDataset(String datasetRoot)
            throws IOException, TranslateException {
        ImageFolder dataset = ImageFolder.builder()
                .setRepositoryPath(Paths.get(datasetRoot))
                .optMaxDepth(10)
                .addTransform(new Resize(IMAGE_WIDTH, IMAGE_HEIGHT))
                .addTransform(new ToTensor())
                .setSampling(BATCH_SIZE, true)
                .build();

        dataset.prepare();
        return dataset;
    }

    private static TrainingConfig configureTraining(Loss lossFunction) {
        return new DefaultTrainingConfig(lossFunction)
                .addEvaluator(new Accuracy())
                .addTrainingListeners(TrainingListener.Defaults.logging());
    }

    private static Model initializeModel() {
        Model model = Model.newInstance(MODEL_NAME);
        Block resNet50 = ResNetV1.builder()
                .setImageShape(new Shape(3, IMAGE_HEIGHT, IMAGE_WIDTH))
                .setNumLayers(50)
                .setOutSize(NUM_OF_OUTPUT)
                .build();
        model.setBlock(resNet50);
        return model;
    }

    private static void saveTrainingResults(Model neuralModel, Trainer trainingManager, Path modelDirectory, ImageFolder imageDataset) throws IOException {
        TrainingResult trainingResult = trainingManager.getTrainingResult();
        neuralModel.setProperty("Epoch", String.valueOf(NUM_EPOCHS));
        neuralModel.setProperty("Accuracy", String.format("%.5f", trainingResult.getValidateEvaluation("Accuracy")));
        neuralModel.setProperty("Loss", String.format("%.5f", trainingResult.getValidateLoss()));
        neuralModel.save(modelDirectory, MODEL_NAME);
        
        List<String> synset = extractSynset(imageDataset);
        saveSynset(modelDirectory, synset);
    }

    private static List<String> extractSynset(ImageFolder dataset) {
        
        return dataset.getClasses().stream().sorted().collect(Collectors.toList());
    }

    private static void saveSynset(Path modelDir, List<String> synset) throws IOException {
        Path synsetFile = modelDir.resolve("synset.txt");
        try (Writer writer = Files.newBufferedWriter(synsetFile)) {
            writer.write(String.join("\n", synset));
        }
    }
}
