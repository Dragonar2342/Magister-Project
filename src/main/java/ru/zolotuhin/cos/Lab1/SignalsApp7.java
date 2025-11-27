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

public class SignalsApp7 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final double sampleRate = 360.0; // Частота дискретизации (Гц)
    private JPanel mainPanel;

    @Override
    public void setChartPanel(ChartPanel chartPanel) {
        this.chartPanel = chartPanel;
        mainPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public List<JButton> createButtons() {
        SignalsGenerator generator = new SignalsGenerator();

        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Фильтрация кардио", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getCardioSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            processSignal(signal, "Кардиосигнал", FilterType.RANG_PASS, 49.0, 51.0);
        }));

        buttons.add(createButton("Фильтрация рео", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getReoSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            processSignal(signal, "Реосигнал", FilterType.LOW_PASS, 22.0, 0);
        }));

        buttons.add(createButton("Фильтрация вело", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getVelaSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            processSignal(signal, "Велосигнал", FilterType.HIGH_PASS, 3.0, 0);
        }));

        buttons.add(createButton("Фильтрация спиро", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getSpiroSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            processSignal(signal, "Спиросигнал", FilterType.BAND_PASS, 2.0, 60.0);
        }));

        return buttons;
    }

    private void processSignal(List<Double> originalSignal, String signalName,
                               FilterType filterType, double freq1, double freq2) {
        mainPanel.removeAll();
        int N = originalSignal.size();

        addSignalChart(originalSignal, signalName + " (оригинал)",
                "Отсчеты", "Амплитуда", Color.BLUE, 600, 200);

        double[] a = new double[N];
        double[] b = new double[N];
        calculateDFT(originalSignal, a, b);

        int cutoff1 = (int)(freq1 * N / sampleRate);
        int cutoff2 = (int)(freq2 * N / sampleRate);

        switch (filterType) {
            case LOW_PASS:
                for (int k = cutoff1; k <= N/2; k++) {
                    a[k] = b[k] = 0;
                    if (k > 0) {
                        a[N-k] = b[N-k] = 0;
                    }
                }
                break;

            case HIGH_PASS:
                for (int k = 0; k <= cutoff1; k++) {
                    a[k] = b[k] = 0;
                    if (k > 0) {
                        a[N-k] = b[N-k] = 0;
                    }
                }
                break;

            case BAND_PASS:
                for (int k = 0; k <= cutoff1; k++) {
                    a[k] = b[k] = 0;
                    if (k > 0) {
                        a[N-k] = b[N-k] = 0;
                    }
                }
                for (int k = cutoff2; k <= N/2; k++) {
                    a[k] = b[k] = 0;
                    if (k > 0) {
                        a[N-k] = b[N-k] = 0;
                    }
                }
                break;
            case RANG_PASS:
                for (int k = cutoff1; k <= cutoff2; k++) {
                    a[k] = b[k] = 0;
                    if (k > 0) {
                        a[N-k] = b[N-k] = 0;
                    }
                }

                // ФНЧ (75 Гц)
                int lowPassCutoff = (int)(75.0 * N / sampleRate);
                for (int k = lowPassCutoff; k <= N/2; k++) {
                    a[k] = b[k] = 0;
                    if (k > 0) {
                        a[N-k] = b[N-k] = 0;
                    }
                }
        }

        List<Double> filteredSpectrum = calculateAmplitudeSpectrum(a, b);
        addSpectrumChart(filteredSpectrum, signalName + " спектр (после фильтра)",
                "Частота (Гц)", "Амплитуда", Color.RED, 600, 200);

        List<Double> filteredSignal = inverseDFT(a, b);
        addSignalChart(filteredSignal, signalName + " (фильтрованный)",
                "Отсчеты", "Амплитуда", Color.GREEN, 600, 200);

        mainPanel.revalidate();
        mainPanel.repaint();
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

    private List<Double> inverseDFT(double[] a, double[] b) {
        int N = a.length;
        List<Double> signal = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            double value = 0;
            for (int k = 0; k < N; k++) {
                double angle = 2 * Math.PI * k * i / N;
                value += a[k] * Math.cos(angle) + b[k] * Math.sin(angle);
            }
            signal.add(value);
        }
        return signal;
    }

    private List<Double> calculateAmplitudeSpectrum(double[] a, double[] b) {
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
        double freqStep = sampleRate / spectrum.size() / 2;
        for (int k = 0; k < spectrum.size(); k++) {
            series.add(k * freqStep, spectrum.get(k));
        }
        addChart(series, title, xLabel, yLabel, color, width, height);
    }

    private void addChart(XYSeries series, String title,
                          String xLabel, String yLabel, Color color,
                          int width, int height) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title, xLabel, yLabel, dataset);

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

    private enum FilterType {
        LOW_PASS, HIGH_PASS, BAND_PASS, RANG_PASS
    }
}
