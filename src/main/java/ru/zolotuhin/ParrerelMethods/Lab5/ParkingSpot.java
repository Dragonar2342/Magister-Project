package ru.zolotuhin.ParrerelMethods.Lab5;

import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;
    private Random random;

    private JLabel spotNumberLabel;
    private JLabel distanceLabel;
    private JLabel statusLabel;
    private JProgressBar distanceBar;
    private JTextArea infoArea;

    private String spotNumber;
    private double distance;
    private boolean isOccupied = false;

    public ParkingSpot(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;
        this.random = new Random();

        initializeGUI();
    }

    public void generateNewSpot() {
        String[] letters = {"A", "B", "C", "D"};
        spotNumber = letters[random.nextInt(letters.length)] + "-" +
                String.format("%02d", random.nextInt(50) + 1);
        distance = 20 + random.nextDouble() * 70; // 20-90 метров
        isOccupied = false;

        updateSpotInfo();
        addInfoMessage("Сгенерировано новое парковочное место: " + spotNumber);
        addInfoMessage("Расстояние до места: " + String.format("%.1f", distance) + "м");
    }

    public boolean reduceDistance(double reduction) {
        if (distance > 0) {
            distance = Math.max(0, distance - reduction);
            updateSpotInfo();

            if (distance <= 0) {
                isOccupied = true;
                statusLabel.setText("Занято");
                addInfoMessage("Парковка завершена! Место " + spotNumber + " занято");
                Main.parkingCompleted();
                return true;
            }
        }
        return false;
    }

    private void updateSpotInfo() {
        SwingUtilities.invokeLater(() -> {
            spotNumberLabel.setText(spotNumber);
            distanceLabel.setText(String.format("%.1f м", distance));
            statusLabel.setText(isOccupied ? "Занято" : "Свободно");

            int progress = (int) ((90 - distance) / 90 * 100);
            distanceBar.setValue(progress);
            distanceBar.setString(progress + "%");

            if (progress > 80) {
                distanceBar.setForeground(Color.GREEN);
            } else if (progress > 50) {
                distanceBar.setForeground(Color.YELLOW);
            } else {
                distanceBar.setForeground(Color.RED);
            }
        });
    }

    public void start() {
        thread = new Thread(this, "ParkingSpot-Thread");
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
        addInfoMessage("Парковочное место запущено");

        while (systemRunning.get() && !Thread.currentThread().isInterrupted() && distance > 0) {
            try {
                monitorStatus();
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (distance <= 0) {
            addInfoMessage("Парковочное место: работа завершена успешно");
        } else {
            addInfoMessage("Парковочное место остановлено");
        }
    }

    public void provideGuidance() {
        addInfoMessage("Передача сигналов точного позиционирования...");
        addInfoMessage("Оставшееся расстояние: " + String.format("%.1f", distance) + "м");
    }

    public void confirmParking() {
        isOccupied = true;
        statusLabel.setText("Занято");
        addInfoMessage("Парковка подтверждена - место " + spotNumber + " занято");
    }

    private void monitorStatus() {
        if (!isOccupied) {
            addInfoMessage("Мониторинг: место " + spotNumber + " свободно, расстояние: " +
                    String.format("%.1f", distance) + "м");
        }
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Парковочное место"));
        JPanel infoPanel = new JPanel(new GridLayout(4, 2, 5, 5));

        infoPanel.add(new JLabel("Номер места:"));
        spotNumberLabel = new JLabel("A-00");
        infoPanel.add(spotNumberLabel);

        infoPanel.add(new JLabel("Расстояние:"));
        distanceLabel = new JLabel("0.0 м");
        infoPanel.add(distanceLabel);

        infoPanel.add(new JLabel("Статус:"));
        statusLabel = new JLabel("Свободно");
        infoPanel.add(statusLabel);

        infoPanel.add(new JLabel("Прогресс:"));
        distanceBar = new JProgressBar(0, 100);
        distanceBar.setStringPainted(true);
        infoPanel.add(distanceBar);

        // Область дополнительной информации
        infoArea = new JTextArea(6, 20);
        infoArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(infoArea);

        add(infoPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addInfoMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            infoArea.append(message + "\n");
            infoArea.setCaretPosition(infoArea.getDocument().getLength());
        });
    }

    // Геттеры
    public String getSpotNumber() {
        return spotNumber;
    }

    public double getDistance() {
        return distance;
    }

    public boolean isOccupied() {
        return isOccupied;
    }
}