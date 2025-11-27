package ru.zolotuhin.cos.Lab2;

import java.util.Arrays;

class DaubechiesWavelet implements WaveletTransform {
    private static final double SQRT3 = Math.sqrt(3.0);
    private static final double SQRT2 = Math.sqrt(2.0);

    private final double[] forwardLow = {
            (1 + SQRT3) / (4 * SQRT2),
            (3 + SQRT3) / (4 * SQRT2),
            (3 - SQRT3) / (4 * SQRT2),
            (1 - SQRT3) / (4 * SQRT2)
    };

    private final double[] forwardHigh = {
            (1 - SQRT3) / (4 * SQRT2),
            -(3 - SQRT3) / (4 * SQRT2),
            (3 + SQRT3) / (4 * SQRT2),
            -(1 + SQRT3) / (4 * SQRT2)
    };

    private final double[] inverseLow = {
            (1 - SQRT3) / (4 * SQRT2),
            (3 - SQRT3) / (4 * SQRT2),
            (3 + SQRT3) / (4 * SQRT2),
            (1 + SQRT3) / (4 * SQRT2)
    };

    private final double[] inverseHigh = {
            (1 + SQRT3) / (4 * SQRT2),
            -(3 + SQRT3) / (4 * SQRT2),
            (3 - SQRT3) / (4 * SQRT2),
            -(1 - SQRT3) / (4 * SQRT2)
    };

    @Override
    public double[] forwardTransform(double[] data, int order) {
        double[] transformed = Arrays.copyOf(data, data.length);

        for (int o = 0; o < order; o++) {
            int length = transformed.length / (int) Math.pow(2, o);
            transformed = forwardStep(transformed, length);
        }

        return transformed;
    }

    private double[] forwardStep(double[] data, int length) {
        double[] transformed = new double[data.length];
        System.arraycopy(data, 0, transformed, 0, data.length);

        double[] temp = new double[length];

        for (int i = 0; i < length / 2; i++) {
            double low = 0.0;
            double high = 0.0;

            for (int k = 0; k < 4; k++) {
                int index = (2 * i + k) % length;
                low += transformed[index] * forwardLow[k];
                high += transformed[index] * forwardHigh[k];
            }

            temp[i] = low;
            temp[length / 2 + i] = high;
        }

        System.arraycopy(temp, 0, transformed, 0, length);
        return transformed;
    }

    @Override
    public double[] inverseTransform(double[] transformedData, int order) {
        double[] reconstructed = Arrays.copyOf(transformedData, transformedData.length);

        for (int o = order - 1; o >= 0; o--) {
            int length = reconstructed.length / (int) Math.pow(2, o);
            reconstructed = inverseStep(reconstructed, length);
        }

        return reconstructed;
    }

    private double[] inverseStep(double[] data, int length) {
        double[] reconstructed = new double[data.length];
        System.arraycopy(data, 0, reconstructed, 0, data.length);

        double[] temp = new double[length];

        for (int i = 0; i < length / 2; i++) {
            double a = reconstructed[i];
            double b = reconstructed[length / 2 + i];

            for (int k = 0; k < 4; k++) {
                int index = (i - k + length / 2) % (length / 2);
                if (index < 0) index += length / 2;

                if (2 * index + k % 2 < length) {
                    temp[2 * index + k % 2] += a * inverseLow[k] + b * inverseHigh[k];
                }
            }
        }

        System.arraycopy(temp, 0, reconstructed, 0, length);
        return reconstructed;
    }

    @Override
    public double[][] forwardTransform2D(double[][] data, int order) {
        double[][] transformed = new double[data.length][data[0].length];

        for (int i = 0; i < data.length; i++) {
            transformed[i] = Arrays.copyOf(data[i], data[i].length);
        }

        for (int o = 0; o < order; o++) {
            int size = data.length / (int) Math.pow(2, o);
            transformed = forwardStep2D(transformed, size);
        }

        return transformed;
    }

    private double[][] forwardStep2D(double[][] data, int size) {
        double[][] transformed = new double[data.length][data[0].length];

        for (int i = 0; i < size; i++) {
            double[] row = new double[size];
            System.arraycopy(data[i], 0, row, 0, size);
            row = forwardStep(row, size);
            System.arraycopy(row, 0, transformed[i], 0, size);
        }

        for (int j = 0; j < size; j++) {
            double[] col = new double[size];
            for (int i = 0; i < size; i++) {
                col[i] = transformed[i][j];
            }
            col = forwardStep(col, size);
            for (int i = 0; i < size; i++) {
                transformed[i][j] = col[i];
            }
        }

        return transformed;
    }

    @Override
    public double[][] inverseTransform2D(double[][] transformedData, int order) {
        double[][] reconstructed = new double[transformedData.length][transformedData[0].length];

        for (int i = 0; i < transformedData.length; i++) {
            reconstructed[i] = Arrays.copyOf(transformedData[i], transformedData[i].length);
        }

        for (int o = order - 1; o >= 0; o--) {
            int size = transformedData.length / (int) Math.pow(2, o);
            reconstructed = inverseStep2D(reconstructed, size);
        }

        return reconstructed;
    }

    private double[][] inverseStep2D(double[][] data, int size) {
        double[][] reconstructed = new double[data.length][data[0].length];

        for (int j = 0; j < size; j++) {
            double[] col = new double[size];
            for (int i = 0; i < size; i++) {
                col[i] = data[i][j];
            }
            col = inverseStep(col, size);
            for (int i = 0; i < size; i++) {
                reconstructed[i][j] = col[i];
            }
        }

        for (int i = 0; i < size; i++) {
            double[] row = new double[size];
            System.arraycopy(reconstructed[i], 0, row, 0, size);
            row = inverseStep(row, size);
            System.arraycopy(row, 0, reconstructed[i], 0, size);
        }

        return reconstructed;
    }
}
