package ru.zolotuhin.ParrerelMethods.Lab6;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusWindow extends JPanel implements Runnable {
    private Thread thread;
    private final AtomicBoolean systemRunning;
    private final ConcurrentHashMap<String, JLabel> statusLabels;
    private final ConcurrentHashMap<String, JProgressBar> progressBars;
    private final ConcurrentHashMap<String, Long> lastUpdateTimes;

    private JTextArea logArea;
    private JLabel systemStatusLabel;
    private JLabel lastUpdateLabel;
    private JPanel mainPanel;
    private SimpleDateFormat timeFormat;

    private static final int[] PORTS = {5001, 5002, 5003, 5004, 5005};
    private static final String[] MODULES = {"Автопилот", "Камера", "Парковочное место", "Тормоз", "Руль"};

    public StatusWindow(AtomicBoolean systemRunning) {
        this.systemRunning = systemRunning;
        this.statusLabels = new ConcurrentHashMap<>();
        this.progressBars = new ConcurrentHashMap<>();
        this.lastUpdateTimes = new ConcurrentHashMap<>();

        initializeGUI();
        timeFormat = new SimpleDateFormat("HH:mm:ss");

        long currentTime = System.currentTimeMillis();
        for (String module : MODULES) {
            lastUpdateTimes.put(module, currentTime);
        }
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

            JButton startButton = new JButton("Запуск монитора");
            JButton stopButton = new JButton("Остановка");
            JButton refreshButton = new JButton("Обновить статус");
            JButton clearLogButton = new JButton("Очистить логи");
            JButton testAllButton = new JButton("Тест всех систем");

            startButton.addActionListener(e -> {
                statusWindow.start();
                statusWindow.logMessage("Ручной запуск монитора системы");
            });

            stopButton.addActionListener(e -> {
                statusWindow.interrupt();
                statusWindow.logMessage("Ручная остановка монитора");
            });

            refreshButton.addActionListener(e -> {
                statusWindow.refreshAllStatuses();
                statusWindow.logMessage("Ручное обновление статусов");
            });

            clearLogButton.addActionListener(e -> {
                statusWindow.clearLog();
                statusWindow.logMessage("Очистка журнала логов");
            });

            testAllButton.addActionListener(e -> {
                statusWindow.testAllConnections();
                statusWindow.logMessage("Запуск теста всех соединений");
            });

            controlPanel.add(startButton);
            controlPanel.add(stopButton);
            controlPanel.add(refreshButton);
            controlPanel.add(clearLogButton);
            controlPanel.add(testAllButton);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(statusWindow, BorderLayout.CENTER);

            frame.setSize(1000, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            statusWindow.start();
        });
    }

    public void start() {
        if (thread == null || !thread.isAlive()) {
            systemRunning.set(true);
            thread = new Thread(this, "StatusWindow-Thread");
            thread.start();
            updateSystemStatus("Активен", Color.GREEN);
            logMessage("Монитор системы запущен");
        }
    }

    public void interrupt() {
        systemRunning.set(false);
        if (thread != null) {
            thread.interrupt();
        }
        updateSystemStatus("Неактивен", Color.RED);
        logMessage("Монитор системы остановлен");
    }

    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    @Override
    public void run() {
        logMessage("Запуск мониторинга системы автоматической парковки");

        testAllConnections();

        while (systemRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                checkModuleConnections();
                checkLastUpdateTimes();
                updateLastUpdateTime();
                Thread.sleep(3000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logMessage("Мониторинг системы остановлен");
        updateSystemStatus("Неактивен", Color.RED);
    }

    private void checkModuleConnections() {
        for (int i = 0; i < PORTS.length; i++) {
            final String moduleName = MODULES[i];
            final int port = PORTS[i];

            new Thread(() -> {
                boolean isConnected = checkPortConnection("localhost", port);
                SwingUtilities.invokeLater(() -> {
                    if (isConnected) {
                        updateModuleStatus(moduleName, "Подключен", Color.GREEN);
                        lastUpdateTimes.put(moduleName, System.currentTimeMillis());
                    } else {
                        updateModuleStatus(moduleName, "Отключен", Color.RED);
                    }
                });
            }).start();
        }
    }

    private boolean checkPortConnection(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkLastUpdateTimes() {
        long currentTime = System.currentTimeMillis();
        for (String module : MODULES) {
            Long lastUpdate = lastUpdateTimes.get(module);
            if (lastUpdate != null) {
                long timeDiff = currentTime - lastUpdate;
                if (timeDiff > 10000) { // 10 секунд без обновления
                    SwingUtilities.invokeLater(() -> {
                        updateModuleStatus(module, "Нет данных", Color.ORANGE);
                    });
                }
            }
        }
    }

    public void updateModuleStatus(String module, String status, String additionalInfo) {
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = statusLabels.get(module);
            if (statusLabel != null) {
                statusLabel.setText(status + " - " + additionalInfo);

                Color color;
                if (status.contains("Подключен") || status.contains("Активен")) {
                    color = Color.GREEN;
                } else if (status.contains("Отключен") || status.contains("Ошибка")) {
                    color = Color.RED;
                } else if (status.contains("Нет данных")) {
                    color = Color.ORANGE;
                } else {
                    color = Color.GRAY;
                }

                statusLabel.setBackground(color);
                if (color == Color.RED || color == Color.ORANGE) {
                    statusLabel.setForeground(Color.WHITE);
                } else {
                    statusLabel.setForeground(Color.BLACK);
                }
            }

            if (!status.equals("Подключен")) { // Логируем только проблемные статусы
                logMessage(module + ": " + status + " - " + additionalInfo);
            }
        });
    }

    public void updateModuleStatus(String module, String status, Color color) {
        updateModuleStatus(module, status, getStatusDescription(status));
    }

    private String getStatusDescription(String status) {
        switch (status) {
            case "Подключен": return "Связь установлена";
            case "Отключен": return "Связь отсутствует";
            case "Нет данных": return "Данные не обновляются";
            case "Активен": return "Работает нормально";
            case "Ошибка": return "Обнаружена проблема";
            default: return "Неизвестный статус";
        }
    }

    public void updateProgress(String module, int progress) {
        SwingUtilities.invokeLater(() -> {
            JProgressBar progressBar = progressBars.get(module);
            if (progressBar != null) {
                progressBar.setValue(progress);

                if (progress < 30) {
                    progressBar.setForeground(Color.RED);
                } else if (progress < 70) {
                    progressBar.setForeground(Color.ORANGE);
                } else {
                    progressBar.setForeground(Color.GREEN);
                }
            }
        });
    }

    public void testAllConnections() {
        logMessage("=== ЗАПУСК ТЕСТА ВСЕХ СИСТЕМ ===");

        for (String module : MODULES) {
            updateModuleStatus(module, "Тестирование...", Color.ORANGE);
            updateProgress(module, 0);
        }

        new Thread(() -> {
            try {
                for (int progress = 0; progress <= 100; progress += 10) {
                    for (String module : MODULES) {
                        updateProgress(module, progress);
                    }
                    Thread.sleep(200);
                }

                // Финальная проверка соединений
                checkModuleConnections();

                logMessage("=== ТЕСТ СИСТЕМ ЗАВЕРШЕН ===");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void refreshAllStatuses() {
        logMessage("Принудительное обновление статусов всех систем");
        checkModuleConnections();
    }

    private void updateSystemStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            systemStatusLabel.setText(status);
            systemStatusLabel.setBackground(color);
            if (color == Color.RED || color == Color.ORANGE) {
                systemStatusLabel.setForeground(Color.WHITE);
            } else {
                systemStatusLabel.setForeground(Color.BLACK);
            }
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
        systemStatusLabel = new JLabel("Неактивен", JLabel.CENTER);
        systemStatusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        systemStatusLabel.setOpaque(true);
        systemStatusLabel.setBackground(Color.RED);
        systemStatusLabel.setForeground(Color.WHITE);
        systemStatusLabel.setPreferredSize(new Dimension(200, 50));
        systemStatusPanel.add(systemStatusLabel, BorderLayout.CENTER);
        systemStatusPanel.setBorder(BorderFactory.createTitledBorder("Общий статус системы"));

        JPanel updatePanel = new JPanel(new BorderLayout());
        lastUpdateLabel = new JLabel("Последнее обновление: --:--:--", JLabel.CENTER);
        lastUpdateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        updatePanel.add(lastUpdateLabel, BorderLayout.CENTER);
        updatePanel.setBorder(BorderFactory.createTitledBorder("Время обновления"));

        topPanel.add(systemStatusPanel);
        topPanel.add(updatePanel);

        mainPanel = new JPanel(new GridLayout(MODULES.length, 2, 10, 5));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Статусы модулей системы"));

        for (String module : MODULES) {
            addModuleRow(module);
        }

        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setPreferredSize(new Dimension(800, 300));

        JPanel progressPanel = new JPanel(new GridLayout(MODULES.length, 1, 5, 5));
        progressPanel.setBorder(BorderFactory.createTitledBorder("Прогресс работы модулей"));

        for (String module : MODULES) {
            addProgressBar(module, progressPanel);
        }

        JScrollPane progressScrollPane = new JScrollPane(progressPanel);
        progressScrollPane.setPreferredSize(new Dimension(800, 150));

        logArea = new JTextArea(15, 80);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Журнал событий системы"));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(mainScrollPane, BorderLayout.NORTH);
        centerPanel.add(progressScrollPane, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.SOUTH);
    }

    private void addModuleRow(String module) {
        JPanel rowPanel = new JPanel(new BorderLayout());

        JLabel moduleLabel = new JLabel(module + ":");
        moduleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        moduleLabel.setPreferredSize(new Dimension(150, 25));

        JLabel statusLabel = new JLabel("Неизвестно", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.GRAY);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setPreferredSize(new Dimension(200, 25));

        statusLabels.put(module, statusLabel);

        rowPanel.add(moduleLabel, BorderLayout.WEST);
        rowPanel.add(statusLabel, BorderLayout.CENTER);

        mainPanel.add(rowPanel);
    }

    private void addProgressBar(String module, JPanel panel) {
        JPanel progressRow = new JPanel(new BorderLayout());

        JLabel progressLabel = new JLabel(module + ":");
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        progressLabel.setPreferredSize(new Dimension(120, 20));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setForeground(Color.GRAY);

        progressBars.put(module, progressBar);

        progressRow.add(progressLabel, BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);

        panel.add(progressRow);
    }

    public void updateAutopilotStatus(String status, String additionalInfo) {
        updateModuleStatus("Автопилот", status, additionalInfo);
    }

    public void updateCameraStatus(String status, String additionalInfo) {
        updateModuleStatus("Камера", status, additionalInfo);
    }

    public void updateParkingSpotStatus(String status, String additionalInfo) {
        updateModuleStatus("Парковочное место", status, additionalInfo);
    }

    public void updateBrakeStatus(String status, String additionalInfo) {
        updateModuleStatus("Тормоз", status, additionalInfo);
    }

    public void updateSteeringStatus(String status, String additionalInfo) {
        updateModuleStatus("Руль", status, additionalInfo);
    }

    public ConcurrentHashMap<String, JLabel> getStatusLabels() {
        return statusLabels;
    }

    public ConcurrentHashMap<String, JProgressBar> getProgressBars() {
        return progressBars;
    }
}