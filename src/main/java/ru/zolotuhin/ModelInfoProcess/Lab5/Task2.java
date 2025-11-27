package ru.zolotuhin.ModelInfoProcess.Lab5;

import java.util.Arrays;

public class Task2 {
    private SequenceGenerator sequenceGen;
    private HypothesisTester hypothesisTester;
    private HistogramGenerator histogramGenerator;

    public Task2() {
        this.sequenceGen = new SequenceGenerator();
        this.hypothesisTester = new HypothesisTester();
    }

    public void execute() {
        System.out.println("\nЗАДАНИЕ 2: Последовательности и проверка гипотез");
        System.out.println("-".repeat(50));

        generateAndAnalyzeSequences();
        testHypotheses();
    }

    private void generateAndAnalyzeSequences() {
        System.out.println("\n1. Генерация последовательностей и гистограммы:");

        // Равномерное распределение
        double[] uniformSeq = sequenceGen.generateUniformSequence(100);
        System.out.println("Равномерное распределение (первые 20 чисел):");
        printArray(Arrays.copyOf(uniformSeq, 20));
        HistogramGenerator.printHistogram(uniformSeq, 5,
                "Гистограмма равномерного распределения (100 чисел)");

        // Экспоненциальное распределение
        double[] expSeq = sequenceGen.generateExponentialSequence(2.0, 100);
        System.out.println("\nЭкспоненциальное распределение (первые 20 чисел):");
        printArray(Arrays.copyOf(expSeq, 20));
        HistogramGenerator.printHistogram(expSeq, 5,
                "Гистограмма экспоненциального распределения");

        // Распределение Пуассона
        int[] poissonSeq = sequenceGen.generatePoissonSequence(3.0, 100);
        System.out.println("\nРаспределение Пуассона (первые 20 чисел):");
        printArray(Arrays.copyOf(poissonSeq, 20));
        HistogramGenerator.printHistogram(poissonSeq, 6,
                "Гистограмма распределения Пуассона");

        // Нормальное распределение
        double[] normalSeq = sequenceGen.generateNormalSequence(0, 1, 100);
        System.out.println("\nНормальное распределение (первые 20 чисел):");
        printArray(Arrays.copyOf(normalSeq, 20));
        HistogramGenerator.printHistogram(normalSeq, 5,
                "Гистограмма нормального распределения");
    }

    private void testHypotheses() {
        System.out.println("\n2. Проверка статистических гипотез:");

        // Генерация тестовых данных
        double[] expTestData = sequenceGen.generateExponentialSequence(2.0, 200);

        // Проверка гипотезы об экспоненциальном распределении
        hypothesisTester.testExponentialHypothesis(expTestData, 2.0, 0.05);

        // Дополнительная проверка с другими параметрами
        System.out.println("\n" + "-".repeat(40));
        double[] expTestData2 = sequenceGen.generateExponentialSequence(1.5, 150);
        hypothesisTester.testExponentialHypothesis(expTestData2, 1.5, 0.05);
    }

    private void printArray(double[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.printf("%.4f ", array[i]);
            if ((i + 1) % 10 == 0) System.out.println();
        }
        System.out.println();
    }

    private void printArray(int[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.printf("%d ", array[i]);
            if ((i + 1) % 10 == 0) System.out.println();
        }
        System.out.println();
    }
}
