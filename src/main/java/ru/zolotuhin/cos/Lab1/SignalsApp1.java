package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.zolotuhin.cos.signalFiles.SignalsGenerator;
import ru.zolotuhin.cos.signalFiles.TestSignalsGenerator;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SignalsApp1 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final int samples = 720;

    @Override
    public void setChartPanel(ChartPanel chartPanel) {
        this.chartPanel = chartPanel;
    }

    @Override
    public List<JButton> createButtons() {
        TestSignalsGenerator testSignalGenerator = new TestSignalsGenerator();
        SignalsGenerator generator = new SignalsGenerator();

        List<JButton> buttons = new ArrayList<>();

        buttons.add(createButton("Прямоугольный", () ->
                updateChart(testSignalGenerator.generateSquareSignals(samples, 5), "Прямоугольный сигнал")));
        buttons.add(createButton("Пилообразный", () ->
                updateChart(testSignalGenerator.generateSawtoothSignals(samples), "Пилообразный сигнал")));
        buttons.add(createButton("Треугольный", () ->
                updateChart(testSignalGenerator.generateTriangleSignals(samples), "Треугольный сигнал")));
        buttons.add(createButton("Кардио", () ->
        {
            try {
                updateChart(generator.getCardioSignals(), "Кардиосигнал");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }));
        buttons.add(createButton("Рео", () ->
        {
            try {
                updateChart(generator.getReoSignals(), "Реосигнал");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }));
        buttons.add(createButton("Спиро", () ->
        {
            try {
                updateChart(generator.getSpiroSignals(), "Спиросигнал");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }));
        buttons.add(createButton("Вело", () ->
        {
            try {
                updateChart(generator.getVelaSignals(), "Велосигнал");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }));

        return buttons;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }


    private void updateChart(List<Double> signal, String title) {
        XYSeries series = new XYSeries(title);
        for (int i = 0; i < signal.size(); i++) {
            series.add(i, signal.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Измерения",
                "Амплитуда",
                dataset // Данные
        );

        chartPanel.setChart(chart);
    }
}
