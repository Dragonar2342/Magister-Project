package ru.zolotuhin.ParrerelMethods.Lab5;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Brake extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;

    private JLabel speedLabel;
    private JLabel brakeLevelLabel;
    private JLabel commandLabel;
    private JProgressBar speedBar;
    private JProgressBar brakeBar;
    private JTextArea logArea;

    private int currentBrakeLevel = 0;
    private double currentSpeed = 0.0;

    public Brake(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;

        initializeGUI();
    }

    public void start() {
        thread = new Thread(this, "Brake-Thread");
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
        logMessage("Тормозная система запущена");

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                currentSpeed = Math.max(0, 30 - (currentBrakeLevel * 0.3));
                updateSpeed(currentSpeed);

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Тормозная система остановлена");
    }

    public void applyBrake(int intensity) {
        currentBrakeLevel = Math.min(100, Math.max(0, intensity));
        updateCommand("Торможение " + currentBrakeLevel + "%");
        logMessage("Применение торможения: " + currentBrakeLevel + "%");
        updateBrakeLevel(currentBrakeLevel);
    }

    public void emergencyStop() {
        currentBrakeLevel = 100;
        currentSpeed = 0;
        updateCommand("ЭКСТРЕННАЯ ОСТАНОВКА");
        logMessage("ЭКСТРЕННАЯ ОСТАНОВКА! Максимальное торможение!");
        updateBrakeLevel(currentBrakeLevel);
        updateSpeed(currentSpeed);
    }

    private void updateSpeed(double speed) {
        SwingUtilities.invokeLater(() -> {
            speedLabel.setText(String.format("%.1f км/ч", speed));
            speedBar.setValue((int)speed);
            speedBar.setString(String.format("%.1f км/ч", speed));
        });
    }

    private void updateBrakeLevel(int level) {
        SwingUtilities.invokeLater(() -> {
            brakeLevelLabel.setText(level + "%");
            brakeBar.setValue(level);
            brakeBar.setString(level + "%");
        });
    }

    private void updateCommand(String command) {
        SwingUtilities.invokeLater(() -> {
            commandLabel.setText(command);
        });
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Тормозная система"));

        JPanel statusPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        statusPanel.add(new JLabel("Скорость:"));
        speedLabel = new JLabel("0 км/ч");
        statusPanel.add(speedLabel);

        statusPanel.add(new JLabel("Уровень торможения:"));
        brakeLevelLabel = new JLabel("0%");
        statusPanel.add(brakeLevelLabel);

        statusPanel.add(new JLabel("Команда:"));
        commandLabel = new JLabel("Нет");
        statusPanel.add(commandLabel);

        JPanel barPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        speedBar = new JProgressBar(0, 50);
        speedBar.setValue(0);
        speedBar.setStringPainted(true);
        speedBar.setString("0 км/ч");
        speedBar.setForeground(Color.BLUE);

        brakeBar = new JProgressBar(0, 100);
        brakeBar.setValue(0);
        brakeBar.setStringPainted(true);
        brakeBar.setString("0%");
        brakeBar.setForeground(Color.RED);

        barPanel.add(new JLabel("Скорость:"));
        barPanel.add(speedBar);
        barPanel.add(new JLabel("Торможение:"));
        barPanel.add(brakeBar);

        logArea = new JTextArea(6, 20);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(statusPanel, BorderLayout.NORTH);
        add(barPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
    }
}
