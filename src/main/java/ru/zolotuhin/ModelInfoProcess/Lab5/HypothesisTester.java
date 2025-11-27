package ru.zolotuhin.ModelInfoProcess.Lab5;

// HypothesisTester.java
import java.util.Arrays;

public class HypothesisTester {

    public void testExponentialHypothesis(double[] data, double lambda, double alpha) {
        int n = data.length;
        int k = 5;

         double[] theoreticalProbs = new double[k];
        double interval = 1.0 / lambda * 3 / k;

        for (int i = 0; i < k; i++) {
            double lower = i * interval;
            double upper = (i + 1) * interval;
            theoreticalProbs[i] = Math.exp(-lambda * lower) - Math.exp(-lambda * upper);
        }

        int[] observedFreq = new int[k];
        double maxVal = Arrays.stream(data).max().orElse(1.0);

        for (double value : data) {
            int bin = (int) (value / (maxVal / k));
            if (bin >= k) bin = k - 1;
            observedFreq[bin]++;
        }

        double chiSquared = calculateChiSquared(observedFreq, theoreticalProbs, n);
        double criticalValue = getChiSquaredCriticalValue(k - 1 - 1, alpha); // k-1-1 степеней свободы

        printTestResults(observedFreq, theoreticalProbs, n, chiSquared, criticalValue,
                "экспоненциальному распределению");
    }

    private double calculateChiSquared(int[] observed, double[] theoreticalProbs, int n) {
        double chiSquared = 0;
        System.out.println("\nПроверка гипотезы:");
        System.out.println("Интервал\tНаблюдаем.\tОжидаем.\tВклад в χ²");

        for (int i = 0; i < observed.length; i++) {
            double expected = n * theoreticalProbs[i];
            double diff = observed[i] - expected;
            double contribution = (diff * diff) / expected;
            chiSquared += contribution;

            System.out.printf("%d\t\t%d\t\t%.1f\t\t%.3f%n",
                    i + 1, observed[i], expected, contribution);
        }

        return chiSquared;
    }

    private double getChiSquaredCriticalValue(int degreesOfFreedom, double alpha) {
        double[][] criticalValues = {
                {1, 3.841}, {2, 5.991}, {3, 7.815}, {4, 9.488}, {5, 11.070},
                {6, 12.592}, {7, 14.067}, {8, 15.507}, {9, 16.919}, {10, 18.307}
        };

        for (double[] value : criticalValues) {
            if ((int)value[0] == degreesOfFreedom) {
                return value[1];
            }
        }
        return 9.488;
    }

    private void printTestResults(int[] observed, double[] theoreticalProbs, int n,
                                  double chiSquared, double criticalValue, String distributionName) {
        System.out.printf("χ² наблюдаемое: %.3f%n", chiSquared);
        System.out.printf("χ² критическое: %.3f%n", criticalValue);
        System.out.printf("Гипотеза о %s %s%n", distributionName,
                chiSquared < criticalValue ? "принимается" : "отвергается");
    }
}
