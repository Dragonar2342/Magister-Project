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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SignalsApp2 implements SignalAppInterface {
    private ChartPanel chartPanel;
    private final int sampleRate = 360;
    private final double duration = 2.0;
    private final int samples = (int)(sampleRate * duration);

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
                updatePhysicalChart(
                        testSignalGenerator.generateSquareSignals(samples, 4),
                        "Прямоугольный сигнал", "Время (с)", "Амплитуда (В)", 1.0)
        ));

        buttons.add(createButton("Пилообразный", () ->
                updatePhysicalChart(
                        testSignalGenerator.generateSawtoothSignals(samples),
                        "Пилообразный сигнал", "Время (с)", "Амплитуда (В)", 1.0)
        ));

        buttons.add(createButton("Треугольный", () ->
                updatePhysicalChart(
                        testSignalGenerator.generateTriangleSignals(samples),
                        "Треугольный сигнал", "Время (с)", "Амплитуда (В)", 1.0)
        ));

        buttons.add(createButton("Кардио", () -> {
            try {
                List<Double> rawData = generator.getCardioSignals();
                List<Double> physicalData = convertCardioToPhysical(rawData);
                updatePhysicalChart(
                        physicalData,
                        "Кардиосигнал", "Время (с)", "Амплитуда (мВ)", 1.0);
            } catch (FileNotFoundException ex) {
                showError("Файл кардиосигнала не найден");
            }
        }));

        buttons.add(createButton("Рео", () -> {
            try {
                List<Double> rawData = generator.getReoSignals();
                List<Double> physicalData = convertReoToPhysical(rawData);
                updatePhysicalChart(
                        physicalData,
                        "Реограмма", "Время (с)", "Амплитуда (мОм)", 0.1);
            } catch (FileNotFoundException ex) {
                showError("Файл реограммы не найден");
            }
        }));

        buttons.add(createButton("Вело", () -> {
            try {
                List<Double> rawData = generator.getVelaSignals();
                List<Double> physicalData = convertVeloToPhysical(rawData);
                updatePhysicalChart(
                        physicalData,
                        "Велоэргометрия", "Время (с)", "Амплитуда (мВ)", 1.0);
            } catch (FileNotFoundException ex) {
                showError("Файл велоэргометрии не найден");
            }
        }));

        buttons.add(createButton("Спиро", () -> {
            try {
                List<Double> rawData = generator.getSpiroSignals();
                List<Double> physicalData = convertSpiroToPhysical(rawData);
                updatePhysicalChart(
                        physicalData,
                        "Спирограмма", "Время (с)", "Амплитуда (л)", 1.0);
            } catch (FileNotFoundException ex) {
                showError("Файл спирограммы не найден");
            }
        }));

        return buttons;
    }

    private List<Double> convertCardioToPhysical(List<Double> rawData) {
        List<Double> result = new ArrayList<>();
        for (Double val : rawData) {
            result.add((val - 127) / 60); // Формула для мВ
        }
        return result;
    }

    private List<Double> convertReoToPhysical(List<Double> rawData) {
        List<Double> result = new ArrayList<>();
        for (Double val : rawData) {
            result.add(0.1 * val / 100); // Формула для мОм
        }
        return result;
    }

    private List<Double> convertVeloToPhysical(List<Double> rawData) {
        List<Double> result = new ArrayList<>();
        for (Double val : rawData) {
            result.add((val - 512) / 240); // Формула для мВ
        }
        return result;
    }

    private List<Double> convertSpiroToPhysical(List<Double> rawData) {
        List<Double> result = new ArrayList<>();
        for (Double val : rawData) {
            result.add((val - 512) / 100); // Формула для литров
        }
        return result;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void updatePhysicalChart(List<Double> signal, String title,
                                     String xAxisLabel, String yAxisLabel,
                                     double yAxisTickUnit) {
        XYSeries series = new XYSeries(title);

        for (int i = 0; i < signal.size(); i++) {
            double time = i / (double)sampleRate;
            series.add(time, signal.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setRange(0, duration);
        plot.getRangeAxis().setAutoRange(true);

        chartPanel.setChart(chart);
    }
}
