package ru.zolotuhin.ParrerelMethods.Lab6;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatusWindow extends JPanel {
    private final ConcurrentHashMap<String, JLabel> statusLabels;

    public StatusWindow() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Статус системы"));

        statusLabels = new ConcurrentHashMap<>();
        JPanel statusPanel = new JPanel(new GridLayout(6, 2, 5, 5)) ;

        addStatusRow(statusPanel, "Автопилот", "Неактивен");
        addStatusRow(statusPanel, "Камера", "Неактивна");
        addStatusRow(statusPanel, "Парковочное место", "Неактивно");
        addStatusRow(statusPanel, "Тормоз", "Неактивен");
        addStatusRow(statusPanel, "Руль", "Неактивен");
        addStatusRow(statusPanel, "Система", "Остановлена");

        add(statusPanel, BorderLayout.NORTH);
    }

    private void addStatusRow(JPanel panel, String component, String status) {
        panel.add(new JLabel(component + ":"));
        JLabel statusLabel = new JLabel(status);
        statusLabels.put(component, statusLabel);
        panel.add(statusLabel);
    }

    public void updateStatus(String component, String status) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = statusLabels.get(component);
            if (label != null) {
                label.setText(status);

                if (status.equals("Активен") || status.equals("Активна") || status.equals("Активно") || status.equals("Работает")) {
                    label.setForeground(Color.GREEN);
                } else if (status.equals("Ошибка") || status.equals("Неисправность")) {
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(Color.ORANGE);
                }
            }
        });
    }
}