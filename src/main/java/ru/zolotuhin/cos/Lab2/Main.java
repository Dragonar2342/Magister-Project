package ru.zolotuhin.cos.Lab2;

import ru.zolotuhin.cos.signalFiles.SignalsGenerator;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            SignalsGenerator signalsGenerator = new SignalsGenerator();

            List<Double> cardioSignal = signalsGenerator.getCardioSignals();
            List<Double> velaSignal = signalsGenerator.getVelaSignals();

            double[] cardioArray = listToArray(cardioSignal);
            double[] velaArray = listToArray(velaSignal);

            cardioArray = SignalUtils.trimToPowerOfTwo(cardioArray);
            velaArray = SignalUtils.trimToPowerOfTwo(velaArray);

            BufferedImage image = ImageUtils.loadImage("C:\\GitHub\\Magister\\src\\main\\java\\ru\\zolotuhin\\cos\\Lab2\\input.jpg");
            image = ImageUtils.resizeToPowerOfTwo(image, 512);

            WaveletTransform haar = new HaarWavelet();
            WaveletTransform daubechies = new DaubechiesWavelet();

            int transformOrder = 3; // Порядок преобразования
            double[] haarCardio = haar.forwardTransform(cardioArray, transformOrder);
            double[] daubechiesCardio = daubechies.forwardTransform(cardioArray, transformOrder);
            double[] haarVela = haar.forwardTransform(velaArray, transformOrder);
            double[] daubechiesVela = daubechies.forwardTransform(velaArray, transformOrder);

            double[][] imageData = ImageUtils.imageToArray(image);
            double[][] haarImage = haar.forwardTransform2D(imageData, 1);
            double[][] daubechiesImage = daubechies.forwardTransform2D(imageData, 1);

            Visualization.showSignal(cardioArray, "Оригинальный Кардио сигнал");
            Visualization.showSignal(haarCardio, "Вейвлет Хаара Кардио");
            Visualization.showSignal(daubechiesCardio, "Вейвлет Дебоши Кардио");

            Visualization.showSignal(velaArray, "Оригинальный Вело сигнал");
            Visualization.showSignal(haarVela, "Вейвлет Хаара Вело");
            Visualization.showSignal(daubechiesVela, "Вейвлет Дебоши Вело");

            BufferedImage haarImageVis = ImageUtils.arrayToImage(haarImage);
            BufferedImage daubechiesImageVis = ImageUtils.arrayToImage(daubechiesImage);
            Visualization.showImage(image, "Оригинальное изображение");
            Visualization.showImage(haarImageVis, "Вейвлет Хаара изображения");
            Visualization.showImage(daubechiesImageVis, "Вейвлет Дебоши изображения");

            // 4. Применение порога и восстановление
            double[] thresholds = {0.01, 0.05, 0.1}; // Различные пороговые значения
            for (double threshold : thresholds) {
                System.out.println("\nThreshold: " + threshold);

                double[] compressedHaarCardio = SignalUtils.applyThreshold(haarCardio, threshold);
                double[] reconstructedHaarCardio = haar.inverseTransform(compressedHaarCardio, transformOrder);

                double[] compressedDaubechiesCardio = SignalUtils.applyThreshold(daubechiesCardio, threshold);
                double[] reconstructedDaubechiesCardio = daubechies.inverseTransform(compressedDaubechiesCardio, transformOrder);

                Visualization.showSignal(reconstructedHaarCardio, "Реконструированный Хаар (Cardio, Порог=" + threshold + ")");
                Visualization.showSignal(reconstructedDaubechiesCardio, "Реконструированный Дебоши (Cardio, Порог=" + threshold + ")");

                // 5. Расчет степени сжатия
                double compressionRatioHaar = SignalUtils.calculateCompressionRatio(haarCardio, threshold);
                double compressionRatioDaubechies = SignalUtils.calculateCompressionRatio(daubechiesCardio, threshold);

                System.out.printf("Кардиосигнал — сжатие Хаара: %.2f%%, сжатие Добеши: %.2f%%\n",
                        compressionRatioHaar, compressionRatioDaubechies);

                double[] compressedHaarVela = SignalUtils.applyThreshold(velaArray, threshold);
                double[] reconstructedHaarVela = haar.inverseTransform(compressedHaarVela, transformOrder);

                double[] compressedDaubechiesVela = SignalUtils.applyThreshold(daubechiesVela, threshold);
                double[] reconstructedDaubechiesVela = daubechies.inverseTransform(compressedDaubechiesVela, transformOrder);

                Visualization.showSignal(reconstructedHaarVela, "Реконструированный Хаар (Vela, Порог=" + threshold + ")");
                Visualization.showSignal(reconstructedDaubechiesVela, "Реконструированный Дебоши (Vela, Порог=" + threshold + ")");

                double compressionRatioHaarVela = SignalUtils.calculateCompressionRatio(haarVela, threshold);
                double compressionRatioDaubechiesVela = SignalUtils.calculateCompressionRatio(daubechiesVela, threshold);

                System.out.printf("Вело сигнал — сжатие Хаара: %.2f%%, сжатие Добеши: %.2f%%\n",
                        compressionRatioHaarVela, compressionRatioDaubechiesVela);
            }

        } catch (FileNotFoundException e) {
            System.err.println("Signal file not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Image processing error: " + e.getMessage());
        }
    }

    private static double[] listToArray(List<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}

