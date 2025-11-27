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

public class SignalsApp4 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final int N = 720;
    private final int displayedHarmonics = 20;

    @Override
    public void setChartPanel(ChartPanel chartPanel) {
        this.chartPanel = chartPanel;
    }

    @Override
    public List<JButton> createButtons() {
        TestSignalsGenerator testSignalGenerator = new TestSignalsGenerator();

        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Спектр ступеньки", () -> {
            List<Double> signal = testSignalGenerator.generateSquareSignals(N, 4);
            showSpectrum(signal, "Амплитудный спектр ступеньки");
        }));

        buttons.add(createButton("Спектр пилы", () -> {
            List<Double> signal = testSignalGenerator.generateSawtoothSignals(N);
            showSpectrum(signal, "Амплитудный спектр пилы");
        }));

        buttons.add(createButton("Спектр треугольника", () -> {
            List<Double> signal = testSignalGenerator.generateTriangleSignals(N);
            showSpectrum(signal, "Амплитудный спектр треугольника");
        }));

        return buttons;
    }

    private void showSpectrum(List<Double> signal, String title) {
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

        XYSeries spectrumSeries = new XYSeries(title);
        for (int k = 0; k < displayedHarmonics; k++) {
            double amplitude = Math.sqrt(a[k]*a[k] + b[k]*b[k]);
            spectrumSeries.add(k, amplitude);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(spectrumSeries);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Номер гармоники (k)",
                "Амплитуда",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
        plot.getDomainAxis().setRange(0, displayedHarmonics-1);

        chartPanel.setChart(chart);
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }
}
