package ru.zolotuhin.cos.Lab2;

import java.util.Arrays;

class SignalUtils {

    public static double[] trimToPowerOfTwo(double[] signal) {
        int newLength = 1;
        while (newLength * 2 <= signal.length) {
            newLength *= 2;
        }
        return Arrays.copyOf(signal, newLength);
    }

    public static double[] applyThreshold(double[] data, double threshold) {
        double[] result = Arrays.copyOf(data, data.length);
        for (int i = 0; i < result.length; i++) {
            if (Math.abs(result[i]) < threshold) {
                result[i] = 0.0;
            }
        }
        return result;
    }

    public static double calculateCompressionRatio(double[] data, double threshold) {
        int nonZero = 0;
        for (double value : data) {
            if (Math.abs(value) >= threshold) {
                nonZero++;
            }
        }
        return (nonZero / (double) data.length) * 100.0;
    }
}
