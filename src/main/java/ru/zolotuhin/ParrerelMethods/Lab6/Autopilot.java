package ru.zolotuhin.ParrerelMethods.Lab6;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class Autopilot extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;
    private ParkingSpot parkingSpot;

    private JTextArea logArea;
    private JTextField incomingDataField;
    private JTextField outgoingDataField;
    private JTextField currentActionField;
    private JTextField parkingSpotField;
    private JTextField distanceField;
    private SimpleDateFormat timeFormat;

    private double currentDistance = 0;
    private String currentSpotNumber = "Не задано";

    private boolean parkingSpotSearched = false;
    private boolean parkingInProgress = false;
    private boolean parkingCompleted = false;

    public Autopilot(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;
        this.parkingSpot = new ParkingSpot();

        initializeGUI();
        timeFormat = new SimpleDateFormat("HH:mm:ss");

        SocketUtils.startServer(5001, this::handleIncomingMessage);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Автопилот - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            Autopilot autopilot = new Autopilot(systemRunning);

            // Панель управления
            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление автопилотом"));

            JButton startButton = new JButton("Запуск парковки");
            JButton stopButton = new JButton("Экстренная остановка");
            JButton findSpotButton = new JButton("Найти место");
            JButton resetButton = new JButton("Сброс парковки");

            startButton.addActionListener(e -> {
                autopilot.start();
                autopilot.logMessage("Ручной запуск парковки");
            });

            stopButton.addActionListener(e -> {
                autopilot.handleEmergencyStop();
                autopilot.logMessage("Ручная экстренная остановка");
            });

            findSpotButton.addActionListener(e -> {
                autopilot.findParkingSpot();
                autopilot.logMessage("Поиск парковочного места");
            });

            resetButton.addActionListener(e -> {
                autopilot.resetParking();
                autopilot.logMessage("Ручной сброс парковки");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(findSpotButton);
            controlPanel.add(resetButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(autopilot, BorderLayout.CENTER);

            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            autopilot.findParkingSpot();
            autopilot.start();
        });
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "Autopilot-Thread");
            thread.start();
        }
    }

    public void interrupt() {
        systemRunning.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    @Override
    public void run() {
        logMessage("Автопилот запущен");
        updateCurrentAction("Инициализация системы");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (currentDistance <= 0) {
            logMessage("Парковочное место не задано - запуск поиска...");
            findParkingSpot();
            // Ждем получения данных о месте
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (currentDistance <= 0) {
                    break;
                }

                performParkingStep();
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (currentDistance <= 0) {
            logMessage("Автопилот: парковка успешно завершена!");
            updateCurrentAction("Парковка завершена");

            sendStopCommandsToAllComponents();

            SocketUtils.sendMessage("localhost", 5003, "CONFIRM_PARKING");

            parkingCompleted = true;
            parkingInProgress = false;
        } else {
            logMessage("Автопилот остановлен");
            updateCurrentAction("Остановлен");
        }
    }

    private void sendStopCommandsToAllComponents() {
        logMessage("Отправка команд остановки всем компонентам...");

        sendCommandToCamera("STOP");
        sendCommandToParkingSpot("STOP");
        sendCommandToBrake("FULL_STOP");
        sendCommandToSteering("RETURN_NEUTRAL");

        logMessage("Все компоненты получили команды остановки");
    }

    private void handleIncomingMessage(String message) {
        updateIncomingData(message);
        logMessage("Входящая команда: " + message);

        try {
            if (message.startsWith("SPOT:")) {
                String[] parts = message.split(":");
                if (parts.length >= 3) {
                    currentSpotNumber = parts[1];
                    String distanceStr = parts[2].replace(',', '.');
                    currentDistance = Double.parseDouble(distanceStr);
                    updateParkingSpotInfo(currentSpotNumber, currentDistance);
                    logMessage("Получено парковочное место: " + currentSpotNumber + ", расстояние: " + currentDistance + "м");
                    parkingSpotSearched = true;
                }
            }
            else if (message.startsWith("PARKING_COMPLETED:")) {
                String spotNumber = message.substring(18);
                handleParkingCompleted(spotNumber);
            }
            else {
                switch (message) {
                    case "OBSTACLE_СЛЕВА" -> handleObstacleLeft();
                    case "OBSTACLE_СПРАВА" -> handleObstacleRight();
                    case "OBSTACLE_СПЕРЕДИ" -> handleObstacleFront();
                    case "OBSTACLE_СЗАДИ" -> handleObstacleRear();
                    case "OBSTACLE_CLEAR" -> handleObstacleClear();
                    case "EMERGENCY_STOP" -> handleEmergencyStop();
                    case "PARKING_SPOT_FOUND" -> handleParkingSpotFound();
                    case "ENVIRONMENT_ANALYZED" -> logMessage("Камера завершила анализ окружения");
                    case "STEERING_CALIBRATED" -> logMessage("Рулевое управление откалибровано");
                    case "CALIBRATION_COMPLETE" -> logMessage("Камера откалибрована");
                }
            }
        } catch (NumberFormatException e) {
            logMessage("ОШИБКА: Неверный формат числа в сообщении: " + message);
            System.err.println("Ошибка парсинга числа: " + message);
        } catch (Exception e) {
            logMessage("ОШИБКА обработки сообщения: " + e.getMessage());
            System.err.println("Ошибка обработки сообщения: " + e.getMessage());
        }
    }

    private void findParkingSpot() {
        logMessage("Запрос на поиск парковочного места");
        updateCurrentAction("Поиск парковочного места");
        SocketUtils.sendMessage("localhost", 5002, "FIND_PARKING_SPOT");
    }

    private void handleParkingSpotFound() {
        logMessage("Парковочное место найдено камерой");
        updateCurrentAction("Ожидание данных места");
    }

    private void performParkingStep() {
        if (currentDistance <= 0) {
            logMessage("Парковка завершена, расстояние: " + currentDistance + "м");
            updateCurrentAction("Парковка завершена");
            return;
        }

        logMessage("Выполняю шаг парковки. Оставшееся расстояние: " + String.format("%.1f", currentDistance) + "м");

        if (currentDistance > 20) {
            updateCurrentAction("Движение к парковочному месту");
            updateOutgoingData("Запрос анализа окружения");

            sendCommandToCamera("ANALYZE_ENVIRONMENT");
            sendCommandToParkingSpot("PROVIDE_GUIDANCE");
            sendCommandToSteering("TURN_WHEELS:0");
            sendCommandToBrake("APPLY_BRAKE:0");
            sendCommandToParkingSpot("REDUCE_DISTANCE:5.0");

        } else if (currentDistance > 10) {
            updateCurrentAction("Подготовка к парковке");
            sendCommandToSteering("TURN_WHEELS:15");
            sendCommandToBrake("APPLY_BRAKE:20");
            sendCommandToParkingSpot("REDUCE_DISTANCE:2.0");

        } else if (currentDistance > 5) {
            updateCurrentAction("Маневрирование");
            sendCommandToSteering("TURN_WHEELS:30");
            sendCommandToBrake("APPLY_BRAKE:30");
            sendCommandToParkingSpot("REDUCE_DISTANCE:1.0");

        } else if (currentDistance > 2) {
            updateCurrentAction("Точное позиционирование");
            sendCommandToSteering("TURN_WHEELS:45");
            sendCommandToBrake("APPLY_BRAKE:50");
            sendCommandToParkingSpot("REDUCE_DISTANCE:0.5");

        } else if (currentDistance > 0.5) {
            updateCurrentAction("Финальное приближение");
            sendCommandToSteering("TURN_WHEELS:0");
            sendCommandToBrake("APPLY_BRAKE:70");
            sendCommandToParkingSpot("REDUCE_DISTANCE:0.2");

        } else if (currentDistance > 0) {
            updateCurrentAction("Финальная остановка");
            sendCommandToSteering("TURN_WHEELS:0");
            sendCommandToBrake("FULL_STOP");
            sendCommandToParkingSpot("STOP");
            sendCommandToCamera("STOP");

            currentDistance = 0;
            logMessage("Финальная остановка выполнена, расстояние: 0м");
        }

        if (currentDistance > 0) {
            currentDistance = getUpdatedDistance();
            logMessage("Расстояние обновлено: " + String.format("%.1f", currentDistance) + "м");
        }
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
        currentDistance = 0;
        currentSpotNumber = "Не задано";
        updateParkingSpotInfo(currentSpotNumber, currentDistance);
        updateCurrentAction("Ожидание запуска");

        SocketUtils.sendMessage("localhost", 5002, "RESET_PARKING_SEARCH");

        logMessage("Состояние парковки сброшено, готов к новому поиску");
    }

    private void sendCommandToCamera(String command) {
        if (SocketUtils.isPortAvailable("localhost", 5002)) {
            SocketUtils.sendMessage("localhost", 5002, command);
        } else {
            logMessage("Камера не доступна для команды: " + command);
        }
    }

    private void sendCommandToParkingSpot(String command) {
        if (SocketUtils.isPortAvailable("localhost", 5003)) {
            SocketUtils.sendMessage("localhost", 5003, command);
        } else {
            logMessage("Модуль парковки не доступен для команды: " + command);
        }
    }

    private void sendCommandToBrake(String command) {
        if (SocketUtils.isPortAvailable("localhost", 5004)) {
            SocketUtils.sendMessage("localhost", 5004, command);
        } else {
            logMessage("Тормозная система не доступна для команды: " + command);
        }
    }

    private void sendCommandToSteering(String command) {
        if (SocketUtils.isPortAvailable("localhost", 5005)) {
            SocketUtils.sendMessage("localhost", 5005, command);
        } else {
            logMessage("Рулевое управление не доступно для команды: " + command);
        }
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
        SocketUtils.sendMessage("localhost", 5005, "TURN_WHEELS:15");
        SocketUtils.sendMessage("localhost", 5004, "APPLY_BRAKE:40");
    }

    private void handleObstacleRight() {
        logMessage("Препятствие справа - поворот налево");
        SocketUtils.sendMessage("localhost", 5005, "TURN_WHEELS:-15");
        SocketUtils.sendMessage("localhost", 5004, "APPLY_BRAKE:40");
    }

    private void handleObstacleFront() {
        logMessage("Препятствие спереди - экстренное торможение");
        SocketUtils.sendMessage("localhost", 5004, "EMERGENCY_STOP");
        SocketUtils.sendMessage("localhost", 5005, "TURN_WHEELS:0");
    }

    private void handleObstacleRear() {
        logMessage("Препятствие сзади - движение вперед");
        SocketUtils.sendMessage("localhost", 5004, "APPLY_BRAKE:0");
        SocketUtils.sendMessage("localhost", 5005, "TURN_WHEELS:0");
    }

    private void handleObstacleClear() {
        logMessage("Препятствие устранено - возобновление нормальной работы");
        SocketUtils.sendMessage("localhost", 5005, "TURN_WHEELS:0");
        SocketUtils.sendMessage("localhost", 5004, "APPLY_BRAKE:10");
    }

    private void handleEmergencyStop() {
        logMessage("Выполняю экстренную остановку");
        sendStopCommandsToAllComponents();
        systemRunning.set(false);
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateIncomingData(String data) {
        SwingUtilities.invokeLater(() -> {
            incomingDataField.setText(data);
        });
    }

    private void updateOutgoingData(String data) {
        SwingUtilities.invokeLater(() -> {
            outgoingDataField.setText(data);
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
        setBorder(BorderFactory.createTitledBorder("Автопилот системы парковки"));

        JPanel statusPanel = new JPanel(new GridLayout(5, 2, 5, 5));

        statusPanel.add(new JLabel("Парковочное место:"));
        parkingSpotField = new JTextField("Не задано");
        parkingSpotField.setEditable(false);
        statusPanel.add(parkingSpotField);

        statusPanel.add(new JLabel("Расстояние:"));
        distanceField = new JTextField("0.0 м");
        distanceField.setEditable(false);
        statusPanel.add(distanceField);

        statusPanel.add(new JLabel("Входящие данные:"));
        incomingDataField = new JTextField();
        incomingDataField.setEditable(false);
        statusPanel.add(incomingDataField);

        statusPanel.add(new JLabel("Исходящие данные:"));
        outgoingDataField = new JTextField();
        outgoingDataField.setEditable(false);
        statusPanel.add(outgoingDataField);

        statusPanel.add(new JLabel("Текущее действие:"));
        currentActionField = new JTextField("Ожидание запуска");
        currentActionField.setEditable(false);
        statusPanel.add(currentActionField);

        logArea = new JTextArea(15, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Журнал работы"));

        add(statusPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private static class ParkingSpot {
        private String spotNumber;
        private double distance;

        public ParkingSpot() {}
    }
}