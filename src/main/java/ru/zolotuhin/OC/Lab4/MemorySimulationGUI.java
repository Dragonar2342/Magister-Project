package ru.zolotuhin.OC.Lab4;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MemorySimulationGUI extends JFrame {
    private final MemorySystem memorySystem;
    private final TaskGenerator taskGenerator;
    private TaskGenerator.TaskSequence currentScenario;

    private JTextArea logArea;
    private JPanel memoryVisualizationPanel;
    private JLabel[] pageFrameLabels;
    private JPanel statsPanel;
    private JProgressBar memoryUsageBar;
    private JLabel statsLabel;
    private JButton startTestButton;
    private JButton nextStepButton;
    private JButton autoTestButton;
    private JButton resetButton;
    private Timer autoTestTimer;
    private int currentOperationIndex;

    public MemorySimulationGUI() {
        memorySystem = new MemorySystem(4096, 16); // 16 страниц по 4KB
        taskGenerator = new TaskGenerator();

        setTitle("Симуляция управления памятью");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initComponents();

        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);

        memoryVisualizationPanel = new JPanel();
        memoryVisualizationPanel.setLayout(new GridLayout(4, 4, 5, 5));
        memoryVisualizationPanel.setBorder(BorderFactory.createTitledBorder(
                "Физическая память (" + memorySystem.getPhysicalPages() + " страничных кадров)"));

        pageFrameLabels = new JLabel[memorySystem.getPhysicalPages()];
        for (int i = 0; i < memorySystem.getPhysicalPages(); i++) {
            pageFrameLabels[i] = createMemoryCell("Свободно", Color.LIGHT_GRAY, i);
            memoryVisualizationPanel.add(pageFrameLabels[i]);
        }

        statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Статистика памяти"));

        memoryUsageBar = new JProgressBar(0, 100);
        memoryUsageBar.setStringPainted(true);
        memoryUsageBar.setPreferredSize(new Dimension(300, 25));

        statsLabel = new JLabel("Память не инициализирована");
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        statsPanel.add(memoryUsageBar, BorderLayout.NORTH);
        statsPanel.add(statsLabel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());

        startTestButton = new JButton("Генерировать тест");
        nextStepButton = new JButton("Следующий шаг");
        autoTestButton = new JButton("Автотест");
        resetButton = new JButton("Сброс");

        startTestButton.addActionListener(e -> startNewTest());
        nextStepButton.addActionListener(e -> executeNextStep());
        autoTestButton.addActionListener(e -> toggleAutoTest());
        resetButton.addActionListener(e -> resetSimulation());

        nextStepButton.setEnabled(false);
        autoTestButton.setEnabled(false);

        controlPanel.add(startTestButton);
        controlPanel.add(nextStepButton);
        controlPanel.add(autoTestButton);
        controlPanel.add(resetButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(memoryVisualizationPanel, BorderLayout.CENTER);
        topPanel.add(statsPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.CENTER);
        add(logScroll, BorderLayout.SOUTH);
    }

    private JLabel createMemoryCell(String text, Color color, int frameId) {
        JLabel label = new JLabel("<html><center>Кадр " + frameId + "<br>" + text + "</center></html>",
                SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(color);
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        label.setFont(new Font("Arial", Font.BOLD, 11));
        label.setPreferredSize(new Dimension(100, 80));
        return label;
    }

    private void startNewTest() {
        resetSimulation();

        TaskGenerator.TaskGeneratorConfig config = new TaskGenerator.TaskGeneratorConfig()
                .setProcessRange(3, 5)
                .setMemoryRange(2, 8)
                .setOperationsRange(4, 8);

        currentScenario = taskGenerator.generateTestScenario(config);
        currentOperationIndex = 0;

        log("=== Новый тестовый сценарий сгенерирован ===");
        log("Количество задач: " + currentScenario.tasks.size());
        log("Количество операций: " + currentScenario.operations.size());
        log("==========================================");

        nextStepButton.setEnabled(true);
        autoTestButton.setEnabled(true);

        updateVisuals();
    }

    private void executeNextStep() {
        if (currentScenario == null || currentOperationIndex >= currentScenario.operations.size()) {
            log("=== Тестовый сценарий завершен ===");
            nextStepButton.setEnabled(false);
            autoTestButton.setEnabled(false);
            if (autoTestTimer != null && autoTestTimer.isRunning()) {
                autoTestTimer.stop();
                autoTestButton.setText("Автотест");
            }
            return;
        }

        TaskGenerator.TaskOperation operation = currentScenario.operations.get(currentOperationIndex);

        log("\n[Шаг " + (currentOperationIndex + 1) + "] " + operation.description);

        switch (operation.type) {
            case CREATE:
                handleCreateOperation(operation);
                break;
            case ACCESS:
                handleAccessOperation(operation);
                break;
            case TERMINATE:
                handleTerminateOperation(operation);
                break;
        }

        currentOperationIndex++;
        updateVisuals();
    }

    private void handleCreateOperation(TaskGenerator.TaskOperation operation) {
        TaskGenerator.GeneratedTask task = findTaskById(operation.pid);
        if (task != null) {
            memorySystem.createProcess(task.pid, task.name, task.color, task.memoryRequirement);
        }
    }

    private void handleAccessOperation(TaskGenerator.TaskOperation operation) {
        memorySystem.allocateMemory(operation.pid, operation.virtualPage);
    }

    private void handleTerminateOperation(TaskGenerator.TaskOperation operation) {
        memorySystem.terminateProcess(operation.pid);
    }

    private TaskGenerator.GeneratedTask findTaskById(int pid) {
        if (currentScenario == null) return null;

        for (TaskGenerator.GeneratedTask task : currentScenario.tasks) {
            if (task.pid == pid) {
                return task;
            }
        }
        return null;
    }

    private void toggleAutoTest() {
        if (autoTestTimer == null) {
            autoTestTimer = new Timer(1000, e -> executeNextStep());
        }

        if (autoTestTimer.isRunning()) {
            autoTestTimer.stop();
            autoTestButton.setText("Автотест");
            log("=== Автотест приостановлен ===");
        } else {
            autoTestTimer.start();
            autoTestButton.setText("Стоп");
            log("=== Автотест запущен ===");
        }
    }

    private void updateVisuals() {
        String[] memorySnapshot = memorySystem.getPhysicalMemorySnapshot();

        for (int i = 0; i < memorySnapshot.length; i++) {
            if (memorySnapshot[i] != null) {
                Color frameColor = Color.GRAY;
                String frameText = memorySnapshot[i];

                if (memorySnapshot[i].startsWith("P")) {
                    try {
                        int colonIndex = memorySnapshot[i].indexOf(":");
                        if (colonIndex > 0) {
                            String pidStr = memorySnapshot[i].substring(1, colonIndex);
                            int pid = Integer.parseInt(pidStr);

                            // Ищем процесс для получения цвета
                            TaskGenerator.GeneratedTask task = findTaskById(pid);
                            if (task != null) {
                                frameColor = task.color;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Оставляем серый цвет
                    }
                }

                pageFrameLabels[i].setText("<html><center>Кадр " + i + "<br>" + frameText + "</center></html>");
                pageFrameLabels[i].setBackground(frameColor);
            } else {
                pageFrameLabels[i].setText("<html><center>Кадр " + i + "<br>Свободно</center></html>");
                pageFrameLabels[i].setBackground(Color.LIGHT_GRAY);
            }
        }

        MemorySystem.MemoryStats stats = memorySystem.getMemoryStats();
        memoryUsageBar.setValue((int) stats.memoryUtilization);

        statsLabel.setText(String.format(
                "<html>" +
                        "Использовано: %d/%d страниц (%.1f%%)<br>" +
                        "Page Faults: %d<br>" +
                        "Замещений: %d<br>" +
                        "Активных процессов: %d" +
                        "</html>",
                stats.usedPages, stats.totalPages, stats.memoryUtilization,
                stats.pageFaults, stats.pageReplacements,
                memorySystem.getActiveProcesses().size()
        ));

        updateEventLog();
    }

    private void updateEventLog() {
        List<MemorySystem.MemoryEvent> events = memorySystem.getEventLog();
        if (!events.isEmpty()) {
            MemorySystem.MemoryEvent lastEvent = events.get(events.size() - 1);
        }
    }

    private void resetSimulation() {
        memorySystem.clearMemory();
        taskGenerator.reset();
        currentScenario = null;
        currentOperationIndex = 0;

        if (autoTestTimer != null && autoTestTimer.isRunning()) {
            autoTestTimer.stop();
            autoTestButton.setText("Автотест");
        }

        nextStepButton.setEnabled(false);
        autoTestButton.setEnabled(false);

        log("=== Симуляция сброшена ===");
        updateVisuals();
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MemorySimulationGUI simulation = new MemorySimulationGUI();
            simulation.setVisible(true);
        });
    }
}