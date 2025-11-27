package ru.zolotuhin.cos.Lab1;

import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SignalsPlotterApp extends JFrame {
    private ChartPanel chartPanel;
    private final JPanel buttonPanelGraph;
    private final List<SignalAppInterface> signalApps;

    public SignalsPlotterApp() {
        setTitle("Signal Plotter");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        signalApps = new ArrayList<>();
        signalApps.add(new SignalsApp1());
        signalApps.add(new SignalsApp2());
        signalApps.add(new SignalsApp3());
        signalApps.add(new SignalsApp4());
        signalApps.add(new SignalsApp5());
        signalApps.add(new SignalsApp6());
        signalApps.add(new SignalsApp7());
        signalApps.add(new SignalsApp8());


        JPanel buttonPanelLab = new JPanel();
        buttonPanelLab.setLayout(new BoxLayout(buttonPanelLab, BoxLayout.Y_AXIS));

        for(int i = 0; i < 8; i++) {
            JButton numberButton = new JButton("Номер " + (i + 1));
            int appIndex = i;
            numberButton.addActionListener(e -> updateGraphButtons(appIndex));
            buttonPanelLab.add(numberButton);
        }
        add(buttonPanelLab, BorderLayout.WEST);

        buttonPanelGraph = new JPanel();
        buttonPanelGraph.setLayout(new BoxLayout(buttonPanelGraph, BoxLayout.X_AXIS));
        add(buttonPanelGraph, BorderLayout.NORTH);

        chartPanel = new ChartPanel(null);
        add(chartPanel, BorderLayout.CENTER);
    }

    private void updateGraphButtons(int appIndex) {
        buttonPanelGraph.removeAll();
        SignalAppInterface app = signalApps.get(appIndex);
        app.setChartPanel(chartPanel);

        for(JButton button : app.createButtons()) {
            buttonPanelGraph.add(button);
        }

        buttonPanelGraph.revalidate();
        buttonPanelGraph.repaint();
    }
}
