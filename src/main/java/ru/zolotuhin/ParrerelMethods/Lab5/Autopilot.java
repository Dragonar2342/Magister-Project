package ru.zolotuhin.ParrerelMethods.Lab5;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Autopilot extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;
    private final BlockingQueue<String> commands;

    private Camera camera;
    private Brake brake;
    private Steering steering;
    private ParkingSpot parkingSpot;

    private JTextArea logArea;
    private JTextField incomingDataField;
    private JTextField outgoingDataField;
    private JTextField currentActionField;
    private SimpleDateFormat timeFormat;

    public Autopilot(AtomicBoolean systemRunning, BlockingQueue<String> commands, ParkingSpot parkingSpot) {
        this.systemRunning = systemRunning;
        this.commands = commands;
        this.parkingSpot = parkingSpot;

        initializeGUI();
        timeFormat = new SimpleDateFormat("HH:mm:ss");
    }



    public void start() {
        thread = new Thread(this, "Autopilot-Thread");
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
        logMessage("Автопилот запущен");
        updateCurrentAction("Инициализация системы");

        while (systemRunning.get() && !Thread.currentThread().isInterrupted() && parkingSpot.getDistance() > 0) {
            try {
                processCommands();
                performParkingStep();
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (parkingSpot.getDistance() <= 0) {
            logMessage("Автопилот: парковка успешно завершена!");
            updateCurrentAction("Парковка завершена");
        } else {
            logMessage("Автопилот остановлен");
            updateCurrentAction("Остановлен");
        }
    }

    private void processCommands() throws InterruptedException {
        String command = commands.poll(100, TimeUnit.MILLISECONDS);
        if (command != null) {
            updateIncomingData(command);
            logMessage("Обработка команды: " + command);

            switch (command) {
                case "OBSTACLE_СЛЕВА" -> handleObstacleLeft();
                case "OBSTACLE_СПРАВА" -> handleObstacleRight();
                case "OBSTACLE_СПЕРЕДИ" -> handleObstacleFront();
                case "OBSTACLE_СЗАДИ" -> handleObstacleRear();
                case "OBSTACLE_CLEAR" -> handleObstacleClear();
                case "EMERGENCY_STOP" -> handleEmergencyStop();
            }
        }
    }

    private void performParkingStep() {
        double currentDistance = parkingSpot.getDistance();

        if (currentDistance > 20) {
            updateCurrentAction("Движение к парковочному месту");
            updateOutgoingData("Запрос анализа окружения");
            camera.analyzeEnvironment();

            updateOutgoingData("Запрос данных парковки");
            parkingSpot.provideGuidance();

            steering.turnWheels(0);
            brake.applyBrake(0);
            parkingSpot.reduceDistance(5.0);

        } else if (currentDistance > 10) {
            updateCurrentAction("Подготовка к парковке");
            steering.turnWheels(15);
            brake.applyBrake(20);

            parkingSpot.reduceDistance(2.0);

        } else if (currentDistance > 5) {
            updateCurrentAction("Маневрирование");
            steering.turnWheels(30);
            brake.applyBrake(30);

            parkingSpot.reduceDistance(1.0);

        } else if (currentDistance > 2) {
            updateCurrentAction("Точное позиционирование");
            steering.turnWheels(45);
            brake.applyBrake(50);

            parkingSpot.reduceDistance(0.5);

        } else if (currentDistance > 0) {
            updateCurrentAction("Финальное приближение");
            steering.turnWheels(0);
            brake.applyBrake(70);

            parkingSpot.reduceDistance(0.2);
        }

        logMessage("Оставшееся расстояние: " + String.format("%.1f", currentDistance) + "м");
    }

    private void handleObstacleLeft() {
        logMessage("Препятствие слева - поворот направо");
        steering.turnWheels(15);
        brake.applyBrake(40);
    }

    private void handleObstacleRight() {
        logMessage("Препятствие справа - поворот налево");
        steering.turnWheels(-15);
        brake.applyBrake(40);
    }

    private void handleObstacleFront() {
        logMessage("Препятствие спереди - экстренное торможение");
        brake.emergencyStop();
        steering.turnWheels(0);
    }

    private void handleObstacleRear() {
        logMessage("Препятствие сзади - движение вперед");
        brake.applyBrake(0);
        steering.turnWheels(0);
    }

    private void handleObstacleClear() {
        logMessage("Препятствие устранено - возобновление нормальной работы");
        steering.turnWheels(0);
        brake.applyBrake(10);
    }

    private void handleEmergencyStop() {
        logMessage("Выполняю экстренную остановку");
        brake.emergencyStop();
        steering.returnToNeutral();
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

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Автопилот"));

        JPanel statusPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        statusPanel.add(new JLabel("Входящие данные:"));
        incomingDataField = new JTextField();
        incomingDataField.setEditable(false);
        statusPanel.add(incomingDataField);

        statusPanel.add(new JLabel("Исходящие данные:"));
        outgoingDataField = new JTextField();
        outgoingDataField.setEditable(false);
        statusPanel.add(outgoingDataField);

        statusPanel.add(new JLabel("Текущее действие:"));
        currentActionField = new JTextField();
        currentActionField.setEditable(false);
        statusPanel.add(currentActionField);

        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(statusPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setComponents(Camera camera, ParkingSpot parkingSpot, Brake brake, Steering steering) {
        this.camera = camera;
        this.brake = brake;
        this.steering = steering;
        this.parkingSpot = parkingSpot;
    }
}
