package ru.zolotuhin.ModelInfoProcess.Lab5;

// MonteCarloCalculator.java
import java.util.Arrays;
import java.util.function.Function;

public class MonteCarloCalculator {

    public double calculatePi(int n) {
        int insideCircle = 0;

        for (int i = 0; i < n; i++) {
            double x = Math.random();
            double y = Math.random();

            if (x * x + y * y <= 1) {
                insideCircle++;
            }
        }

        return 4.0 * insideCircle / n;
    }

    public double calculateIntegral(Function<Double, Double> function,
                                    double a, double b, int n) {
        double sum = 0;

        for (int i = 0; i < n; i++) {
            double x = a + (b - a) * Math.random();
            sum += function.apply(x);
        }

        return (b - a) * sum / n;
    }

    public double calculateDoubleIntegral(Function<Double[], Double> function,
                                          double[] a, double[] b, int n) {
        double sum = 0;
        double volume = 1;

        for (int i = 0; i < a.length; i++) {
            volume *= (b[i] - a[i]);
        }

        for (int i = 0; i < n; i++) {
            Double[] point = new Double[a.length];
            for (int j = 0; j < a.length; j++) {
                point[j] = a[j] + (b[j] - a[j]) * Math.random();
            }
            sum += function.apply(point);
        }

        return volume * sum / n;
    }

    public double estimateError(int sampleSize, int trials, double confidence) {
        double[] results = new double[trials];

        for (int i = 0; i < trials; i++) {
            results[i] = calculatePi(sampleSize);
        }

        double mean = calculateMean(results);
        double stdDev = calculateStandardDeviation(results, mean);

        double z = getZValue(confidence);
        return z * stdDev / Math.sqrt(trials);
    }

    private double calculateMean(double[] data) {
        return Arrays.stream(data).average().orElse(0);
    }

    private double calculateStandardDeviation(double[] data, double mean) {
        double variance = Arrays.stream(data)
                .map(x -> (x - mean) * (x - mean))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    private double getZValue(double confidence) {
        switch (Double.toString(confidence)) {
            case "0.90": return 1.645;
            case "0.95": return 1.960;
            case "0.99": return 2.576;
            default: return 1.960; // по умолчанию 95%
        }
    }
}
