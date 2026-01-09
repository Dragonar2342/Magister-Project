package ru.zolotuhin.ParrerelMethods.Lab8;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.LinkedBlockingQueue;

public class Brake extends JPanel implements Runnable {
    private Thread thread;
    private Thread consumerThread;
    private Thread heartbeatThread;
    private final AtomicBoolean systemRunning;

    private JLabel speedLabel;
    private JLabel brakeLevelLabel;
    private JLabel statusLabel;
    private JTextField incomingCommandField;
    private JProgressBar speedBar;
    private JProgressBar brakeBar;
    private JTextArea logArea;
    private JTextArea queueDisplay;

    private int currentBrakeLevel = 0;
    private double currentSpeed = 30.0;
    private boolean emergencyMode = false;

    public Brake(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;

        // Регистрируем компонент
        SocketUtils.registerComponent("Brake");

        initializeGUI();
        startConsumer();
        startHeartbeat();

        // Отправляем сообщение о запуске
        sendStatusMessage("Brake: запущен");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Тормозная система - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            Brake brake = new Brake(systemRunning);

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление тормозами"));

            JButton startButton = new JButton("Запуск системы");
            JButton stopButton = new JButton("Остановка");
            JButton emergencyButton = new JButton("Экстренная остановка");
            JButton releaseButton = new JButton("Отпустить тормоза");
            JButton testQueueButton = new JButton("Тест очереди");

            startButton.addActionListener(e -> {
                brake.start();
                brake.logMessage("Ручной запуск тормозной системы");
            });

            stopButton.addActionListener(e -> {
                brake.interrupt();
                brake.logMessage("Ручная остановка системы");
            });

            emergencyButton.addActionListener(e -> {
                brake.emergencyStop();
                brake.logMessage("Ручная экстренная остановка");
            });

            releaseButton.addActionListener(e -> {
                brake.applyBrake(0);
                brake.logMessage("Ручное отпускание тормозов");
            });

            testQueueButton.addActionListener(e -> {
                brake.testQueueProcessing();
                brake.logMessage("Тест обработки очереди");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(emergencyButton);
            controlPanel.add(releaseButton);
            controlPanel.add(testQueueButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(brake, BorderLayout.CENTER);

            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            brake.start();
        });
    }

    private void startConsumer() {
        consumerThread = new Thread(() -> {
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                // Потребляем сообщения из центральной очереди
                Message message = SocketUtils.receiveMessageViaMQ("Brake");

                if (message != null) {
                    processMessage(message);
                    updateQueueDisplay(message);
                }
            }
        }, "Brake-Consumer");
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
        }, "Brake-Heartbeat");
        heartbeatThread.start();
    }

    private void sendHeartbeat() {
        SocketUtils.sendMessageViaMQ("StatusWindow", "Brake", "HEARTBEAT", MessageType.HEARTBEAT);
    }

    private void sendStatusMessage(String content) {
        SocketUtils.sendMessageViaMQ("StatusWindow", "Brake", content, MessageType.STATUS);
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "Brake-Thread");
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
        logMessage("Тормозная система запущена (Consumer)");
        updateStatus("Активна", Color.GREEN);

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (emergencyMode) {
                    currentSpeed = 0;
                } else {
                    currentSpeed = Math.max(0, 30 - (currentBrakeLevel * 0.3));
                }

                updateSpeed(currentSpeed);
                monitorBrakeSystem();
                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Тормозная система остановлена");
        updateStatus("Неактивна", Color.RED);
    }

    private void processMessage(Message message) {
        String content = message.getContent();
        String from = message.getFrom();

        logMessage("Обработка сообщения от " + from + ": " + content);
        updateIncomingCommand(content);

        if (content.startsWith("APPLY_BRAKE:")) {
            try {
                int intensity = Integer.parseInt(content.substring(12));
                applyBrake(intensity);
            } catch (NumberFormatException e) {
                logMessage("Ошибка парсинга интенсивности торможения: " + content);
            }
        } else if ("EMERGENCY_STOP".equals(content) || "FULL_STOP".equals(content)) {
            emergencyStop();
        } else if ("RELEASE_BRAKE".equals(content) || "STOP".equals(content)) {
            applyBrake(0);
        } else if (content.startsWith("BRAKE:")) {
            try {
                int intensity = Integer.parseInt(content.substring(6));
                applyBrake(intensity);
            } catch (NumberFormatException e) {
                logMessage("Ошибка парсинга команды торможения: " + content);
            }
        }
    }

    public void applyBrake(int intensity) {
        if (emergencyMode && intensity < 100) {
            logMessage("СИСТЕМА В АВАРИЙНОМ РЕЖИМЕ - тормоза заблокированы");
            return;
        }

        int newBrakeLevel = Math.min(100, Math.max(0, intensity));

        if (newBrakeLevel != currentBrakeLevel) {
            currentBrakeLevel = newBrakeLevel;
            emergencyMode = (currentBrakeLevel == 100);

            logMessage("Применение торможения: " + currentBrakeLevel + "%");
            updateBrakeLevel(currentBrakeLevel);

            if (emergencyMode) {
                updateStatus("ЭКСТРЕННАЯ ОСТАНОВКА", Color.RED);
                currentSpeed = 0;
                updateSpeed(currentSpeed);
                logMessage("ЭКСТРЕННАЯ ОСТАНОВКА! Максимальное торможение!");
            } else {
                updateStatus("Активна", Color.GREEN);
            }
        }
    }

    public void emergencyStop() {
        logMessage("АКТИВАЦИЯ ЭКСТРЕННОГО ТОРМОЖЕНИЯ!");
        applyBrake(100);
    }

    private void testQueueProcessing() {
        // Тестовые сообщения для проверки работы Consumer
        String[] testMessages = {
                "APPLY_BRAKE:30",
                "APPLY_BRAKE:70",
                "EMERGENCY_STOP",
                "RELEASE_BRAKE",
                "BRAKE:50"
        };

        logMessage("=== ТЕСТ ОБРАБОТКИ ОЧЕРЕДИ ===");
        for (String msg : testMessages) {
            SocketUtils.sendMessageViaMQ("Test", "Brake", msg, MessageType.COMMAND);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logMessage("=== ТЕСТ ЗАВЕРШЕН ===");
    }

    private void monitorBrakeSystem() {
        if (System.currentTimeMillis() % 10000 < 500) {
            logMessage("✓ Диагностика тормозной системы - ОК");
        }

        if (System.currentTimeMillis() % 15000 < 500) {
            logMessage("✓ Проверка давления в тормозной системе - НОРМА");
        }

        if (currentBrakeLevel > 80 && !emergencyMode) {
            logMessage("ПРЕДУПРЕЖДЕНИЕ: Высокий уровень торможения " + currentBrakeLevel + "%");
            updateStatus("Интенсивное торможение", Color.ORANGE);
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

    private void updateSpeed(double speed) {
        SwingUtilities.invokeLater(() -> {
            speedLabel.setText(String.format("%.1f км/ч", speed));
            speedBar.setValue((int)speed);
            speedBar.setString(String.format("%.1f км/ч", speed));
            speedBar.setForeground(getSpeedColor(speed));
        });
    }

    private Color getSpeedColor(double speed) {
        if (speed > 20) return Color.RED;
        if (speed > 10) return Color.ORANGE;
        return Color.GREEN;
    }

    private void updateBrakeLevel(int level) {
        SwingUtilities.invokeLater(() -> {
            brakeLevelLabel.setText(level + "%");
            brakeBar.setValue(level);
            brakeBar.setString(level + "%");
            brakeBar.setForeground(getBrakeColor(level));
        });
    }

    private Color getBrakeColor(int level) {
        if (level > 80) return Color.RED;
        if (level > 50) return Color.ORANGE;
        if (level > 20) return Color.YELLOW;
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
            logArea.append("[Тормоз] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void produceMessage(String target, String content, MessageType type) {
        Message message = new Message(
                "Brake", content, type);
        boolean success = SocketUtils.sendMessageViaMQ(target, "Brake", content, type);

        if (success) {
            logMessage("Сообщение отправлено в " + target + ": " + content);
        } else {
            logMessage("ОШИБКА: очередь " + target + " переполнена!");
        }
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Тормозная система (Consumer)"));

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

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Показания системы"));

        JPanel speedPanel = new JPanel(new BorderLayout());
        speedPanel.setBorder(BorderFactory.createTitledBorder("Скорость автомобиля"));
        speedLabel = new JLabel("30.0 км/ч", JLabel.CENTER);
        speedLabel.setFont(new Font("Arial", Font.BOLD, 16));
        speedBar = new JProgressBar(0, 50);
        speedBar.setValue(30);
        speedBar.setStringPainted(true);
        speedBar.setString("30.0 км/ч");
        speedBar.setForeground(Color.GREEN);
        speedPanel.add(speedLabel, BorderLayout.NORTH);
        speedPanel.add(speedBar, BorderLayout.CENTER);

        JPanel brakePanel = new JPanel(new BorderLayout());
        brakePanel.setBorder(BorderFactory.createTitledBorder("Уровень торможения"));
        brakeLevelLabel = new JLabel("0%", JLabel.CENTER);
        brakeLevelLabel.setFont(new Font("Arial", Font.BOLD, 16));
        brakeBar = new JProgressBar(0, 100);
        brakeBar.setValue(0);
        brakeBar.setStringPainted(true);
        brakeBar.setString("0%");
        brakeBar.setForeground(Color.GREEN);
        brakePanel.add(brakeLevelLabel, BorderLayout.NORTH);
        brakePanel.add(brakeBar, BorderLayout.CENTER);

        centerPanel.add(speedPanel);
        centerPanel.add(brakePanel);

        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder("Очередь входящих сообщений (Consumer)"));
        queueDisplay = new JTextArea(8, 40);
        queueDisplay.setEditable(false);
        JScrollPane queueScrollPane = new JScrollPane(queueDisplay);
        queuePanel.add(queueScrollPane, BorderLayout.CENTER);

        JButton clearQueueButton = new JButton("Очистить очередь");
        clearQueueButton.addActionListener(e -> {
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
