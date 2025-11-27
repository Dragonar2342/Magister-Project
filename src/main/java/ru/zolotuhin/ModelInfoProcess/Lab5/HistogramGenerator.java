package ru.zolotuhin.ModelInfoProcess.Lab5;

// HistogramGenerator.java
import java.util.Arrays;

public class HistogramGenerator {

    public static void printHistogram(double[] data, int bins, String title) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(title.length()));

        double min = Arrays.stream(data).min().orElse(0);
        double max = Arrays.stream(data).max().orElse(1);
        double binWidth = (max - min) / bins;

        int[] histogram = new int[bins];

        for (double value : data) {
            int binIndex = (int) ((value - min) / binWidth);
            if (binIndex == bins) binIndex--;
            histogram[binIndex]++;
        }

        int maxCount = Arrays.stream(histogram).max().orElse(1);
        int scale = Math.max(1, maxCount / 20);

        for (int i = 0; i < bins; i++) {
            double lower = min + i * binWidth;
            double upper = min + (i + 1) * binWidth;
            String bar = "*".repeat(histogram[i] / scale);
            System.out.printf("[%.3f-%.3f]: %s (%d)%n", lower, upper, bar, histogram[i]);
        }
    }

    public static void printHistogram(int[] data, int bins, String title) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(title.length()));

        int min = Arrays.stream(data).min().orElse(0);
        int max = Arrays.stream(data).max().orElse(1);

        int[] histogram = new int[bins];
        double binWidth = (double)(max - min + 1) / bins;

        for (int value : data) {
            int binIndex = (int) ((value - min) / binWidth);
            if (binIndex == bins) binIndex--;
            histogram[binIndex]++;
        }

        int maxCount = Arrays.stream(histogram).max().orElse(1);
        int scale = Math.max(1, maxCount / 20);

        for (int i = 0; i < bins; i++) {
            double lower = min + i * binWidth;
            double upper = min + (i + 1) * binWidth;
            String bar = "*".repeat(histogram[i] / scale);
            System.out.printf("[%.1f-%.1f]: %s (%d)%n", lower, upper, bar, histogram[i]);
        }
    }
}
