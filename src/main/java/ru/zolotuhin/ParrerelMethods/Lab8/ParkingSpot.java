package ru.zolotuhin.ParrerelMethods.Lab8;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot extends JPanel implements Runnable {
    private Thread thread;
    private Thread consumerThread;
    private Thread heartbeatThread;
    private final AtomicBoolean systemRunning;
    private Random random;

    private JLabel spotNumberLabel;
    private JLabel distanceLabel;
    private JLabel statusLabel;
    private JLabel guidanceStatusLabel;
    private JProgressBar distanceBar;
    private JProgressBar guidanceProgressBar;
    private JTextArea logArea;
    private JTextArea incomingQueueDisplay;
    private JTextArea outgoingQueueDisplay;
    private JLabel queueStatsLabel;
    private JButton generateSpotButton;
    private JButton confirmParkingButton;

    private String spotNumber;
    private double distance;
    private boolean isOccupied = false;
    private boolean guidanceActive = false;
    private boolean spotGenerated = false;
    private SimpleDateFormat timeFormat;
    private int messagesSent = 0;
    private int messagesReceived = 0;

    private static final double MIN_DISTANCE = 20.0;
    private static final double MAX_DISTANCE = 90.0;
    private static final double GUIDANCE_RANGE = 15.0;

    public ParkingSpot(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;
        this.random = new Random();
        this.timeFormat = new SimpleDateFormat("HH:mm:ss");

        // Регистрируем компонент через SocketUtils
        SocketUtils.registerComponent("ParkingSpot");

        initializeGUI();
        startConsumer();
        startHeartbeat();

        // Отправляем сообщение о запуске
        sendStatusMessage("ParkingSpot: запущен");

        SocketUtils.startServer(5003, this::handleSocketMessage);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Парковочное место - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            ParkingSpot parkingSpot = new ParkingSpot(systemRunning);

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление парковочным местом (Hybrid)"));

            JButton startButton = new JButton("Запуск системы");
            JButton stopButton = new JButton("Остановка системы");
            JButton generateButton = new JButton("Сгенерировать место");
            JButton resetButton = new JButton("Сбросить парковку");
            JButton testGuidanceButton = new JButton("Тест наведения");
            JButton testProducerButton = new JButton("Тест Producer");
            JButton testConsumerButton = new JButton("Тест Consumer");

            startButton.addActionListener(e -> {
                parkingSpot.start();
                parkingSpot.logMessage("Ручной запуск системы парковочного места");
            });

            stopButton.addActionListener(e -> {
                parkingSpot.interrupt();
                parkingSpot.logMessage("Ручная остановка системы");
            });

            generateButton.addActionListener(e -> {
                parkingSpot.generateNewSpot();
                parkingSpot.logMessage("Ручная генерация нового парковочного места");
            });

            resetButton.addActionListener(e -> {
                parkingSpot.resetParking();
                parkingSpot.logMessage("Ручной сброс состояния парковки");
            });

            testGuidanceButton.addActionListener(e -> {
                parkingSpot.testGuidanceSystem();
                parkingSpot.logMessage("Тестирование системы наведения");
            });

            testProducerButton.addActionListener(e -> {
                parkingSpot.testProducer();
                parkingSpot.logMessage("Тестирование Producer");
            });

            testConsumerButton.addActionListener(e -> {
                parkingSpot.testConsumer();
                parkingSpot.logMessage("Тестирование Consumer");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(generateButton);
            controlPanel.add(resetButton);
            controlPanel.add(testGuidanceButton);
            controlPanel.add(testProducerButton);
            controlPanel.add(testConsumerButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(parkingSpot, BorderLayout.CENTER);

            frame.setSize(1300, 850);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            parkingSpot.start();
            parkingSpot.generateNewSpot();
        });
    }

    private void startConsumer() {
        consumerThread = new Thread(() -> {
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                // Получаем сообщения через SocketUtils
                Message message = SocketUtils.receiveMessageViaMQ("ParkingSpot");

                if (message != null) {
                    messagesReceived++;
                    processMessage(message);
                    updateQueueStats();
                    updateIncomingQueueDisplay(message);
                }
            }
        }, "ParkingSpot-Consumer");
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
        }, "ParkingSpot-Heartbeat");
        heartbeatThread.start();
    }

    private void sendHeartbeat() {
        SocketUtils.sendMessageViaMQ("StatusWindow", "ParkingSpot", "HEARTBEAT", MessageType.HEARTBEAT);
    }

    private void sendStatusMessage(String content) {
        SocketUtils.sendMessageViaMQ("StatusWindow", "ParkingSpot", content, MessageType.STATUS);
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "ParkingSpot-Thread");
            thread.start();
            updateGuidanceStatus("Система активна", Color.GREEN);
            logMessage("Система парковочного места запущена");
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
        updateGuidanceStatus("Система остановлена", Color.RED);
        logMessage("Система парковочного места остановлена");
    }

    @Override
    public void run() {
        logMessage("Модуль парковочного места запущен (Hybrid)");

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                monitorParkingSpot();
                updateGuidanceData();
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Модуль парковочного места остановлен");
        updateGuidanceStatus("Система остановлена", Color.RED);
    }

    private void handleSocketMessage(String rawMessage) {
        // Помещаем сообщение в свою очередь через SocketUtils
        SocketUtils.sendMessageViaMQ("ParkingSpot", "Socket", rawMessage, MessageType.COMMAND);
        logMessage("Сообщение из сокета помещено в очередь: " + rawMessage);
    }

    private void processMessage(Message message) {
        String content = message.getContent();
        String from = message.getFrom();

        logMessage("Обработка сообщения от " + from + ": " + content);

        try {
            if (content.startsWith("REDUCE_DISTANCE:")) {
                String distanceStr = content.substring(16).replace(',', '.');
                double reduction = Double.parseDouble(distanceStr);
                reduceDistance(reduction);

            } else if ("PROVIDE_GUIDANCE".equals(content)) {
                provideGuidance();

            } else if ("CONFIRM_PARKING".equals(content)) {
                confirmParking();

            } else if ("GET_SPOT_INFO".equals(content)) {
                sendSpotInfoToAutopilot();

            } else if ("RESET_SPOT".equals(content)) {
                resetParking();

            } else if ("EMERGENCY_STOP".equals(content)) {
                handleEmergencyStop();

            } else if ("STOP".equals(content)) {
                logMessage("Получена команда STOP");
                updateGuidanceStatus("Остановка", Color.ORANGE);

            } else if ("NEW_SPOT_DETECTED".equals(content)) {
                logMessage("Камера обнаружила новое парковочное место");
                if (!spotGenerated) {
                    generateNewSpot();
                }
            }
        } catch (NumberFormatException e) {
            logMessage("ОШИБКА: Неверный формат расстояния в команде: " + content);
        } catch (Exception e) {
            logMessage("ОШИБКА обработки команды: " + e.getMessage());
        }
    }

    private void produceMessage(String target, String content, MessageType type) {
        // Используем SocketUtils вместо MessageQueueManager
        boolean success = SocketUtils.sendMessageViaMQ(target, "ParkingSpot", content, type);

        if (success) {
            messagesSent++;
            updateQueueStats();
            Message message = new Message("ParkingSpot", content, type);
            updateOutgoingQueueDisplay(message, target);
            logMessage("Сообщение отправлено в " + target + ": " + content);
        } else {
            logMessage("ОШИБКА: очередь " + target + " переполнена или нет связи с сервером!");
        }
    }

    public void generateNewSpot() {
        if (spotGenerated && !isOccupied) {
            logMessage("Парковочное место уже активно: " + spotNumber);
            return;
        }

        String[] letters = {"A", "B", "C", "D", "E", "F"};
        String[] types = {"Стандарт", "Компакт", "Инвалиды", "Семейный"};

        spotNumber = letters[random.nextInt(letters.length)] + "-" +
                String.format("%02d", random.nextInt(50) + 1);
        distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        isOccupied = false;
        spotGenerated = true;

        String spotType = types[random.nextInt(types.length)];
        double spotWidth = 2.0 + random.nextDouble() * 0.5;
        double spotLength = 4.5 + random.nextDouble() * 0.5;

        updateSpotInfo();

        logMessage("=== НОВОЕ ПАРКОВОЧНОЕ МЕСТО ===");
        logMessage("Номер: " + spotNumber);
        logMessage("Тип: " + spotType);
        logMessage("Расстояние: " + String.format(Locale.US, "%.1f", distance) + "м");
        logMessage("Размеры: " + String.format(Locale.US, "%.1f", spotWidth) + "м x " +
                String.format(Locale.US, "%.1f", spotLength) + "м");
        logMessage("Координаты: X=" + random.nextInt(100) + ", Y=" + random.nextInt(100));
        logMessage("================================");

        sendSpotInfoToAutopilot();

        guidanceActive = true;
        updateGuidanceStatus("Наведение активно", Color.BLUE);
    }

    private void sendSpotInfoToAutopilot() {
        if (spotNumber != null) {
            String spotMessage = "SPOT:" + spotNumber + ":" + String.format(Locale.US, "%.1f", distance);
            produceMessage("Autopilot", spotMessage, MessageType.DATA);
            produceMessage("StatusWindow", "ParkingSpot: место " + spotNumber + " сгенерировано",
                    MessageType.STATUS);
        }
    }

    public boolean reduceDistance(double reduction) {
        if (distance > 0 && !isOccupied) {
            double oldDistance = distance;
            distance = Math.max(0, distance - reduction);
            updateSpotInfo();

            logMessage("Уменьшение расстояния: " + String.format(Locale.US, "%.1f", oldDistance) +
                    "м -> " + String.format(Locale.US, "%.1f", distance) + "м");

            if (distance <= GUIDANCE_RANGE && !guidanceActive) {
                guidanceActive = true;
                updateGuidanceStatus("Точное наведение", Color.BLUE);
                logMessage("Активировано точное наведение (расстояние < " + GUIDANCE_RANGE + "м)");
            }

            if (distance <= 0) {
                completeParking();
                return true;
            }
        }
        return false;
    }

    private void completeParking() {
        isOccupied = true;
        guidanceActive = false;

        updateSpotInfo();
        updateGuidanceStatus("Парковка завершена", Color.GREEN);

        logMessage("★★★★★ ПАРКОВКА ЗАВЕРШЕНА ★★★★★");
        logMessage("Место " + spotNumber + " успешно занято");
        logMessage("Автомобиль припаркован идеально");

        produceMessage("Autopilot", "PARKING_COMPLETED:" + spotNumber, MessageType.DATA);
        produceMessage("StatusWindow", "ParkingSpot: парковка завершена на месте " + spotNumber,
                MessageType.STATUS);

        SwingUtilities.invokeLater(() -> {
            confirmParkingButton.setEnabled(true);
            generateSpotButton.setEnabled(false);
        });
    }

    public void provideGuidance() {
        if (!guidanceActive || isOccupied) {
            logMessage("Система наведения не активна или место занято");
            return;
        }

        logMessage("Запуск системы точного наведения...");
        updateGuidanceStatus("Корректировка траектории", Color.ORANGE);

        double recommendedSpeed = Math.min(10.0, distance / 3.0);
        double steeringCorrection = calculateSteeringCorrection();
        String approachDirection = getApproachDirection();

        logMessage("✓ Рекомендуемая скорость: " + String.format(Locale.US, "%.1f", recommendedSpeed) + " км/ч");
        logMessage("✓ Коррекция руля: " + String.format(Locale.US, "%.1f", steeringCorrection) + "°");
        logMessage("✓ Направление подъезда: " + approachDirection);
        logMessage("✓ Оставшееся расстояние: " + String.format(Locale.US, "%.1f", distance) + "м");

        simulateGuidanceProgress();

        updateGuidanceStatus("Наведение активно", Color.BLUE);
        logMessage("Система наведения готова к работе");

        String guidanceMessage = "GUIDANCE_DATA:" +
                String.format(Locale.US, "%.1f", recommendedSpeed) + ":" +
                String.format(Locale.US, "%.1f", steeringCorrection) + ":" +
                approachDirection;
        produceMessage("Autopilot", guidanceMessage, MessageType.DATA);
    }

    public void confirmParking() {
        if (isOccupied) {
            logMessage("Подтверждение парковки на месте " + spotNumber);

            produceMessage("Autopilot", "PARKING_CONFIRMED:" + spotNumber, MessageType.DATA);
            produceMessage("StatusWindow", "ParkingSpot: парковка подтверждена",
                    MessageType.STATUS);

            logMessage("✓ Парковка подтверждена оператором");
            updateGuidanceStatus("Парковка подтверждена", Color.GREEN);

            SwingUtilities.invokeLater(() -> {
                confirmParkingButton.setEnabled(false);
            });
        }
    }

    public void resetParking() {
        isOccupied = false;
        guidanceActive = false;
        spotGenerated = false;
        distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);

        updateSpotInfo();
        updateGuidanceStatus("Система готова", Color.GREEN);

        logMessage("Сброс состояния парковки");
        logMessage("Новое расстояние: " + String.format(Locale.US, "%.1f", distance) + "м");

        produceMessage("StatusWindow", "ParkingSpot: сброс состояния",
                MessageType.STATUS);

        SwingUtilities.invokeLater(() -> {
            confirmParkingButton.setEnabled(false);
            generateSpotButton.setEnabled(true);
        });
    }

    private void handleEmergencyStop() {
        logMessage("ЭКСТРЕННАЯ ОСТАНОВКА - приостановка наведения");
        updateGuidanceStatus("ЭКСТРЕННАЯ ОСТАНОВКА", Color.RED);
        guidanceActive = false;

        produceMessage("StatusWindow", "ParkingSpot: экстренная остановка",
                MessageType.EMERGENCY);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                if (systemRunning.get()) {
                    guidanceActive = true;
                    updateGuidanceStatus("Наведение восстановлено", Color.BLUE);
                    logMessage("Система наведения восстановлена после аварийной остановки");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void testGuidanceSystem() {
        logMessage("=== ТЕСТ СИСТЕМЫ НАВЕДЕНИЯ ===");

        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    int finalI = i;
                    SwingUtilities.invokeLater(() -> {
                        guidanceProgressBar.setValue(finalI);
                    });
                    Thread.sleep(100);
                }

                logMessage("✓ Тест системы наведения пройден успешно");
                logMessage("✓ Все датчики работают нормально");
                logMessage("✓ Система позиционирования активна");
                logMessage("=== ТЕСТ ЗАВЕРШЕН ===");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void testProducer() {
        logMessage("=== ТЕСТ PRODUCER ===");

        String[] testTargets = {"Autopilot", "StatusWindow"};
        String[] testMessages = {
                "SPOT:TEST-01:25.5",
                "GUIDANCE_DATA:5.0:2.5:Прямой",
                "PARKING_COMPLETED:TEST-01",
                "ParkingSpot: тестовое сообщение"
        };

        for (int i = 0; i < Math.min(testTargets.length, testMessages.length); i++) {
            produceMessage(testTargets[i], testMessages[i], MessageType.DATA);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("=== ТЕСТ PRODUCER ЗАВЕРШЕН ===");
    }

    private void testConsumer() {
        logMessage("=== ТЕСТ CONSUMER ===");

        for (int i = 1; i <= 5; i++) {
            // Отправляем тестовые сообщения сами себе через SocketUtils
            SocketUtils.sendMessageViaMQ("ParkingSpot", "ParkingSpot-Test",
                    "Тестовое сообщение " + i, MessageType.COMMAND);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("=== ТЕСТ CONSUMER ЗАВЕРШЕН ===");
    }

    private void updateQueueStats() {
        SwingUtilities.invokeLater(() -> {
            int queueSize = SocketUtils.getQueueSize("ParkingSpot");
            queueStatsLabel.setText(String.format("Отправлено: %d | Получено: %d | В очереди: %d",
                    messagesSent, messagesReceived, queueSize));
        });
    }

    private void updateIncomingQueueDisplay(Message message) {
        SwingUtilities.invokeLater(() -> {
            String line = String.format("[%tH:%tM:%tS] От: %-15s -> %s",
                    message.getTimestamp(), message.getTimestamp(), message.getTimestamp(),
                    message.getFrom(), message.getContent());
            incomingQueueDisplay.append(line + "\n");
            incomingQueueDisplay.setCaretPosition(incomingQueueDisplay.getDocument().getLength());
        });
    }

    private void updateOutgoingQueueDisplay(Message message, String target) {
        SwingUtilities.invokeLater(() -> {
            String line = String.format("[%tH:%tM:%tS] -> %-15s: %s",
                    message.getTimestamp(), message.getTimestamp(), message.getTimestamp(),
                    target, message.getContent());
            outgoingQueueDisplay.append(line + "\n");
            outgoingQueueDisplay.setCaretPosition(outgoingQueueDisplay.getDocument().getLength());
        });
    }

    private void monitorParkingSpot() {
        if (!isOccupied && distance > 0) {
            if (System.currentTimeMillis() % 15000 < 1000) {
                logMessage("Мониторинг: место " + spotNumber + " свободно, расстояние: " +
                        String.format(Locale.US, "%.1f", distance) + "м");
            }

            if (System.currentTimeMillis() % 20000 < 1000) {
                logMessage("✓ Проверка датчиков расстояния - ОК");
            }

            if (System.currentTimeMillis() % 25000 < 1000) {
                logMessage("✓ Калибровка системы позиционирования - ОК");
            }
        }
    }

    private void updateGuidanceData() {
        if (guidanceActive && !isOccupied && distance > 0) {
            double guidanceAccuracy = Math.min(100, 100 - (distance / GUIDANCE_RANGE * 20));
            SwingUtilities.invokeLater(() -> {
                guidanceProgressBar.setValue((int)guidanceAccuracy);
                guidanceProgressBar.setString(String.format("%.0f%%", guidanceAccuracy));
            });
        }
    }

    private double calculateSteeringCorrection() {
        return (random.nextDouble() - 0.5) * 10.0;
    }

    private String getApproachDirection() {
        String[] directions = {"Прямой", "Левый поворот", "Правый поворот", "Диагональный"};
        return directions[random.nextInt(directions.length)];
    }

    private void simulateGuidanceProgress() {
        new Thread(() -> {
            try {
                for (int i = 0; i <= 5; i++) {
                    logMessage("✓ Шаг наведения " + i + "/5 выполнен");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void updateSpotInfo() {
        SwingUtilities.invokeLater(() -> {
            spotNumberLabel.setText(spotNumber);
            distanceLabel.setText(String.format(Locale.US, "%.1f м", distance));
            statusLabel.setText(isOccupied ? "ЗАНЯТО" : "СВОБОДНО");

            int progress = (int) ((MAX_DISTANCE - distance) / (MAX_DISTANCE - MIN_DISTANCE) * 100);
            progress = Math.max(0, Math.min(100, progress));
            distanceBar.setValue(progress);
            distanceBar.setString(progress + "%");

            if (progress > 80) {
                distanceBar.setForeground(Color.GREEN);
                statusLabel.setForeground(Color.RED);
                statusLabel.setBackground(isOccupied ? Color.RED : new Color(0, 150, 0));
            } else if (progress > 50) {
                distanceBar.setForeground(Color.YELLOW);
                statusLabel.setForeground(Color.BLACK);
                statusLabel.setBackground(Color.YELLOW);
            } else {
                distanceBar.setForeground(Color.RED);
                statusLabel.setForeground(Color.WHITE);
                statusLabel.setBackground(Color.BLUE);
            }
        });
    }

    private void updateGuidanceStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            guidanceStatusLabel.setText(status);
            guidanceStatusLabel.setBackground(color);
            if (color == Color.RED || color == Color.BLUE) {
                guidanceStatusLabel.setForeground(Color.WHITE);
            } else {
                guidanceStatusLabel.setForeground(Color.BLACK);
            }
        });
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Система парковочного места (Hybrid)"));

        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Информация о парковочном месте"));

        infoPanel.add(new JLabel("Номер места:"));
        spotNumberLabel = new JLabel("A-00", JLabel.CENTER);
        spotNumberLabel.setFont(new Font("Arial", Font.BOLD, 16));
        infoPanel.add(spotNumberLabel);

        infoPanel.add(new JLabel("Расстояние:"));
        distanceLabel = new JLabel("0.0 м", JLabel.CENTER);
        distanceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(distanceLabel);

        JPanel statusPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Статус системы"));

        statusPanel.add(new JLabel("Статус места:"));
        statusLabel = new JLabel("СВОБОДНО", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.GREEN);
        statusLabel.setForeground(Color.BLACK);
        statusPanel.add(statusLabel);

        statusPanel.add(new JLabel("Система наведения:"));
        guidanceStatusLabel = new JLabel("Не активна", JLabel.CENTER);
        guidanceStatusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        guidanceStatusLabel.setOpaque(true);
        guidanceStatusLabel.setBackground(Color.GRAY);
        guidanceStatusLabel.setForeground(Color.WHITE);
        statusPanel.add(guidanceStatusLabel);

        topPanel.add(infoPanel);
        topPanel.add(statusPanel);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Индикация прогресса"));

        JPanel distanceProgressPanel = new JPanel(new BorderLayout());
        distanceProgressPanel.setBorder(BorderFactory.createTitledBorder("Прогресс приближения"));
        distanceBar = new JProgressBar(0, 100);
        distanceBar.setValue(0);
        distanceBar.setStringPainted(true);
        distanceBar.setString("0%");
        distanceBar.setForeground(Color.RED);
        distanceProgressPanel.add(distanceBar, BorderLayout.CENTER);

        JPanel guidanceProgressPanel = new JPanel(new BorderLayout());
        guidanceProgressPanel.setBorder(BorderFactory.createTitledBorder("Точность наведения"));
        guidanceProgressBar = new JProgressBar(0, 100);
        guidanceProgressBar.setValue(0);
        guidanceProgressBar.setStringPainted(true);
        guidanceProgressBar.setString("0%");
        guidanceProgressBar.setForeground(Color.BLUE);
        guidanceProgressPanel.add(guidanceProgressBar, BorderLayout.CENTER);

        queueStatsLabel = new JLabel("Отправлено: 0 | Получено: 0 | В очереди: 0");
        queueStatsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        queueStatsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        queueStatsLabel.setBorder(BorderFactory.createTitledBorder("Статистика очередей"));
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.add(queueStatsLabel, BorderLayout.CENTER);

        centerPanel.add(distanceProgressPanel);
        centerPanel.add(guidanceProgressPanel);
        centerPanel.add(statsPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Быстрое управление"));

        generateSpotButton = new JButton("Новое место");
        confirmParkingButton = new JButton("Подтвердить парковку");
        JButton manualGuidanceButton = new JButton("Ручное наведение");

        generateSpotButton.addActionListener(e -> generateNewSpot());
        confirmParkingButton.addActionListener(e -> confirmParking());
        confirmParkingButton.setEnabled(false);
        manualGuidanceButton.addActionListener(e -> provideGuidance());

        buttonPanel.add(generateSpotButton);
        buttonPanel.add(confirmParkingButton);
        buttonPanel.add(manualGuidanceButton);

        JPanel queuePanel = new JPanel(new GridLayout(1, 2, 10, 10));

        JPanel incomingPanel = new JPanel(new BorderLayout());
        incomingPanel.setBorder(BorderFactory.createTitledBorder("Входящая очередь (Consumer)"));
        incomingQueueDisplay = new JTextArea(10, 30);
        incomingQueueDisplay.setEditable(false);
        JScrollPane incomingScroll = new JScrollPane(incomingQueueDisplay);
        incomingPanel.add(incomingScroll, BorderLayout.CENTER);

        JButton clearIncomingButton = new JButton("Очистить");
        clearIncomingButton.addActionListener(e -> {
            incomingQueueDisplay.setText("");
            logMessage("Входящая очередь очищена");
        });
        incomingPanel.add(clearIncomingButton, BorderLayout.SOUTH);

        JPanel outgoingPanel = new JPanel(new BorderLayout());
        outgoingPanel.setBorder(BorderFactory.createTitledBorder("Исходящая очередь (Producer)"));
        outgoingQueueDisplay = new JTextArea(10, 30);
        outgoingQueueDisplay.setEditable(false);
        JScrollPane outgoingScroll = new JScrollPane(outgoingQueueDisplay);
        outgoingPanel.add(outgoingScroll, BorderLayout.CENTER);

        JButton clearOutgoingButton = new JButton("Очистить");
        clearOutgoingButton.addActionListener(e -> {
            outgoingQueueDisplay.setText("");
            logMessage("Исходящая очередь очищена");
        });
        outgoingPanel.add(clearOutgoingButton, BorderLayout.SOUTH);

        queuePanel.add(incomingPanel);
        queuePanel.add(outgoingPanel);

        logArea = new JTextArea(12, 60);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Журнал работы системы"));

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(new JScrollPane(queuePanel));
        mainSplitPane.setBottomComponent(scrollPane);
        mainSplitPane.setResizeWeight(0.5);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(mainSplitPane, BorderLayout.EAST);
    }
}