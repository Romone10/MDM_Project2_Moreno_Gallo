package ch.zhaw.deeplearningjava.biomes;

import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.basicmodelzoo.cv.classification.ResNetV1;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.nn.Block;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class Inference {

    private static final int IMAGE_HEIGHT = 150;
    private static final int IMAGE_WIDTH = 150;
    private static final String MODEL_NAME = "biomeclassifier";
    
    private Predictor<Image, Classifications> predictor;

    public Inference() {
        try {
            Model model = getModel();
            Path modelDir = Paths.get("models");
            model.load(modelDir, MODEL_NAME);

            // define a translator for pre and post processing
            Translator<Image, Classifications> translator = ImageClassificationTranslator.builder()
                    .addTransform(new Resize(IMAGE_WIDTH, IMAGE_HEIGHT))
                    .addTransform(new ToTensor())
                    .optApplySoftmax(true)
                    .build();
            predictor = model.newPredictor(translator);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Classifications predict(byte[] image) throws ModelException, TranslateException, IOException {
        InputStream is = new ByteArrayInputStream(image);
        BufferedImage bi = ImageIO.read(is);
        Image img = ImageFactory.getInstance().fromImage(bi);

        return this.predictor.predict(img);
    }

    private static Model getModel() {
        Model model = Model.newInstance(MODEL_NAME);
        Block resNet50 = ResNetV1.builder()
                .setImageShape(new ai.djl.ndarray.types.Shape(3, IMAGE_HEIGHT, IMAGE_WIDTH))
                .setNumLayers(50)
                .setOutSize(6)  // Number of output classes
                .build();
        model.setBlock(resNet50);
        return model;
    }
}
