package ru.zolotuhin.ParrerelMethods.Lab6;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;

public class Camera extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;
    private final BlockingQueue<String> incomingCommands;
    private final BlockingQueue<String> autopilotCommands;

    private Autopilot autopilot;

    // GUI компоненты
    private JTextArea logArea;
    private JLabel obstacleLabel;
    private Map<String, JButton> obstacleButtons;
    private String currentObstacle = null;

    private int analysisCount = 0;

    public Camera(AtomicBoolean systemRunning, BlockingQueue<String> incomingCommands,
                  BlockingQueue<String> autopilotCommands) {
        this.systemRunning = systemRunning;
        this.incomingCommands = incomingCommands;
        this.autopilotCommands = autopilotCommands;
        this.obstacleButtons = new HashMap<>();

        initializeGUI();
    }

    public void setAutopilot(Autopilot autopilot) {
        this.autopilot = autopilot;
    }

    public void start() {
        thread = new Thread(this, "Camera-Thread");
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
        logMessage("Камера запущена");

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                analysisCount++;
                logMessage("Начало цикла анализа " + analysisCount);

                processCommands();
                performAnalysis();

                logMessage("Цикл анализа " + analysisCount + " завершен");
                Thread.sleep(2500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Камера остановлена");
    }

    private void processCommands() throws InterruptedException {
        String command = incomingCommands.poll(100, TimeUnit.MILLISECONDS);
        if (command != null) {
            logMessage("Получена команда: " + command);

            switch (command) {
                case "OBSTACLE_DETECTED" -> {
                    logMessage("Обнаружено препятствие - отправляю предупреждение");
                    autopilotCommands.offer("OBSTACLE_DETECTED");
                }
                case "PARKING_SPOT_FOUND" -> {
                    logMessage("Парковочное место найдено - отправляю координаты");
                    autopilotCommands.offer("PARKING_SPOT_FOUND");
                }
                case "EMERGENCY_STOP" -> {
                    logMessage("Экстренная остановка - приостанавливаю анализ");
                    Thread.sleep(1000);
                }
                case "CALIBRATION_NEEDED" -> performCalibration();
            }
        }
    }

    public void analyzeEnvironment() {
        logMessage("Анализ окружающей среды...");
    }

    private void performAnalysis() {
        logMessage("Сканирование пространства на наличие парковочных мест...");
        logMessage("Обнаружение препятствий...");
        logMessage("Обработка видеопотока...");
    }

    private void performCalibration() {
        logMessage("Выполнение калибровки...");
        logMessage("Калибровка завершена успешно");
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
        autopilotCommands.offer("OBSTACLE_" + direction);
    }

    private void clearObstacle() {
        if (currentObstacle != null) {
            resetButton(currentObstacle);
            logMessage("Препятствие " + currentObstacle + " устранено");
        }
        currentObstacle = null;
        updateObstacleLabel(null);
        autopilotCommands.offer("OBSTACLE_CLEAR");
    }

    private void updateObstacleLabel(String obstacle) {
        SwingUtilities.invokeLater(() -> {
            if (obstacle == null) {
                obstacleLabel.setText("Препятствий нет");
                obstacleLabel.setBackground(Color.GREEN);
            } else {
                obstacleLabel.setText("Препятствие: " + obstacle);
                obstacleLabel.setBackground(Color.RED);
            }
        });
    }

    private void highlightButton(String direction) {
        JButton button = obstacleButtons.get(direction);
        if (button != null) {
            button.setBackground(Color.RED);
            button.setForeground(Color.WHITE);
        }
    }

    private void resetButton(String direction) {
        JButton button = obstacleButtons.get(direction);
        if (button != null) {
            button.setBackground(Color.LIGHT_GRAY);
            button.setForeground(Color.BLACK);
        }
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
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
        setBorder(BorderFactory.createTitledBorder("Камера"));

        obstacleLabel = new JLabel("Препятствий нет", JLabel.CENTER);
        obstacleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        obstacleLabel.setOpaque(true);
        obstacleLabel.setBackground(Color.GREEN);

        JPanel buttonPanel = createButtonPanel();

        logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(obstacleLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));

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
        button.addActionListener(new ObstacleButtonListener(direction.toUpperCase()));
        return button;
    }
}
