package ru.zolotuhin.ParrerelMethods.Lab8;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Camera extends JPanel implements Runnable {
    private Thread thread;
    private Thread heartbeatThread;
    private final AtomicBoolean systemRunning;
    private Map<String, JButton> obstacleButtons;
    private String currentObstacle = null;
    private Random random = new Random();

    private JTextArea logArea;
    private JLabel obstacleLabel;
    private JLabel statusLabel;
    private JTextField outgoingCommandField;
    private JProgressBar analysisProgressBar;
    private JTextArea queueDisplay;

    private int analysisCount = 0;
    private boolean parkingSpotFound = false;
    private MessageType lastMessageType;

    public Camera(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;
        this.obstacleButtons = new HashMap<>();
        this.lastMessageType = MessageType.DATA;

        SocketUtils.registerComponent("Camera");

        initializeGUI();
        startHeartbeat();

        // Отправляем сообщение о запуске
        sendStatusMessage("Camera: запущена");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Камера - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            Camera camera = new Camera(systemRunning);

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление камерой"));

            JButton startButton = new JButton("Запуск камеры");
            JButton stopButton = new JButton("Остановка камеры");
            JButton findSpotButton = new JButton("Найти место");
            JButton calibrateButton = new JButton("Калибровка");
            JButton sendTestMsgButton = new JButton("Тест сообщения");

            startButton.addActionListener(e -> {
                camera.start();
                camera.logMessage("Ручной запуск камеры");
            });

            stopButton.addActionListener(e -> {
                camera.interrupt();
                camera.logMessage("Ручная остановка камеры");
            });

            findSpotButton.addActionListener(e -> {
                camera.findParkingSpot();
                camera.logMessage("Ручной поиск парковочного места");
            });

            calibrateButton.addActionListener(e -> {
                camera.performCalibration();
                camera.logMessage("Ручная калибровка камеры");
            });

            sendTestMsgButton.addActionListener(e -> {
                camera.sendTestMessage();
                camera.logMessage("Отправка тестового сообщения");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(findSpotButton);
            controlPanel.add(calibrateButton);
            controlPanel.add(sendTestMsgButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(camera, BorderLayout.CENTER);

            frame.setSize(1000, 750);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            camera.start();
        });
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
        }, "Camera-Heartbeat");
        heartbeatThread.start();
    }

    private void sendHeartbeat() {
        SocketUtils.sendMessageViaMQ("StatusWindow", "Camera", "HEARTBEAT", MessageType.HEARTBEAT);
    }

    private void sendStatusMessage(String content) {
        produceMessage("StatusWindow", content, MessageType.STATUS);
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "Camera-Thread");
            thread.start();
            updateStatus("Активна", Color.GREEN);
        }
    }

    public void interrupt() {
        systemRunning.set(false);
        if (thread != null) {
            thread.interrupt();
        }
        updateStatus("Неактивна", Color.RED);
    }

    @Override
    public void run() {
        logMessage("Камера запущена и готова к работе");
        updateStatus("Активна", Color.GREEN);

        produceMessage("StatusWindow", "Camera: запущена", MessageType.STATUS);

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                analysisCount++;
                logMessage("=== Начало цикла анализа " + analysisCount + " ===");

                performAnalysis();

                if (analysisCount % 5 == 0 && !parkingSpotFound) {
                    findParkingSpot();
                }

                logMessage("=== Цикл анализа " + analysisCount + " завершен ===");
                Thread.sleep(3000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Камера остановлена");
        updateStatus("Неактивна", Color.RED);
    }

    public void findParkingSpot() {
        if (parkingSpotFound) {
            logMessage("Парковочное место уже найдено, ожидание завершения парковки...");
            return;
        }

        logMessage("Запуск поиска парковочного места...");
        updateAnalysisProgress(20);

        logMessage("✓ Сканирование парковочной зоны");
        updateAnalysisProgress(50);

        String[] letters = {"A", "B", "C", "D"};
        String spotNumber = letters[random.nextInt(letters.length)] + "-" +
                String.format("%02d", random.nextInt(50) + 1);
        double distance = 25 + random.nextDouble() * 65;

        logMessage("✓ Анализ доступных мест");
        updateAnalysisProgress(80);
        logMessage("✓ Проверка габаритов");
        updateAnalysisProgress(100);

        logMessage("Парковочное место найдено: " + spotNumber + " на расстоянии " +
                String.format("%.1f", distance) + "м");

        String spotMessage = "SPOT:" + spotNumber + ":" + String.format(Locale.US, "%.1f", distance);

        // Производим сообщения (только Producer)
        produceMessage("Autopilot", spotMessage, MessageType.DATA);
        produceMessage("ParkingSpot", "NEW_SPOT_DETECTED", MessageType.DATA);
        produceMessage("StatusWindow", "Camera: парковочное место найдено: " + spotNumber,
                MessageType.STATUS);

        parkingSpotFound = true;
        updateOutgoingCommand("PARKING_SPOT_FOUND: " + spotNumber);
    }

    public void performCalibration() {
        logMessage("Начинаю калибровку камеры...");
        updateStatus("Калибровка", Color.ORANGE);

        updateAnalysisProgress(20);
        logMessage("✓ Калибровка цветопередачи");
        updateAnalysisProgress(40);
        logMessage("✓ Настройка фокуса");
        updateAnalysisProgress(60);
        logMessage("✓ Калибровка углов обзора");
        updateAnalysisProgress(80);
        logMessage("✓ Тестирование системы стабилизации");
        updateAnalysisProgress(100);

        logMessage("Калибровка камеры завершена успешно");
        updateStatus("Активна", Color.GREEN);

        produceMessage("Autopilot", "CALIBRATION_COMPLETE", MessageType.STATUS);
        produceMessage("StatusWindow", "Camera: калибровка завершена",
                MessageType.STATUS);
    }

    private void performAnalysis() {
        logMessage("Выполняю стандартный анализ...");
        updateAnalysisProgress(25);
        logMessage("✓ Сканирование пространства на наличие парковочных мест");
        updateAnalysisProgress(50);
        logMessage("✓ Обнаружение препятствий в реальном времени");
        updateAnalysisProgress(75);

        // Случайное обнаружение препятствий (для демонстрации)
        if (random.nextInt(10) > 7) {
            String[] directions = {"СЛЕВА", "СПРАВА", "СПЕРЕДИ", "СЗАДИ"};
            String obstacle = directions[random.nextInt(directions.length)];
            produceMessage("Autopilot", "OBSTACLE_" + obstacle, MessageType.EMERGENCY);
            produceMessage("StatusWindow", "Camera: обнаружено препятствие " + obstacle,
                    MessageType.EMERGENCY);
            logMessage("Обнаружено препятствие: " + obstacle);
        }

        updateAnalysisProgress(100);
        logMessage("Стандартный анализ завершен");
    }

    private void handleObstacle(String direction) {
        if (currentObstacle != null && currentObstacle.equals(direction)) {
            clearObstacle();
        } else {
            if (currentObstacle != null) {
                resetButton(currentObstacle);
            }
            setObstacle(direction);
        }
    }

    private void setObstacle(String direction) {
        currentObstacle = direction;
        updateObstacleLabel(direction);
        highlightButton(direction);
        logMessage("Обнаружено препятствие: " + direction);

        produceMessage("Autopilot", "OBSTACLE_" + direction, MessageType.EMERGENCY);
        produceMessage("StatusWindow", "Camera: препятствие " + direction,
                MessageType.EMERGENCY);
    }

    private void clearObstacle() {
        if (currentObstacle != null) {
            resetButton(currentObstacle);
            logMessage("Препятствие " + currentObstacle + " устранено");
        }
        currentObstacle = null;
        updateObstacleLabel(null);

        produceMessage("Autopilot", "OBSTACLE_CLEAR", MessageType.DATA);
        produceMessage("StatusWindow", "Camera: препятствий нет", MessageType.STATUS);
    }

    private void produceMessage(String target, String content, MessageType type) {
        boolean success = SocketUtils.sendMessageViaMQ(target, "Camera", content, type);

        if (type != MessageType.HEARTBEAT) {
            logMessage("Сообщение отправлено в " + target + ": " + content);
        }
    }

    private void sendTestMessage() {
        String[] testMessages = {
                "Тестовое сообщение 1",
                "ANALYZE_ENVIRONMENT",
                "OBSTACLE_FRONT",
                "CALIBRATION_NEEDED"
        };

        String target = "Autopilot";
        String message = testMessages[random.nextInt(testMessages.length)];
        MessageType type = MessageType.COMMAND;

        produceMessage(target, message, type);
    }

    private void updateQueueDisplay(Message message, String target) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date(message.getTimestamp()));
            String line = String.format("[%s] -> %-15s: %s",
                    timestamp, target, message.getContent());
            queueDisplay.append(line + "\n");
            queueDisplay.setCaretPosition(queueDisplay.getDocument().getLength());
        });
    }

    private void updateObstacleLabel(String obstacle) {
        SwingUtilities.invokeLater(() -> {
            if (obstacle == null) {
                obstacleLabel.setText("Препятствий нет");
                obstacleLabel.setBackground(Color.GREEN);
                obstacleLabel.setForeground(Color.BLACK);
            } else {
                obstacleLabel.setText("ПРЕПЯТСТВИЕ: " + obstacle);
                obstacleLabel.setBackground(Color.RED);
                obstacleLabel.setForeground(Color.WHITE);
            }
        });
    }

    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setBackground(color);
            statusLabel.setForeground(color == Color.RED ? Color.WHITE : Color.BLACK);
        });
    }

    private void updateOutgoingCommand(String command) {
        SwingUtilities.invokeLater(() -> {
            outgoingCommandField.setText(command);
        });
    }

    private void updateAnalysisProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            analysisProgressBar.setValue(progress);
            analysisProgressBar.setString(progress + "%");
            analysisProgressBar.setForeground(getProgressColor(progress));
        });

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Color getProgressColor(int progress) {
        if (progress < 30) return Color.RED;
        if (progress < 70) return Color.ORANGE;
        return Color.GREEN;
    }

    private void highlightButton(String direction) {
        JButton button = obstacleButtons.get(direction);
        if (button != null) {
            button.setBackground(Color.RED);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Arial", Font.BOLD, 12));
        }
    }

    private void resetButton(String direction) {
        JButton button = obstacleButtons.get(direction);
        if (button != null) {
            button.setBackground(Color.LIGHT_GRAY);
            button.setForeground(Color.BLACK);
            button.setFont(new Font("Arial", Font.PLAIN, 12));
        }
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[Камера] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private class ObstacleButtonListener implements ActionListener {
        private String direction;

        public ObstacleButtonListener(String direction) {
            this.direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            handleObstacle(direction);
        }
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Камера (Producer)"));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 5));

        JPanel obstaclePanel = new JPanel(new BorderLayout());
        obstacleLabel = new JLabel("Препятствий нет", JLabel.CENTER);
        obstacleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        obstacleLabel.setOpaque(true);
        obstacleLabel.setBackground(Color.GREEN);
        obstaclePanel.add(obstacleLabel, BorderLayout.CENTER);
        obstaclePanel.setBorder(BorderFactory.createTitledBorder("Обнаружение препятствий"));

        JPanel cameraStatusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Неактивна", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.RED);
        statusLabel.setForeground(Color.WHITE);
        cameraStatusPanel.add(statusLabel, BorderLayout.CENTER);
        cameraStatusPanel.setBorder(BorderFactory.createTitledBorder("Статус камеры"));

        topPanel.add(obstaclePanel);
        topPanel.add(cameraStatusPanel);

        JPanel centerPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = createButtonPanel();
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Ручное обнаружение препятствий"));
        centerPanel.add(buttonPanel, BorderLayout.NORTH);

        JPanel progressPanel = new JPanel(new BorderLayout());
        analysisProgressBar = new JProgressBar(0, 100);
        analysisProgressBar.setValue(0);
        analysisProgressBar.setStringPainted(true);
        analysisProgressBar.setString("Ожидание анализа");
        analysisProgressBar.setForeground(Color.BLUE);
        progressPanel.add(new JLabel("Прогресс анализа:"), BorderLayout.NORTH);
        progressPanel.add(analysisProgressBar, BorderLayout.CENTER);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        centerPanel.add(progressPanel, BorderLayout.CENTER);

        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setBorder(BorderFactory.createTitledBorder("Последнее отправленное сообщение"));
        outgoingCommandField = new JTextField();
        outgoingCommandField.setEditable(false);
        commandPanel.add(outgoingCommandField, BorderLayout.CENTER);

        centerPanel.add(commandPanel, BorderLayout.SOUTH);

        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder("Очередь исходящих сообщений (Producer)"));
        queueDisplay = new JTextArea(10, 40);
        queueDisplay.setEditable(false);
        JScrollPane queueScrollPane = new JScrollPane(queueDisplay);
        queuePanel.add(queueScrollPane, BorderLayout.CENTER);

        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Журнал работы камеры"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, queuePanel, logScrollPane);
        splitPane.setResizeWeight(0.5);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(splitPane, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton leftButton = createObstacleButton("Слева");
        JButton rightButton = createObstacleButton("Справа");
        JButton frontButton = createObstacleButton("Спереди");
        JButton rearButton = createObstacleButton("Сзади");

        obstacleButtons.put("СЛЕВА", leftButton);
        obstacleButtons.put("СПРАВА", rightButton);
        obstacleButtons.put("СПЕРЕДИ", frontButton);
        obstacleButtons.put("СЗАДИ", rearButton);

        panel.add(leftButton);
        panel.add(rightButton);
        panel.add(frontButton);
        panel.add(rearButton);

        return panel;
    }

    private JButton createObstacleButton(String direction) {
        JButton button = new JButton(direction);
        button.setBackground(Color.LIGHT_GRAY);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.addActionListener(new ObstacleButtonListener(direction.toUpperCase()));
        return button;
    }
}
