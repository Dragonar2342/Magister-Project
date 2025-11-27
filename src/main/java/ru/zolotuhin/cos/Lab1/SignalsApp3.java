package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SignalsApp3 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final int harmonicsCount = 20;

    @Override
    public void setChartPanel(ChartPanel chartPanel) {
        this.chartPanel = chartPanel;
    }

    @Override
    public List<JButton> createButtons() {
        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Спектр ступеньки", () ->
                showSpectrum(this::calculateStepSpectrum, "Амплитудный спектр ступеньки")
        ));

        buttons.add(createButton("Спектр пилы", () ->
                showSpectrum(this::calculateSawtoothSpectrum, "Амплитудный спектр пилы")
        ));

        buttons.add(createButton("Спектр треугольника", () ->
                showSpectrum(this::calculateTriangleSpectrum, "Амплитудный спектр треугольника")
        ));

        return buttons;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void showSpectrum(SpectrumCalculator calculator, String title) {
        XYSeries series = new XYSeries(title);

        for (int k = 0; k < harmonicsCount; k++) {
            double amplitude = calculator.calculate(k);
            series.add(k, amplitude);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Номер гармоники (k)",
                "Амплитуда (A_k)",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setRange(0, harmonicsCount-1);

        plot.getRenderer().setSeriesVisible(0, true);
        plot.getRenderer().setSeriesVisible(0, true);

        chartPanel.setChart(chart);
    }

    private interface SpectrumCalculator {
        double calculate(int k);
    }

    private double calculateStepSpectrum(int k) {
        if (k == 0) return 0.0;
        return Math.abs(Math.sin(Math.PI * k / 2) / (Math.PI * k));
    }

    private double calculateSawtoothSpectrum(int k) {
        if (k == 0) return 0.0;
        return 1.0 / (Math.PI * k);
    }

    private double calculateTriangleSpectrum(int k) {
        if (k == 0) return 0.5;
        if (k % 2 == 0) return 0.0;
        return 4.0 / (Math.PI * Math.PI * k * k);
    }
}
