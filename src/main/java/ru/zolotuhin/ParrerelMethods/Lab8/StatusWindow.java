package ru.zolotuhin.ParrerelMethods.Lab8;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusWindow extends JPanel implements Runnable {
    private Thread thread;
    private Thread consumerThread;
    private final AtomicBoolean systemRunning;
    private final ConcurrentHashMap<String, JLabel> statusLabels;
    private final ConcurrentHashMap<String, JLabel> queueSizeLabels;
    private final ConcurrentHashMap<String, Long> lastHeartbeatTimes;

    private JTextArea logArea;
    private JLabel systemStatusLabel;
    private JLabel lastUpdateLabel;
    private JPanel queuePanel;
    private SimpleDateFormat timeFormat;

    private static final String[] MODULES = {"Autopilot", "Camera", "ParkingSpot", "Brake", "Steering"};

    public StatusWindow(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;
        this.statusLabels = new ConcurrentHashMap<>();
        this.queueSizeLabels = new ConcurrentHashMap<>();
        this.lastHeartbeatTimes = new ConcurrentHashMap<>();

        SocketUtils.registerComponent("StatusWindow");

        initializeGUI();
        timeFormat = new SimpleDateFormat("HH:mm:ss");

        System.out.println("StatusWindow: Регистрация компонентов...");
        for (String module : MODULES) {
            SocketUtils.registerComponent(module);
            lastHeartbeatTimes.put(module, 0L);
            System.out.println("StatusWindow: Зарегистрирован компонент: " + module);
        }

        startConsumer();
        start();
        logMessage("Монитор системы запущен");
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "StatusWindow-Thread");
            thread.start();
            updateSystemStatus("Активен", Color.GREEN);
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
        updateSystemStatus("Неактивен", Color.RED);
        logMessage("Мониторинг системы остановлен");
    }

    private void startConsumer() {
        consumerThread = new Thread(() -> {
            System.out.println("StatusWindow-Consumer: Поток запущен");
            while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Message message = SocketUtils.receiveMessageViaMQ("StatusWindow");

                    if (message != null) {
                        System.out.println("StatusWindow-Consumer: Получено сообщение от " +
                                message.getFrom() + ": " + message.getContent());
                        processStatusMessage(message);
                    } else {
                        System.out.println("StatusWindow-Consumer: Нет сообщений, ожидание...");
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("StatusWindow-Consumer: Поток прерван");
                    break;
                }
            }
            System.out.println("StatusWindow-Consumer: Поток остановлен");
        }, "StatusWindow-Consumer");
        consumerThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Монитор системы - Система автоматической парковки");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            AtomicBoolean systemRunning = new AtomicBoolean(true);
            StatusWindow statusWindow = new StatusWindow(systemRunning);

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.setBorder(BorderFactory.createTitledBorder("Управление монитором"));

            JButton startButton = new JButton("Запуск мониторинга");
            JButton stopButton = new JButton("Остановка");
            JButton refreshButton = new JButton("Обновить статусы");
            JButton clearLogButton = new JButton("Очистить журнал");

            startButton.addActionListener(e -> {
                if (statusWindow.thread == null || !statusWindow.thread.isAlive()) {
                    statusWindow.thread = new Thread(statusWindow, "StatusWindow-Thread");
                    statusWindow.thread.start();
                    statusWindow.logMessage("Мониторинг запущен");
                }
            });

            stopButton.addActionListener(e -> {
                systemRunning.set(false);
                if (statusWindow.thread != null) {
                    statusWindow.thread.interrupt();
                }
                if (statusWindow.consumerThread != null) {
                    statusWindow.consumerThread.interrupt();
                }
                statusWindow.logMessage("Мониторинг остановлен");
            });

            refreshButton.addActionListener(e -> {
                statusWindow.refreshQueueStatuses();
                statusWindow.logMessage("Статусы обновлены вручную");
            });

            clearLogButton.addActionListener(e -> {
                statusWindow.clearLog();
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(refreshButton);
            controlPanel.add(clearLogButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(statusWindow, BorderLayout.CENTER);

            frame.setSize(1000, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Автоматически запускаем мониторинг при старте
            if (statusWindow.thread == null || !statusWindow.thread.isAlive()) {
                statusWindow.thread = new Thread(statusWindow, "StatusWindow-Thread");
                statusWindow.thread.start();
            }
        });
    }

    @Override
    public void run() {
        logMessage("Запуск мониторинга системы");
        updateSystemStatus("Активен", Color.GREEN);

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                checkComponentsActivity();
                refreshQueueStatuses();
                updateLastUpdateTime();
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Мониторинг системы остановлен");
        updateSystemStatus("Неактивен", Color.RED);
    }

    private void processStatusMessage(Message message) {
        String from = message.getFrom();
        String content = message.getContent();
        MessageType type = message.getType();

        System.out.println("StatusWindow получил: от " + message.getFrom() +
                " - " + message.getContent() +
                " (тип: " + message.getType() + ")");

        logMessage("Сообщение от " + from + ": " + content + " (тип: " + type + ")");

        if (type == MessageType.HEARTBEAT) {
            // Обновляем время последнего heartbeat
            System.out.println("StatusWindow: Получен HEARTBEAT от " + from);
            lastHeartbeatTimes.put(from, System.currentTimeMillis());
            updateModuleStatus(from, "Активен", Color.GREEN);
        } else {
            logMessage("Сообщение от " + from + ": " + content);

            if (content.contains("запущен") || content.contains("активен") ||
                    content.contains("работает") || content.contains("готов")) {
                updateModuleStatus(from, "Активен", Color.GREEN);
            } else if (content.contains("остановлен") || content.contains("отключ") ||
                    content.contains("ошибка") || content.contains("авария")) {
                updateModuleStatus(from, "Ошибка", Color.RED);
            } else if (content.contains("ожидание") || content.contains("калибр") ||
                    content.contains("тест") || content.contains("поиск")) {
                updateModuleStatus(from, "Работает", Color.ORANGE);
            }
        }
    }

    private void checkComponentsActivity() {
        long currentTime = System.currentTimeMillis();

        for (String module : MODULES) {
            Long lastHeartbeat = lastHeartbeatTimes.get(module);
            if (lastHeartbeat == null || lastHeartbeat == 0) {
                // Никогда не получали heartbeat
                updateModuleStatus(module, "Неизвестно", Color.GRAY);
            } else {
                long timeDiff = currentTime - lastHeartbeat;

                if (timeDiff > 15000) { // 15 секунд без heartbeat
                    updateModuleStatus(module, "Нет связи", Color.RED);
                    logMessage("ВНИМАНИЕ: " + module + " не отвечает более 15 секунд");
                } else if (timeDiff > 10000) { // 10 секунд без heartbeat
                    updateModuleStatus(module, "Медленно", Color.ORANGE);
                }
            }
        }
    }

    public void refreshQueueStatuses() {
        SwingUtilities.invokeLater(() -> {
            for (String module : MODULES) {
                int queueSize = SocketUtils.getQueueSize(module);
                JLabel sizeLabel = queueSizeLabels.get(module);

                if (sizeLabel != null) {
                    String labelText = module + ": " + queueSize + " сообщений";
                    sizeLabel.setText(labelText);

                    // Цветовая индикация загруженности
                    if (queueSize > 20) {
                        sizeLabel.setForeground(Color.RED);
                        sizeLabel.setFont(new Font("Arial", Font.BOLD, 12));
                    } else if (queueSize > 10) {
                        sizeLabel.setForeground(Color.ORANGE);
                        sizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                    } else if (queueSize > 5) {
                        sizeLabel.setForeground(Color.YELLOW);
                        sizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                    } else if (queueSize > 0) {
                        sizeLabel.setForeground(Color.GREEN);
                        sizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                    } else {
                        sizeLabel.setForeground(Color.GRAY);
                        sizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                    }
                }
            }
        });
    }

    private void updateModuleStatus(String module, String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = statusLabels.get(module);
            if (statusLabel != null) {
                statusLabel.setText(status);
                statusLabel.setBackground(color);
                statusLabel.setForeground(color == Color.RED || color == Color.ORANGE ?
                        Color.WHITE : Color.BLACK);
            }
        });
    }

    private void updateSystemStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            systemStatusLabel.setText(status);
            systemStatusLabel.setBackground(color);
            systemStatusLabel.setForeground(color == Color.RED ? Color.WHITE : Color.BLACK);
        });
    }

    private void updateLastUpdateTime() {
        SwingUtilities.invokeLater(() -> {
            lastUpdateLabel.setText("Последнее обновление: " + timeFormat.format(new Date()));
        });
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void clearLog() {
        SwingUtilities.invokeLater(() -> {
            logArea.setText("");
            logMessage("Журнал очищен");
        });
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Монитор системы автоматической парковки"));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 5));

        JPanel systemStatusPanel = new JPanel(new BorderLayout());
        systemStatusLabel = new JLabel("Активен", JLabel.CENTER);
        systemStatusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        systemStatusLabel.setOpaque(true);
        systemStatusLabel.setBackground(Color.GREEN);
        systemStatusLabel.setForeground(Color.BLACK);
        systemStatusPanel.add(systemStatusLabel, BorderLayout.CENTER);
        systemStatusPanel.setBorder(BorderFactory.createTitledBorder("Общий статус системы"));

        JPanel updatePanel = new JPanel(new BorderLayout());
        lastUpdateLabel = new JLabel("Последнее обновление: --:--:--", JLabel.CENTER);
        lastUpdateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        updatePanel.add(lastUpdateLabel, BorderLayout.CENTER);
        updatePanel.setBorder(BorderFactory.createTitledBorder("Время обновления"));

        topPanel.add(systemStatusPanel);
        topPanel.add(updatePanel);

        JPanel centerPanel = new JPanel(new BorderLayout());

        // Панель статусов компонентов
        JPanel statusPanel = new JPanel(new GridLayout(MODULES.length, 1, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Статусы модулей системы"));

        for (String module : MODULES) {
            JPanel modulePanel = new JPanel(new BorderLayout());

            JLabel moduleLabel = new JLabel(module + ":");
            moduleLabel.setFont(new Font("Arial", Font.BOLD, 12));
            moduleLabel.setPreferredSize(new Dimension(100, 25));

            JLabel statusLabel = new JLabel("Неизвестно", JLabel.CENTER);
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            statusLabel.setOpaque(true);
            statusLabel.setBackground(Color.GRAY);
            statusLabel.setForeground(Color.WHITE);
            statusLabel.setPreferredSize(new Dimension(120, 25));

            statusLabels.put(module, statusLabel);

            modulePanel.add(moduleLabel, BorderLayout.WEST);
            modulePanel.add(statusLabel, BorderLayout.CENTER);
            statusPanel.add(modulePanel);
        }

        // Панель загруженности очередей
        queuePanel = new JPanel(new GridLayout(MODULES.length, 1, 5, 5));
        queuePanel.setBorder(BorderFactory.createTitledBorder("Загруженность очередей"));

        for (String module : MODULES) {
            JPanel queueSizePanel = new JPanel(new BorderLayout());

            JLabel queueLabel = new JLabel(module + ": 0 сообщений");
            queueLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            queueLabel.setForeground(Color.GRAY);

            queueSizeLabels.put(module, queueLabel);

            queueSizePanel.add(queueLabel, BorderLayout.CENTER);
            queuePanel.add(queueSizePanel);
        }

        JSplitPane statusSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, statusPanel, queuePanel);
        statusSplitPane.setResizeWeight(0.5);

        centerPanel.add(statusSplitPane, BorderLayout.CENTER);

        logArea = new JTextArea(15, 80);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Журнал событий"));

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.SOUTH);
    }
}