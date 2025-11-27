package ru.zolotuhin.cos.signalFiles;

import java.util.ArrayList;
import java.util.List;

public class TestSignalsGenerator {
    public List<Double> generateSquareSignals(int n, int k) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i % (n / k) < ( n / k) / 2) {
                series.add(i, 1.0);
            } else {
                series.add(i, 0.0);
            }
        }
        return series;
    }

    public List<Double> generateSawtoothSignals(int n) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i<n/2) {
                series.add(i, i * (1.0 / (n / 2.0)));
            } else {
                series.add(i, (i - n / 2.0) * (1.0 / (n / 2.0)) - 1);
            }
        }
        return series;
    }

    public List<Double> generateTriangleSignals(int n) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i<n/2) {
                series.add(i, i * (1.0 / (n / 2.0)));
            } else {
                series.add(i, (-1) * ((1.0 * i - n / 2.0) / (n / 2.0)) + 1);
            }
        }
        return series;
    }
}
