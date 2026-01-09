package ru.zolotuhin.ParrerelMethods.Lab5;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Steering extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;

    // GUI компоненты
    private JLabel currentAngleLabel;
    private JLabel commandLabel;
    private JProgressBar angleBar;
    private JTextArea logArea;

    private int operationCount = 0;
    private int currentAngle = 0;

    public Steering(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;

        initializeGUI();
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Рулевое управление"));

        // Панель текущего состояния
        JPanel statusPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        currentAngleLabel = new JLabel("Текущий угол: 0°", JLabel.CENTER);
        currentAngleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        commandLabel = new JLabel("Последняя команда: Нет", JLabel.CENTER);

        angleBar = new JProgressBar(-90, 90);
        angleBar.setValue(0);
        angleBar.setStringPainted(true);
        angleBar.setString("0°");

        statusPanel.add(currentAngleLabel);
        statusPanel.add(commandLabel);
        statusPanel.add(angleBar);

        // Область логов
        logArea = new JTextArea(6, 20);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(statusPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void start() {
        thread = new Thread(this, "Steering-Thread");
        thread.start();
    }

    public void interrupt() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    public void join(long millis) throws InterruptedException {
        if (thread != null) {
            thread.join(millis);
        }
    }

    @Override
    public void run() {
        logMessage("Руль запущен");

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                operationCount++;
                logMessage("Начало цикла " + operationCount + " (Угол: " + currentAngle + "°)");

                monitorSteeringSystem();

                if (currentAngle != 0 && operationCount % 2 == 0) {
                    currentAngle = (int) (currentAngle * 0.8);
                    logMessage("Автоматическая коррекция угла до " + currentAngle + "°");
                    updateAngle(currentAngle);
                }

                logMessage("Цикл " + operationCount + " завершен");
                Thread.sleep(1800);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Руль остановлен");
    }

    public void turnWheels(int angle) {
        currentAngle = Math.max(-90, Math.min(90, angle));
        updateCommand("Поворот на " + angle + "°");
        logMessage("Поворот колес на угол " + currentAngle + "°");
        updateAngle(currentAngle);
    }

    public void returnToNeutral() {
        currentAngle = 0;
        updateCommand("Возврат в нейтральное положение");
        logMessage("Возврат в нейтральное положение");
        updateAngle(currentAngle);
    }

    private void monitorSteeringSystem() {
        logMessage("Проверка системы управления...");
        if (operationCount % 4 == 0) {
            logMessage("Калибровка датчиков угла - ОК");
        }
    }

    private void updateAngle(int angle) {
        SwingUtilities.invokeLater(() -> {
            currentAngleLabel.setText("Текущий угол: " + angle + "°");
            angleBar.setValue(angle);
            angleBar.setString(angle + "°");

            if (Math.abs(angle) > 60) {
                angleBar.setForeground(Color.RED);
            } else if (Math.abs(angle) > 30) {
                angleBar.setForeground(Color.ORANGE);
            } else {
                angleBar.setForeground(Color.GREEN);
            }
        });
    }

    private void updateCommand(String command) {
        SwingUtilities.invokeLater(() -> {
            commandLabel.setText("Последняя команда: " + command);
        });
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public int getCurrentAngle() {
        return currentAngle;
    }
}
