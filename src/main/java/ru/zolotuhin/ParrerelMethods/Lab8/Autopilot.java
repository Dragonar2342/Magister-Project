package ru.zolotuhin.ParrerelMethods.Lab8;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class Autopilot extends JPanel implements Runnable {
    private Thread thread;
    private Thread consumerThread;
    private Thread heartbeatThread;
    private final AtomicBoolean systemRunning;
    private boolean waitingForSpot = false;

    private JTextArea logArea;
    private JTextArea incomingQueueDisplay;
    private JTextArea outgoingQueueDisplay;
    private JTextField currentActionField;
    private JTextField parkingSpotField;
    private JTextField distanceField;
    private JLabel queueStatsLabel;
    private SimpleDateFormat timeFormat;

    private double currentDistance = 0;
    private String currentSpotNumber = "Не задано";
    private int messagesSent = 0;
    private int messagesReceived = 0;

    private boolean parkingSpotSearched = false;
    private boolean parkingInProgress = false;
    private boolean parkingCompleted = false;

    public Autopilot(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;

        // Регистрируем компонент через SocketUtils
        SocketUtils.registerComponent("Autopilot");

        initializeGUI();
        startConsumer();
        startHeartbeat();
        timeFormat = new SimpleDateFormat("HH:mm:ss");

        // Отправляем сообщение о запуске
        sendStatusMessage("Autopilot: запущен и готов к работе");

        SocketUtils.startServer(5001, this::handleSocketMessage);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Автопилот - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            Autopilot autopilot = new Autopilot(systemRunning);

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление автопилотом"));

            JButton startButton = new JButton("Запуск");
            JButton stopButton = new JButton("Остановка");
            JButton startParkingButton = new JButton("Начать парковку");
            JButton findSpotButton = new JButton("Найти место");
            JButton resetButton = new JButton("Сброс");
            JButton testProducerButton = new JButton("Тест Producer");
            JButton testConsumerButton = new JButton("Тест Consumer");

            startButton.addActionListener(e -> {
                autopilot.start();
                autopilot.logMessage("Ручной запуск автопилота");
            });

            stopButton.addActionListener(e -> {
                autopilot.interrupt();
                autopilot.logMessage("Ручная остановка автопилота");
            });

            startParkingButton.addActionListener(e -> {
                autopilot.startParkingProcess();
                autopilot.logMessage("Ручной запуск процесса парковки");
            });

            findSpotButton.addActionListener(e -> {
                autopilot.findParkingSpot();
                autopilot.logMessage("Ручной поиск парковочного места");
            });

            resetButton.addActionListener(e -> {
                autopilot.resetParking();
                autopilot.logMessage("Ручной сброс парковки");
            });

            testProducerButton.addActionListener(e -> {
                autopilot.testProducer();
                autopilot.logMessage("Тестирование Producer");
            });

            testConsumerButton.addActionListener(e -> {
                autopilot.testConsumer();
                autopilot.logMessage("Тестирование Consumer");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(startParkingButton);
            controlPanel.add(findSpotButton);
            controlPanel.add(resetButton);
            controlPanel.add(testProducerButton);
            controlPanel.add(testConsumerButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(autopilot, BorderLayout.CENTER);

            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            autopilot.start();
        });
    }

    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Отправляем heartbeat каждые 5 секунд через SocketUtils
                    SocketUtils.sendMessageViaMQ("StatusWindow", "Autopilot", "HEARTBEAT", MessageType.HEARTBEAT);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Autopilot-Heartbeat");
        heartbeatThread.start();
    }

    private void sendStatusMessage(String content) {
        produceMessage("StatusWindow", content, MessageType.STATUS);
    }

    private void startConsumer() {
        consumerThread = new Thread(() -> {
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Получаем сообщение через SocketUtils
                    Message message = SocketUtils.receiveMessageViaMQ("Autopilot");

                    if (message != null) {
                        messagesReceived++;
                        processMessage(message);
                        updateQueueStats();
                        updateIncomingQueueDisplay(message);
                    } else {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "Autopilot-Consumer");
        consumerThread.start();
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "Autopilot-Thread");
            thread.start();
            updateCurrentAction("Инициализация системы");
            logMessage("Автопилот запущен (Hybrid)");
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
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        updateCurrentAction("Остановлен");
        sendStatusMessage("Autopilot: остановлен");
    }

    @Override
    public void run() {
        logMessage("Автопилот запущен (Hybrid)");
        updateCurrentAction("Ожидание запуска");

        // Главный цикл автопилота
        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (parkingInProgress && currentDistance > 0 && !parkingCompleted) {
                    performParkingStep();
                } else if (waitingForSpot) {
                    updateCurrentAction("Ожидание парковочного места...");
                    Thread.sleep(1000);
                } else {
                    // Автопилот работает в фоновом режиме
                    monitorSystem();
                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Автопилот остановлен");
        updateCurrentAction("Остановлен");
    }

    public void startParkingProcess() {
        if (parkingCompleted) {
            logMessage("Парковка уже завершена. Сначала выполните сброс.");
            return;
        }

        if (currentDistance <= 0) {
            logMessage("Парковочное место не задано. Запуск поиска...");
            waitingForSpot = true;
            findParkingSpot();
            updateCurrentAction("Поиск парковочного места");
        } else {
            logMessage("Начинаю процесс парковки на месте " + currentSpotNumber);
            parkingInProgress = true;
            waitingForSpot = false;
            updateCurrentAction("Парковка на месте " + currentSpotNumber);

            produceMessage("StatusWindow", "Autopilot: начата парковка на месте " + currentSpotNumber,
                    MessageType.STATUS);
        }
    }

    private void handleSocketMessage(String rawMessage) {
        // Помещаем сообщение в свою очередь через SocketUtils
        SocketUtils.sendMessageViaMQ("Autopilot", "Socket", rawMessage, MessageType.COMMAND);
        logMessage("Сообщение из сокета помещено в очередь: " + rawMessage);
    }

    private void processMessage(Message message) {
        String content = message.getContent();
        String from = message.getFrom();

        logMessage("Обработка сообщения от " + from + ": " + content);

        try {
            if (content.startsWith("SPOT:")) {
                String[] parts = content.split(":");
                if (parts.length >= 3) {
                    currentSpotNumber = parts[1];
                    String distanceStr = parts[2].replace(',', '.');
                    currentDistance = Double.parseDouble(distanceStr);
                    updateParkingSpotInfo(currentSpotNumber, currentDistance);
                    logMessage("Получено парковочное место: " + currentSpotNumber + ", расстояние: " + currentDistance + "м");
                    parkingSpotSearched = true;
                    waitingForSpot = false;

                    if (!parkingInProgress) {
                        logMessage("Парковочное место найдено. Готов к парковке.");
                        updateCurrentAction("Готов к парковке на " + currentSpotNumber);
                    }

                    produceMessage("StatusWindow", "Autopilot: получено место " + currentSpotNumber + " (" + currentDistance + "м)",
                            MessageType.STATUS);
                }
            }
            else if (content.startsWith("PARKING_COMPLETED:")) {
                String spotNumber = content.substring(18);
                handleParkingCompleted(spotNumber);
            }
            else {
                switch (content) {
                    case "OBSTACLE_СЛЕВА" -> handleObstacleLeft();
                    case "OBSTACLE_СПРАВА" -> handleObstacleRight();
                    case "OBSTACLE_СПЕРЕДИ" -> handleObstacleFront();
                    case "OBSTACLE_СЗАДИ" -> handleObstacleRear();
                    case "OBSTACLE_CLEAR" -> handleObstacleClear();
                    case "EMERGENCY_STOP" -> handleEmergencyStop();
                    case "PARKING_SPOT_FOUND" -> {
                        handleParkingSpotFound();
                        produceMessage("StatusWindow", "Autopilot: парковочное место найдено",
                                MessageType.STATUS);
                    }
                    case "ENVIRONMENT_ANALYZED" -> {
                        logMessage("Камера завершила анализ окружения");
                        produceMessage("StatusWindow", "Autopilot: окружение проанализировано",
                                MessageType.STATUS);
                    }
                    case "STEERING_CALIBRATED" -> {
                        logMessage("Рулевое управление откалибровано");
                        produceMessage("StatusWindow", "Autopilot: руль откалиброван",
                                MessageType.STATUS);
                    }
                    case "CALIBRATION_COMPLETE" -> {
                        logMessage("Камера откалибрована");
                        produceMessage("StatusWindow", "Autopilot: камера откалибрована",
                                MessageType.STATUS);
                    }
                }
            }
        } catch (NumberFormatException e) {
            logMessage("ОШИБКА: Неверный формат числа в сообщении: " + content);
        } catch (Exception e) {
            logMessage("ОШИБКА обработки сообщения: " + e.getMessage());
        }
    }

    private void produceMessage(String target, String content, MessageType type) {
        // Используем SocketUtils вместо MessageQueueManager
        boolean success = SocketUtils.sendMessageViaMQ(target, "Autopilot", content, type);

        if (success) {
            messagesSent++;
            updateQueueStats();
            Message message = new Message("Autopilot", content, type);
            updateOutgoingQueueDisplay(message, target);
            logMessage("Сообщение отправлено в " + target + ": " + content);
        } else {
            logMessage("ОШИБКА: очередь " + target + " переполнена или нет связи с сервером!");
        }
    }

    public void findParkingSpot() {
        logMessage("Запрос на поиск парковочного места");
        updateCurrentAction("Поиск парковочного места");
        waitingForSpot = true;
        produceMessage("Camera", "FIND_PARKING_SPOT", MessageType.COMMAND);
        produceMessage("StatusWindow", "Autopilot: поиск парковочного места",
                MessageType.STATUS);
    }

    private void handleParkingSpotFound() {
        logMessage("Парковочное место найдено камерой");
        updateCurrentAction("Ожидание данных места");
    }

    private void performParkingStep() {
        if (currentDistance <= 0) {
            completeParking();
            return;
        }

        logMessage("Выполняю шаг парковки. Оставшееся расстояние: " + String.format("%.1f", currentDistance) + "м");

        if (currentDistance > 20) {
            updateCurrentAction("Движение к парковочному месту");
            produceMessage("Camera", "ANALYZE_ENVIRONMENT", MessageType.COMMAND);
            produceMessage("ParkingSpot", "PROVIDE_GUIDANCE", MessageType.COMMAND);
            produceMessage("Steering", "TURN_WHEELS:0", MessageType.COMMAND);
            produceMessage("Brake", "APPLY_BRAKE:0", MessageType.COMMAND);
            produceMessage("ParkingSpot", "REDUCE_DISTANCE:5.0", MessageType.COMMAND);

        } else if (currentDistance > 10) {
            updateCurrentAction("Подготовка к парковке");
            produceMessage("Steering", "TURN_WHEELS:15", MessageType.COMMAND);
            produceMessage("Brake", "APPLY_BRAKE:20", MessageType.COMMAND);
            produceMessage("ParkingSpot", "REDUCE_DISTANCE:2.0", MessageType.COMMAND);

        } else if (currentDistance > 5) {
            updateCurrentAction("Маневрирование");
            produceMessage("Steering", "TURN_WHEELS:30", MessageType.COMMAND);
            produceMessage("Brake", "APPLY_BRAKE:30", MessageType.COMMAND);
            produceMessage("ParkingSpot", "REDUCE_DISTANCE:1.0", MessageType.COMMAND);

        } else if (currentDistance > 2) {
            updateCurrentAction("Точное позиционирование");
            produceMessage("Steering", "TURN_WHEELS:45", MessageType.COMMAND);
            produceMessage("Brake", "APPLY_BRAKE:50", MessageType.COMMAND);
            produceMessage("ParkingSpot", "REDUCE_DISTANCE:0.5", MessageType.COMMAND);

        } else if (currentDistance > 0.5) {
            updateCurrentAction("Финальное приближение");
            produceMessage("Steering", "TURN_WHEELS:0", MessageType.COMMAND);
            produceMessage("Brake", "APPLY_BRAKE:70", MessageType.COMMAND);
            produceMessage("ParkingSpot", "REDUCE_DISTANCE:0.2", MessageType.COMMAND);

        } else if (currentDistance > 0) {
            updateCurrentAction("Финальная остановка");
            produceMessage("Steering", "TURN_WHEELS:0", MessageType.COMMAND);
            produceMessage("Brake", "FULL_STOP", MessageType.COMMAND);
            produceMessage("ParkingSpot", "STOP", MessageType.COMMAND);
            produceMessage("Camera", "STOP", MessageType.COMMAND);

            currentDistance = 0;
            logMessage("Финальная остановка выполнена, расстояние: 0м");
        }

        if (currentDistance > 0) {
            currentDistance = getUpdatedDistance();
            logMessage("Расстояние обновлено: " + String.format("%.1f", currentDistance) + "м");
        }
    }

    private void completeParking() {
        logMessage("★★★★★ ПАРКОВКА ЗАВЕРШЕНА ★★★★★");
        logMessage("Автомобиль припаркован на месте " + currentSpotNumber);
        updateCurrentAction("Парковка завершена");
        parkingCompleted = true;
        parkingInProgress = false;

        sendStopCommandsToAllComponents();

        produceMessage("ParkingSpot", "CONFIRM_PARKING", MessageType.COMMAND);
        produceMessage("StatusWindow", "Autopilot: парковка завершена на месте " + currentSpotNumber,
                MessageType.STATUS);
    }

    private void sendStopCommandsToAllComponents() {
        logMessage("Отправка команд остановки всем компонентам...");

        produceMessage("Camera", "STOP", MessageType.COMMAND);
        produceMessage("ParkingSpot", "STOP", MessageType.COMMAND);
        produceMessage("Brake", "FULL_STOP", MessageType.COMMAND);
        produceMessage("Steering", "RETURN_NEUTRAL", MessageType.COMMAND);
        produceMessage("StatusWindow", "Autopilot: парковка завершена",
                MessageType.STATUS);

        logMessage("Все компоненты получили команды остановки");
    }

    private void handleParkingCompleted(String spotNumber) {
        logMessage("Парковка завершена на месте: " + spotNumber);
        parkingCompleted = true;
        parkingInProgress = false;
        currentDistance = 0;
        updateParkingSpotInfo(spotNumber, 0);
    }

    public void resetParking() {
        logMessage("Сброс состояния парковки...");
        parkingCompleted = false;
        parkingInProgress = false;
        parkingSpotSearched = false;
        waitingForSpot = false;
        currentDistance = 0;
        currentSpotNumber = "Не задано";
        updateParkingSpotInfo(currentSpotNumber, currentDistance);
        updateCurrentAction("Ожидание запуска");

        produceMessage("Camera", "RESET_PARKING_SEARCH", MessageType.COMMAND);
        produceMessage("StatusWindow", "Autopilot: сброс парковки", MessageType.STATUS);

        logMessage("Состояние парковки сброшено, готов к новому поиску");
    }

    private double getUpdatedDistance() {
        double reduction;

        if (currentDistance > 20) {
            reduction = 5.0;
        } else if (currentDistance > 10) {
            reduction = 2.0;
        } else if (currentDistance > 5) {
            reduction = 1.0;
        } else if (currentDistance > 2) {
            reduction = 0.5;
        } else {
            reduction = 0.2;
        }

        double newDistance = Math.max(0, currentDistance - reduction);
        logMessage("Уменьшение расстояния: " + String.format("%.1f", currentDistance) +
                "м -> " + String.format("%.1f", newDistance) + "м (шаг: " + reduction + "м)");

        return newDistance;
    }

    private void handleObstacleLeft() {
        logMessage("Препятствие слева - поворот направо");
        produceMessage("Steering", "TURN_WHEELS:15", MessageType.COMMAND);
        produceMessage("Brake", "APPLY_BRAKE:40", MessageType.COMMAND);
    }

    private void handleObstacleRight() {
        logMessage("Препятствие справа - поворот налево");
        produceMessage("Steering", "TURN_WHEELS:-15", MessageType.COMMAND);
        produceMessage("Brake", "APPLY_BRAKE:40", MessageType.COMMAND);
    }

    private void handleObstacleFront() {
        logMessage("Препятствие спереди - экстренное торможение");
        produceMessage("Brake", "EMERGENCY_STOP", MessageType.EMERGENCY);
        produceMessage("Steering", "TURN_WHEELS:0", MessageType.COMMAND);
    }

    private void handleObstacleRear() {
        logMessage("Препятствие сзади - движение вперед");
        produceMessage("Brake", "APPLY_BRAKE:0", MessageType.COMMAND);
        produceMessage("Steering", "TURN_WHEELS:0", MessageType.COMMAND);
    }

    private void handleObstacleClear() {
        logMessage("Препятствие устранено - возобновление нормальной работы");
        produceMessage("Steering", "TURN_WHEELS:0", MessageType.COMMAND);
        produceMessage("Brake", "APPLY_BRAKE:10", MessageType.COMMAND);
    }

    private void handleEmergencyStop() {
        logMessage("Выполняю экстренную остановку");
        sendStopCommandsToAllComponents();
        systemRunning.set(false);
    }

    private void monitorSystem() {
        // Периодический мониторинг системы
        if (System.currentTimeMillis() % 10000 < 500) {
            produceMessage("StatusWindow", "Autopilot: работает нормально",
                    MessageType.STATUS);
        }
    }

    private void testProducer() {
        logMessage("=== ТЕСТ PRODUCER ===");

        String[] testTargets = {"Brake", "Steering", "Camera", "ParkingSpot", "StatusWindow"};
        String[] testMessages = {
                "Тестовое сообщение 1",
                "ANALYZE_ENVIRONMENT",
                "TURN_WHEELS:45",
                "APPLY_BRAKE:30",
                "Проверка связи"
        };

        for (int i = 0; i < testTargets.length; i++) {
            produceMessage(testTargets[i], testMessages[i], MessageType.COMMAND);
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

        // Отправляем тестовые сообщения сами себе через SocketUtils
        for (int i = 1; i <= 5; i++) {
            SocketUtils.sendMessageViaMQ("Autopilot", "Autopilot-Test",
                    "Тестовое сообщение " + i, MessageType.DATA);
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
            int queueSize = SocketUtils.getQueueSize("Autopilot");
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

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateCurrentAction(String action) {
        SwingUtilities.invokeLater(() -> {
            currentActionField.setText(action);
        });
    }

    private void updateParkingSpotInfo(String spotNumber, double distance) {
        SwingUtilities.invokeLater(() -> {
            parkingSpotField.setText(spotNumber);
            distanceField.setText(String.format("%.1f м", distance));
        });
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Автопилот системы парковки (Hybrid)"));

        JPanel topPanel = new JPanel(new GridLayout(2, 3, 5, 5));

        topPanel.add(new JLabel("Парковочное место:"));
        parkingSpotField = new JTextField("Не задано");
        parkingSpotField.setEditable(false);
        topPanel.add(parkingSpotField);

        topPanel.add(new JLabel("Расстояние:"));
        distanceField = new JTextField("0.0 м");
        distanceField.setEditable(false);
        topPanel.add(distanceField);

        topPanel.add(new JLabel("Текущее действие:"));
        currentActionField = new JTextField("Ожидание запуска");
        currentActionField.setEditable(false);
        topPanel.add(currentActionField);

        queueStatsLabel = new JLabel("Отправлено: 0 | Получено: 0 | В очереди: 0");
        queueStatsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        queueStatsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        queueStatsLabel.setBorder(BorderFactory.createTitledBorder("Статистика очередей"));
        topPanel.add(queueStatsLabel);

        add(topPanel, BorderLayout.NORTH);

        JPanel queuePanel = new JPanel(new GridLayout(1, 2, 10, 10));

        JPanel incomingPanel = new JPanel(new BorderLayout());
        incomingPanel.setBorder(BorderFactory.createTitledBorder("Входящая очередь (Consumer)"));
        incomingQueueDisplay = new JTextArea(15, 35);
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
        outgoingQueueDisplay = new JTextArea(15, 35);
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

        logArea = new JTextArea(12, 80);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Журнал работы"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queuePanel, logScrollPane);
        splitPane.setResizeWeight(0.6);

        add(splitPane, BorderLayout.CENTER);
    }
}