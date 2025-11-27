package ru.zolotuhin.cos.Lab1;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SignalsPlotterApp viewer = new SignalsPlotterApp();
            viewer.setVisible(true);
        });
    }
}
