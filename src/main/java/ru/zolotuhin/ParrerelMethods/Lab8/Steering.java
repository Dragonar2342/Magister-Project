package ru.zolotuhin.ParrerelMethods.Lab8;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Steering extends JPanel implements Runnable {
    private Thread thread;
    private Thread consumerThread;
    private Thread heartbeatThread;
    private final AtomicBoolean systemRunning;

    private JLabel currentAngleLabel;
    private JLabel statusLabel;
    private JTextField incomingCommandField;
    private JProgressBar angleBar;
    private JTextArea logArea;
    private JTextArea queueDisplay;

    private int operationCount = 0;
    private int currentAngle = 0;
    private boolean isCalibrated = false;

    public Steering(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;

        // Регистрируем компонент через SocketUtils
        SocketUtils.registerComponent("Steering");

        initializeGUI();
        startConsumer();
        startHeartbeat();

        // Отправляем сообщение о запуске
        sendStatusMessage("Steering: запущен");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Рулевое управление - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            Steering steering = new Steering(systemRunning);

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление рулем"));

            JButton startButton = new JButton("Запуск системы");
            JButton stopButton = new JButton("Остановка");
            JButton calibrateButton = new JButton("Калибровка");
            JButton neutralButton = new JButton("В нейтраль");
            JButton testQueueButton = new JButton("Тест очереди");

            startButton.addActionListener(e -> {
                steering.start();
                steering.logMessage("Ручной запуск системы рулевого управления");
            });

            stopButton.addActionListener(e -> {
                steering.interrupt();
                steering.logMessage("Ручная остановка системы");
            });

            calibrateButton.addActionListener(e -> {
                steering.performCalibration();
                steering.logMessage("Ручная калибровка руля");
            });

            neutralButton.addActionListener(e -> {
                steering.returnToNeutral();
                steering.logMessage("Ручной возврат в нейтральное положение");
            });

            testQueueButton.addActionListener(e -> {
                steering.testQueueProcessing();
                steering.logMessage("Тест обработки очереди");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(calibrateButton);
            controlPanel.add(neutralButton);
            controlPanel.add(testQueueButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(steering, BorderLayout.CENTER);

            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            steering.start();
        });
    }

    private void startConsumer() {
        consumerThread = new Thread(() -> {
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                // Потребляем сообщения из центральной очереди через SocketUtils
                Message message = SocketUtils.receiveMessageViaMQ("Steering");

                if (message != null) {
                    processMessage(message);
                    updateQueueDisplay(message);
                }
            }
        }, "Steering-Consumer");
        consumerThread.start();
    }

    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    sendHeartbeat();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Steering-Heartbeat");
        heartbeatThread.start();
    }

    private void sendHeartbeat() {
        // Используем SocketUtils вместо MessageQueueManager
        SocketUtils.sendMessageViaMQ("StatusWindow", "Steering", "HEARTBEAT", MessageType.HEARTBEAT);
    }

    private void sendStatusMessage(String content) {
        produceMessage("StatusWindow", content, MessageType.STATUS);
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "Steering-Thread");
            thread.start();
            updateStatus("Активна", Color.GREEN);
        }
    }

    public void interrupt() {
        systemRunning.set(false);
        if (thread != null) {
            thread.interrupt();
        }
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        updateStatus("Неактивна", Color.RED);
    }

    @Override
    public void run() {
        logMessage("Система рулевого управления запущена (Consumer)");
        updateStatus("Активна", Color.GREEN);

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                operationCount++;
                logMessage("Цикл мониторинга " + operationCount + " (Угол: " + currentAngle + "°)");

                monitorSteeringSystem();

                if (currentAngle != 0 && operationCount % 3 == 0) {
                    int correction = (int)(currentAngle * 0.3);
                    currentAngle = currentAngle > 0 ?
                            Math.max(0, currentAngle - correction) :
                            Math.min(0, currentAngle - correction);
                    logMessage("Автоматическая коррекция угла до " + currentAngle + "°");
                    updateAngle(currentAngle);
                }

                Thread.sleep(2000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Система рулевого управления остановлена");
        updateStatus("Неактивна", Color.RED);
    }

    private void processMessage(Message message) {
        String content = message.getContent();
        String from = message.getFrom();

        logMessage("Обработка сообщения от " + from + ": " + content);
        updateIncomingCommand(content);

        if (content.startsWith("TURN_WHEELS:")) {
            try {
                int angle = Integer.parseInt(content.substring(12));
                turnWheels(angle);
            } catch (NumberFormatException e) {
                logMessage("Ошибка парсинга угла поворота: " + content);
            }
        } else if ("RETURN_NEUTRAL".equals(content) || "NEUTRAL".equals(content)) {
            returnToNeutral();
        } else if ("CALIBRATE".equals(content) || "CALIBRATION".equals(content)) {
            performCalibration();
        } else if ("EMERGENCY_STOP".equals(content)) {
            handleEmergencyStop();
        } else if (content.startsWith("ANGLE:")) {
            try {
                int angle = Integer.parseInt(content.substring(6));
                turnWheels(angle);
            } catch (NumberFormatException e) {
                logMessage("Ошибка парсинга команды угла: " + content);
            }
        }
    }

    public void turnWheels(int angle) {
        if (!isCalibrated) {
            logMessage("ВНИМАНИЕ: Система не откалибрована! Выполните калибровку.");
            updateStatus("Требуется калибровка", Color.ORANGE);
            return;
        }

        int newAngle = Math.max(-90, Math.min(90, angle));

        if (newAngle != currentAngle) {
            currentAngle = newAngle;
            logMessage("Поворот колес на угол " + currentAngle + "°");
            updateAngle(currentAngle);
            simulateSteeringMovement();
        }
    }

    public void returnToNeutral() {
        if (currentAngle != 0) {
            logMessage("Возврат колес в нейтральное положение");
            currentAngle = 0;
            updateAngle(currentAngle);
            simulateSteeringMovement();
        }
    }

    private void handleEmergencyStop() {
        logMessage("ЭКСТРЕННАЯ ОСТАНОВКА - фиксация текущего угла");
        updateStatus("ЭКСТРЕННАЯ ОСТАНОВКА", Color.RED);
    }

    private void performCalibration() {
        logMessage("Начало калибровки системы рулевого управления...");
        updateStatus("Калибровка", Color.ORANGE);

        simulateCalibrationProcess();

        isCalibrated = true;
        logMessage("Калибровка системы рулевого управления завершена успешно");
        updateStatus("Активна", Color.GREEN);

        // Отправляем сообщение о завершении калибровки
        produceMessage("Autopilot", "STEERING_CALIBRATED", MessageType.STATUS);
    }

    private void testQueueProcessing() {
        // Тестовые сообщения для проверки работы Consumer
        String[] testMessages = {
                "TURN_WHEELS:45",
                "TURN_WHEELS:-30",
                "RETURN_NEUTRAL",
                "CALIBRATE",
                "EMERGENCY_STOP"
        };

        logMessage("=== ТЕСТ ОБРАБОТКИ ОЧЕРЕДИ ===");
        for (String msg : testMessages) {
            // Отправляем через SocketUtils вместо MessageQueueManager
            SocketUtils.sendMessageViaMQ("Steering", "Test", msg, MessageType.COMMAND);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logMessage("=== ТЕСТ ЗАВЕРШЕН ===");
    }

    private void monitorSteeringSystem() {
        if (operationCount % 5 == 0) {
            logMessage("✓ Проверка датчиков угла - ОК");
        }
        if (operationCount % 7 == 0) {
            logMessage("✓ Диагностика гидроусилителя - ОК");
        }

        if (Math.abs(currentAngle) > 75) {
            logMessage("ПРЕДУПРЕЖДЕНИЕ: Критический угол поворота " + currentAngle + "°");
            updateStatus("Критический угол", Color.ORANGE);
        }
    }

    private void simulateSteeringMovement() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateCalibrationProcess() {
        for (int i = 1; i <= 5; i++) {
            try {
                Thread.sleep(400);
                logMessage("✓ Шаг калибровки " + i + "/5 завершен");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void updateQueueDisplay(Message message) {
        SwingUtilities.invokeLater(() -> {
            String line = String.format("[%tH:%tM:%tS] От: %-15s -> %s",
                    message.getTimestamp(), message.getTimestamp(), message.getTimestamp(),
                    message.getFrom(), message.getContent());
            queueDisplay.append(line + "\n");
            queueDisplay.setCaretPosition(queueDisplay.getDocument().getLength());
        });
    }

    private void updateAngle(int angle) {
        SwingUtilities.invokeLater(() -> {
            currentAngleLabel.setText("Текущий угол: " + angle + "°");
            angleBar.setValue(angle);
            angleBar.setString(angle + "°");
            angleBar.setForeground(getAngleColor(angle));
        });
    }

    private Color getAngleColor(int angle) {
        if (Math.abs(angle) > 60) return Color.RED;
        if (Math.abs(angle) > 30) return Color.ORANGE;
        return Color.GREEN;
    }

    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setBackground(color);
            statusLabel.setForeground(color == Color.RED || color == Color.ORANGE ?
                    Color.WHITE : Color.BLACK);
        });
    }

    private void updateIncomingCommand(String command) {
        SwingUtilities.invokeLater(() -> {
            incomingCommandField.setText(command);
        });
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[Руль] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void produceMessage(String target, String content, MessageType type) {
        // Используем SocketUtils вместо MessageQueueManager
        boolean success = SocketUtils.sendMessageViaMQ(target, "Steering", content, type);

        if (success) {
            logMessage("Сообщение отправлено в " + target + ": " + content);
        } else {
            logMessage("ОШИБКА: очередь " + target + " переполнена или нет связи с сервером!");
        }
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Рулевое управление (Consumer)"));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 5));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Неактивна", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.RED);
        statusLabel.setForeground(Color.WHITE);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.setBorder(BorderFactory.createTitledBorder("Статус системы"));

        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setBorder(BorderFactory.createTitledBorder("Последняя команда"));
        incomingCommandField = new JTextField("Ожидание команд...");
        incomingCommandField.setEditable(false);
        commandPanel.add(incomingCommandField, BorderLayout.CENTER);

        topPanel.add(statusPanel);
        topPanel.add(commandPanel);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Индикация угла поворота"));

        currentAngleLabel = new JLabel("Текущий угол: 0°", JLabel.CENTER);
        currentAngleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        currentAngleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        angleBar = new JProgressBar(-90, 90);
        angleBar.setValue(0);
        angleBar.setStringPainted(true);
        angleBar.setString("0°");
        angleBar.setForeground(Color.GREEN);

        centerPanel.add(currentAngleLabel, BorderLayout.NORTH);
        centerPanel.add(angleBar, BorderLayout.CENTER);

        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder("Очередь входящих сообщений (Consumer)"));
        queueDisplay = new JTextArea(10, 40);
        queueDisplay.setEditable(false);
        JScrollPane queueScrollPane = new JScrollPane(queueDisplay);
        queuePanel.add(queueScrollPane, BorderLayout.CENTER);

        JButton clearQueueButton = new JButton("Очистить очередь");
        clearQueueButton.addActionListener(e -> {
            // Используем SocketUtils вместо MessageQueueManager
            SocketUtils.clearQueue("Steering");
            queueDisplay.setText("");
            logMessage("Очередь очищена");
        });
        queuePanel.add(clearQueueButton, BorderLayout.SOUTH);

        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Журнал работы"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, queuePanel, logScrollPane);
        splitPane.setResizeWeight(0.5);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(splitPane, BorderLayout.SOUTH);
    }
}