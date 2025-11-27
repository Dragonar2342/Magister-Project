package ru.zolotuhin.cos.Lab2;

public interface WaveletTransform {
    double[] forwardTransform(double[] data, int order);
    double[] inverseTransform(double[] transformedData, int order);
    double[][] forwardTransform2D(double[][] data, int order);
    double[][] inverseTransform2D(double[][] transformedData, int order);
}