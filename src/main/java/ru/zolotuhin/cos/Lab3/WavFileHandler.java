package ru.zolotuhin.cos.Lab3;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

abstract class WavFileHandler {
    protected static final int SAMPLE_RATE = 44100;
    protected static final int BITS_PER_SAMPLE = 16;
    protected static final int HEADER_SIZE = 44;
    protected static JTabbedPane tabbedPane;

    protected short[] readAudioData(String filePath) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(filePath))) {
            byte[] header = new byte[HEADER_SIZE];
            dis.readFully(header);

            List<Short> samples = new ArrayList<>();
            try {
                while (true) {
                    short sample = Short.reverseBytes(dis.readShort());
                    samples.add(sample);
                }
            } catch (EOFException e) {
            }

            short[] audioData = new short[samples.size()];
            for (int i = 0; i < samples.size(); i++) {
                audioData[i] = samples.get(i);
            }
            return audioData;
        }
    }

    protected void writeAudioData(String filePath, short[] audioData) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            writeWavHeader(dos, audioData.length);

            for (short sample : audioData) {
                dos.writeShort(Short.reverseBytes(sample));
            }
        }
    }

    private void writeWavHeader(DataOutputStream dos, int dataSize) throws IOException {
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + dataSize * 2));
        dos.writeBytes("WAVE");

        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short)1));
        dos.writeShort(Short.reverseBytes((short)1));
        dos.writeInt(Integer.reverseBytes(SAMPLE_RATE));
        dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * BITS_PER_SAMPLE / 8));
        dos.writeShort(Short.reverseBytes((short)(BITS_PER_SAMPLE / 8)));
        dos.writeShort(Short.reverseBytes((short)BITS_PER_SAMPLE));

        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(dataSize * 2));
    }

    protected void addChartToTabbedPane(String tabTitle, String chartTitle, double[] data, String xLabel, String yLabel) {
        XYSeries series = new XYSeries("Данные");
        for (int i = 0; i < data.length; i++) {
            series.add(i, data[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle, xLabel, yLabel, dataset);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));

        tabbedPane.addTab(tabTitle, chartPanel);
    }
}
