package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.zolotuhin.cos.signalFiles.SignalsGenerator;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SignalsApp8 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final double sampleRate = 360.0;
    private JPanel mainPanel;

    @Override
    public void setChartPanel(ChartPanel chartPanel) {
        this.chartPanel = chartPanel;
        mainPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public List<JButton> createButtons() {
        SignalsGenerator generator = new SignalsGenerator();
        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Сравнение ДПФ/БПФ", () -> {
            List<Double> originalSignal = null;
            try {
                originalSignal = generator.getSpiroSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            int originalN = originalSignal.size();
            processSignal(originalSignal, originalN);
        }));

        return buttons;
    }

    private void processSignal(List<Double> originalSignal, int originalN) {
        mainPanel.removeAll();

        int adjustedN = adjustSize(originalSignal);
        List<Double> adjustedSignal = adjustSignalSize(originalSignal, adjustedN);

        addSignalChart(originalSignal, "Оригинальный сигнал (N=" + originalN + ")",
                "Отсчеты", "Амплитуда", Color.BLUE, 800, 200);
        addSignalChart(adjustedSignal, "Адаптированный сигнал (N=" + adjustedN + ")",
                "Отсчеты", "Амплитуда", Color.RED, 800, 200);

        compareTransforms(adjustedSignal, adjustedN);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private int adjustSize(List<Double> signal) {
        int originalN = signal.size();
        int lowerPow = prevPowerOfTwo(originalN);
        int higherPow = nextPowerOfTwo(originalN);

        return (originalN - lowerPow) < (higherPow - originalN) ? lowerPow : higherPow;
    }

    private int prevPowerOfTwo(int n) {
        return Integer.highestOneBit(n);
    }

    private int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }

    private List<Double> adjustSignalSize(List<Double> signal, int newSize) {
        if (signal.size() == newSize) return signal;

        if (signal.size() > newSize) {
            return signal.subList(0, newSize);
        } else {
            List<Double> adjusted = new ArrayList<>(signal);
            while (adjusted.size() < newSize) {
                adjusted.add(0.0);
            }
            return adjusted;
        }
    }

    private void compareTransforms(List<Double> signal, int N) {
        double[] aDFT = new double[N];
        double[] bDFT = new double[N];
        double[] aFFT = new double[N];
        double[] bFFT = new double[N];

        long startDFT = System.nanoTime();
        calculateDFT(signal, aDFT, bDFT);
        long timeDFT = System.nanoTime() - startDFT;

        long startFFT = System.nanoTime();
        calculateFFT(signal, aFFT, bFFT);
        long timeFFT = System.nanoTime() - startFFT;

        List<Double> spectrumDFT = calculateSpectrum(aDFT, bDFT);
        List<Double> spectrumFFT = calculateSpectrum(aFFT, bFFT);

        addSpectrumChart(spectrumDFT, "Спектр (ДПФ, время: " + (timeDFT/1e6) + " мс)",
                "Частота, Гц", "Амплитуда", Color.BLUE, 800, 200);
        addSpectrumChart(spectrumFFT, "Спектр (БПФ, время: " + (timeFFT/1e6) + " мс)",
                "Частота, Гц", "Амплитуда", Color.RED, 800, 200);

        List<Double> diffSpectrum = new ArrayList<>();
        for (int i = 0; i < spectrumDFT.size(); i++) {
            diffSpectrum.add(Math.abs(spectrumDFT.get(i) - spectrumFFT.get(i)));
        }
        addSpectrumChart(diffSpectrum, "Разница спектров",
                "Частота, Гц", "Разница", Color.GREEN, 800, 200);
    }

    private void calculateDFT(List<Double> signal, double[] a, double[] b) {
        int N = signal.size();
        for (int k = 0; k < N; k++) {
            a[k] = b[k] = 0;
            for (int i = 0; i < N; i++) {
                double angle = 2 * Math.PI * k * i / N;
                a[k] += signal.get(i) * Math.cos(angle);
                b[k] += signal.get(i) * Math.sin(angle);
            }
            a[k] /= N;
            b[k] /= N;
        }
    }

    private void calculateFFT(List<Double> signal, double[] a, double[] b) {
        int N = signal.size();
        Complex[] complexSignal = new Complex[N];
        for (int i = 0; i < N; i++) {
            complexSignal[i] = new Complex(signal.get(i), 0);
        }

        Complex[] fftResult = fft(complexSignal);

        for (int k = 0; k < N; k++) {
            a[k] = fftResult[k].re() / N;
            b[k] = fftResult[k].im() / N;
        }
    }

    private Complex[] fft(Complex[] x) {
        int N = x.length;
        if (N == 1) return new Complex[]{x[0]};
        if (N % 2 != 0) throw new IllegalArgumentException("N не степень двойки");

        Complex[] even = new Complex[N/2];
        Complex[] odd = new Complex[N/2];
        for (int k = 0; k < N/2; k++) {
            even[k] = x[2*k];
            odd[k] = x[2*k + 1];
        }

        Complex[] q = fft(even);
        Complex[] r = fft(odd);

        Complex[] y = new Complex[N];
        for (int k = 0; k < N/2; k++) {
            double kth = -2 * k * Math.PI / N;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].plus(wk.times(r[k]));
            y[k + N/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

    private List<Double> calculateSpectrum(double[] a, double[] b) {
        int N = a.length;
        List<Double> spectrum = new ArrayList<>(N/2);
        for (int k = 0; k < N/2; k++) {
            spectrum.add(Math.sqrt(a[k]*a[k] + b[k]*b[k]));
        }
        return spectrum;
    }

    private void addSignalChart(List<Double> signal, String title,
                                String xLabel, String yLabel, Color color,
                                int width, int height) {
        XYSeries series = new XYSeries(title);
        for (int i = 0; i < signal.size(); i++) {
            series.add(i, signal.get(i));
        }
        addChart(series, title, xLabel, yLabel, color, width, height);
    }

    private void addSpectrumChart(List<Double> spectrum, String title,
                                  String xLabel, String yLabel, Color color,
                                  int width, int height) {
        XYSeries series = new XYSeries(title);
        double step = sampleRate / (spectrum.size() * 2);
        for (int k = 0; k < spectrum.size(); k++) {
            series.add(k * step, spectrum.get(k));
        }
        addChart(series, title, xLabel, yLabel, color, width, height);
    }

    private void addChart(XYSeries series, String title,
                          String xLabel, String yLabel, Color color,
                          int width, int height) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, dataset);
        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesPaint(0, color);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(width, height));
        mainPanel.add(chartPanel);
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private static class Complex {
        private final double re;
        private final double im;

        public Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }

        public double re() { return re; }
        public double im() { return im; }

        public Complex plus(Complex b) {
            return new Complex(re + b.re, im + b.im);
        }

        public Complex minus(Complex b) {
            return new Complex(re - b.re, im - b.im);
        }

        public Complex times(Complex b) {
            return new Complex(re * b.re - im * b.im, re * b.im + im * b.re);
        }
    }
}
