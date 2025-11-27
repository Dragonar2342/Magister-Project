package ru.zolotuhin.ParrerelMethods.Lab6;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final AtomicBoolean systemRunning = new AtomicBoolean(false);
    private static final BlockingQueue<String> cameraCommands = new LinkedBlockingQueue<>();
    private static final BlockingQueue<String> autopilotCommands = new LinkedBlockingQueue<>();

    private static Autopilot autopilot;
    private static ParkingSpot parkingSpot;
    private static Camera camera;
    private static Brake brake;
    private static Steering steering;

    private static JFrame mainFrame;
    private static StatusWindow statusWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createGUI());
    }

    private static void startSystem() {
        if (!systemRunning.get()) {
            systemRunning.set(true);

            parkingSpot.generateNewSpot();

            autopilot.start();
            parkingSpot.start();
            camera.start();
            brake.start();
            steering.start();

            statusWindow.updateStatus("Автопилот", "Активен");
            statusWindow.updateStatus("Камера", "Активна");
            statusWindow.updateStatus("Парковочное место", "Активно");
            statusWindow.updateStatus("Тормоз", "Активен");
            statusWindow.updateStatus("Руль", "Активен");
            statusWindow.updateStatus("Система", "Работает");

            JOptionPane.showMessageDialog(mainFrame, "Система запущена! Найдено парковочное место: " +
                    parkingSpot.getSpotNumber() + " на расстоянии " +
                    String.format("%.1f", parkingSpot.getDistance()) + "м");
        }
    }

    private static void stopSystem() {
        if (systemRunning.get()) {
            systemRunning.set(false);
            shutdownSystem();

            statusWindow.updateStatus("Автопилот", "Неактивен");
            statusWindow.updateStatus("Камера", "Неактивна");
            statusWindow.updateStatus("Парковочное место", "Неактивно");
            statusWindow.updateStatus("Тормоз", "Неактивен");
            statusWindow.updateStatus("Руль", "Неактивен");
            statusWindow.updateStatus("Система", "Остановлена");

            JOptionPane.showMessageDialog(mainFrame, "Система остановлена!");
        }
    }

    public static void parkingCompleted() {
        systemRunning.set(false);
        shutdownSystem();

        statusWindow.updateStatus("Автопилот", "Завершен");
        statusWindow.updateStatus("Камера", "Завершена");
        statusWindow.updateStatus("Парковочное место", "Завершено");
        statusWindow.updateStatus("Тормоз", "Завершен");
        statusWindow.updateStatus("Руль", "Завершен");
        statusWindow.updateStatus("Система", "Парковка завершена");

        JOptionPane.showMessageDialog(mainFrame,
                "Парковка успешно завершена! Автомобиль припаркован на месте " +
                        parkingSpot.getSpotNumber());
    }

    private static void updateStatus() {
        statusWindow.updateStatus("Автопилот", autopilot.isAlive() ? "Активен" : "Неактивен");
        statusWindow.updateStatus("Камера", camera.isAlive() ? "Активна" : "Неактивна");
        statusWindow.updateStatus("Парковочное место", parkingSpot.isAlive() ? "Активно" : "Неактивно");
        statusWindow.updateStatus("Тормоз", brake.isAlive() ? "Активен" : "Неактивен");
        statusWindow.updateStatus("Руль", steering.isAlive() ? "Активен" : "Неактивen");
        statusWindow.updateStatus("Система", systemRunning.get() ? "Работает" : "Остановлена");
    }

    private static void shutdownSystem() {
        autopilot.interrupt();
        parkingSpot.interrupt();
        camera.interrupt();
        brake.interrupt();
        steering.interrupt();

        try {
            autopilot.join(1000);
            parkingSpot.join(1000);
            camera.join(1000);
            brake.join(1000);
            steering.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void createGUI() {
        mainFrame = new JFrame("Система автоматической парковки");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        parkingSpot = new ParkingSpot(systemRunning);
        autopilot = new Autopilot(systemRunning, autopilotCommands, parkingSpot);
        camera = new Camera(systemRunning, cameraCommands, autopilotCommands);
        brake = new Brake(systemRunning);
        steering = new Steering(systemRunning);
        statusWindow = new StatusWindow();

        autopilot.setComponents(camera, parkingSpot, brake, steering);
        camera.setAutopilot(autopilot);

        JPanel controlPanel = createControlPanel();
        mainFrame.add(controlPanel, BorderLayout.NORTH);

        JPanel windowsPanel = createWindowsPanel();
        mainFrame.add(new JScrollPane(windowsPanel), BorderLayout.CENTER);

        mainFrame.setSize(1200, 800);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Управление системой"));

        JButton startButton = new JButton("Запуск системы");
        JButton stopButton = new JButton("Остановка системы");
        JButton statusButton = new JButton("Обновить статус");

        startButton.addActionListener(e -> startSystem());
        stopButton.addActionListener(e -> stopSystem());
        statusButton.addActionListener(e -> updateStatus());

        panel.add(startButton);
        panel.add(stopButton);
        panel.add(statusButton);

        return panel;
    }

    private static JPanel createWindowsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(autopilot);
        panel.add(parkingSpot);
        panel.add(camera);
        panel.add(steering);
        panel.add(brake);
        panel.add(statusWindow);

        return panel;
    }
}
