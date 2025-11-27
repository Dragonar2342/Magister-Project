package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.zolotuhin.cos.signalFiles.TestSignalsGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SignalsApp6 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final int N = 720; //
    private final int[] harmonicsCounts = {3, 5, 10, 30};
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
        TestSignalsGenerator testSignalGenerator = new TestSignalsGenerator();

        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Аппроксимация ступеньки", () -> {
            List<Double> signal = adjustSignalSize(testSignalGenerator.generateSquareSignals(N, 4));
            showSignalApproximations(signal, "Ступенька");
        }));

        buttons.add(createButton("Аппроксимация пилы", () -> {
            List<Double> signal = adjustSignalSize(testSignalGenerator.generateSawtoothSignals(N));
            showSignalApproximations(signal, "Пила");
        }));

        buttons.add(createButton("Аппроксимация треугольника", () -> {
            List<Double> signal = adjustSignalSize(testSignalGenerator.generateTriangleSignals(N));
            showSignalApproximations(signal, "Треугольник");
        }));

        return buttons;
    }

    private void showSignalApproximations(List<Double> originalSignal, String signalName) {
        mainPanel.removeAll();

        if (originalSignal.size() != N) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Неверный размер сигнала. Ожидается: " + N + ", получено: " + originalSignal.size(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        XYSeries originalSeries = new XYSeries("Оригинал");
        for (int i = 0; i < originalSignal.size(); i++) {
            originalSeries.add(i, originalSignal.get(i));
        }

        JFreeChart originalChart = createChart(originalSeries,
                signalName + " - Оригинальный сигнал", "Отсчеты", "Амплитуда", Color.BLUE);
        ChartPanel originalPanel = new ChartPanel(originalChart);
        originalPanel.setPreferredSize(new Dimension(800, 200));
        mainPanel.add(originalPanel);

        double[] a = new double[N];
        double[] b = new double[N];

        for (int k = 0; k < N; k++) {
            a[k] = 0;
            b[k] = 0;
            for (int i = 0; i < N; i++) {
                double angle = 2 * Math.PI * k * i / N;
                a[k] += originalSignal.get(i) * Math.cos(angle);
                b[k] += originalSignal.get(i) * Math.sin(angle);
            }
            a[k] /= N;
            b[k] /= N;
        }

        for (int harmonics : harmonicsCounts) {
            List<Double> approximated = new ArrayList<>();
            for (int i = 0; i < N; i++) {
                double value = 0;
                for (int k = 0; k < Math.min(harmonics, N); k++) {
                    double angle = 2 * Math.PI * k * i / N;
                    value += a[k] * Math.cos(angle) + b[k] * Math.sin(angle);
                }
                approximated.add(value);
            }

            XYSeries approxSeries = new XYSeries(String.format("%d гармоник", harmonics));
            for (int i = 0; i < approximated.size(); i++) {
                approxSeries.add(i, approximated.get(i));
            }

            JFreeChart approxChart = createChart(approxSeries,
                    String.format("%s - %d гармоник", signalName, harmonics),
                    "Отсчеты", "Амплитуда", getColorForHarmonics(harmonics));

            ChartPanel approxPanel = new ChartPanel(approxChart);
            approxPanel.setPreferredSize(new Dimension(800, 200));
            mainPanel.add(approxPanel);
        }

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private List<Double> adjustSignalSize(List<Double> signal) {
        if (signal.size() == 720) {
            return signal;
        }

        List<Double> adjusted = new ArrayList<>(signal);
        while (adjusted.size() < 720) {
            adjusted.add(0.0);
        }
        return adjusted.subList(0, 720);
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

    private Color getColorForHarmonics(int harmonics) {
        return switch (harmonics) {
            case 3 -> Color.RED;
            case 5 -> Color.GREEN.darker();
            case 10 -> Color.MAGENTA;
            case 30 -> Color.ORANGE;
            default -> Color.BLACK;
        };
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }
}
