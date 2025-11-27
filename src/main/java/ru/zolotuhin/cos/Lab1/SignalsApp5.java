package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.zolotuhin.cos.signalFiles.SignalsGenerator;
import ru.zolotuhin.cos.signalFiles.TestSignalsGenerator;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SignalsApp5 implements SignalAppInterface {
    private final int N = 720;
    private JPanel mainPanel;

    @Override
    public void setChartPanel(ChartPanel chartPanel) {
        mainPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public List<JButton> createButtons() {
        TestSignalsGenerator testSignalGenerator = new TestSignalsGenerator();
        SignalsGenerator generator = new SignalsGenerator();

        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Спектр ступеньки", () -> {
            List<Double> signal = testSignalGenerator.generateSquareSignals(N, 4);
            showSignalAnalysis(signal, "Ступенька", false);
        }));

        buttons.add(createButton("Спектр пилы", () -> {
            List<Double> signal = testSignalGenerator.generateSawtoothSignals(N);
            showSignalAnalysis(signal, "Пила", false);
        }));

        buttons.add(createButton("Спектр треугольника", () -> {
            List<Double> signal = testSignalGenerator.generateTriangleSignals(N);
            showSignalAnalysis(signal, "Треугольник", false);
        }));

        buttons.add(createButton("Кардио спектр", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getCardioSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            showSignalAnalysis(signal, "Кардиосигнал", true);
        }));

        buttons.add(createButton("Рео спектр", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getReoSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            showSignalAnalysis(signal, "Реосигнал", true);
        }));

        buttons.add(createButton("Вело спектр", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getVelaSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            showSignalAnalysis(signal, "Велосигнал", true);
        }));

        buttons.add(createButton("Спиро спектр", () -> {
            List<Double> signal = null;
            try {
                signal = generator.getSpiroSignals();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            showSignalAnalysis(signal, "Спиросигнал", true);
        }));

        return buttons;
    }

    private void showSignalAnalysis(List<Double> signal, String signalName, boolean isMedical) {
        mainPanel.removeAll();

        if (signal.size() != N) {
            signal = adjustSignalSize(signal);
        }

        double[] a = new double[N];
        double[] b = new double[N];

        for (int k = 0; k < N; k++) {
            a[k] = 0;
            b[k] = 0;
            for (int i = 0; i < N; i++) {
                double angle = 2 * Math.PI * k * i / N;
                a[k] += signal.get(i) * Math.cos(angle);
                b[k] += signal.get(i) * Math.sin(angle);
            }
            a[k] /= N;
            b[k] /= N;
        }

        XYSeries amplitudeSeries = new XYSeries("Амплитудный спектр");
        double sampleRate = 360.0;
        int displayedHarmonics = N / 2;
        for (int k = 0; k < displayedHarmonics; k++) {
            double xValue = isMedical ? (k * sampleRate / N) : k;
            amplitudeSeries.add(xValue, Math.sqrt(a[k]*a[k] + b[k]*b[k]));
        }

        JFreeChart amplitudeChart = createChart(
                amplitudeSeries,
                signalName + " - Амплитудный спектр",
                isMedical ? "Частота (Гц)" : "Номер гармоники (k)",
                "Амплитуда",
                Color.BLUE
        );

        if (isMedical) {
            XYSeries phaseSeries = new XYSeries("Фазовый спектр");
            for (int k = 0; k < displayedHarmonics; k++) {
                double frequency = k * sampleRate / N;
                phaseSeries.add(frequency, Math.atan2(b[k], a[k]));
            }

            JFreeChart phaseChart = createChart(
                    phaseSeries,
                    signalName + " - Фазовый спектр",
                    "Частота (Гц)",
                    "Фаза (рад)",
                    Color.RED
            );

            mainPanel.add(new ChartPanel(phaseChart));
        } else {
            mainPanel.add(new JPanel());
        }

        List<Double> reconstructedSignal = reconstructSignal(a, b);

        XYSeries originalSeries = new XYSeries("Оригинал");
        XYSeries reconstructedSeries = new XYSeries("Восстановленный");

        for (int i = 0; i < N; i++) {
            originalSeries.add(i, signal.get(i));
            reconstructedSeries.add(i, reconstructedSignal.get(i));
        }

        JFreeChart comparisonChart = createComparisonChart(
                originalSeries,
                reconstructedSeries,
                signalName + " - Сравнение",
                "Отсчеты",
                "Амплитуда"
        );

        mainPanel.add(new ChartPanel(amplitudeChart));
        mainPanel.add(new ChartPanel(comparisonChart));

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private List<Double> reconstructSignal(double[] a, double[] b) {
        List<Double> reconstructed = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            double value = 0;
            for (int k = 0; k < N; k++) {
                double angle = 2 * Math.PI * k * i / N;
                value += a[k] * Math.cos(angle) + b[k] * Math.sin(angle);
            }
            reconstructed.add(value);
        }
        return reconstructed;
    }

    private JFreeChart createChart(XYSeries series, String title,
                                   String xLabel, String yLabel, Color color) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, dataset);

        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesPaint(0, color);

        return chart;
    }

    private JFreeChart createComparisonChart(XYSeries series1, XYSeries series2,
                                             String title, String xLabel, String yLabel) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, dataset);

        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(1.5f));
        plot.getRenderer().setSeriesPaint(1, Color.RED);

        return chart;
    }

    private List<Double> adjustSignalSize(List<Double> signal) {
        List<Double> adjusted = new ArrayList<>(signal);
        while (adjusted.size() < 720) {
            adjusted.add(0.0);
        }
        return adjusted.subList(0, 720);
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }
}