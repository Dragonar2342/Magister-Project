package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.util.List;

public interface SignalAppInterface {
    void setChartPanel(ChartPanel chartPanel);
    List<JButton> createButtons();
}
