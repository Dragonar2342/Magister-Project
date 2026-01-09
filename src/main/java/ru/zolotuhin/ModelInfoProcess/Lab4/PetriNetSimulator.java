package ru.zolotuhin.ModelInfoProcess.Lab4;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class PetriNetSimulator extends JFrame {
    private JTabbedPane tabbedPane;
    private PetriPanel basicPanel;
    private PetriPanel advancedPanel;
    private JTable reachabilityTable;
    private DefaultTableModel tableModel;
    private JButton simulateButton, clearButton, deleteButton;
    private Timer simulationTimer;
    private int simulationStep = 0;
    private JLabel statusLabel;
    private JPanel modePanel;
    private ButtonGroup modeGroup;
    private JRadioButton selectModeButton, addPlaceModeButton, addTransitionModeButton, addArcModeButton, deleteModeButton;

    public PetriNetSimulator() {
        setTitle("Симулятор Сетей Петри");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        basicPanel = new PetriPanel("Базовая задача: Контроль доступа к данным");
        advancedPanel = new PetriPanel("Продвинутая задача: Вычислительная система");

        loadBasicNetwork();
        loadAdvancedNetwork();

        tabbedPane.addTab("Чтение/Запись", basicPanel);
        tabbedPane.addTab("Вычислительная система", advancedPanel);

        add(tabbedPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        simulateButton = createStyledButton("▶ Запустить Симуляцию", new Color(50, 205, 50));
        clearButton = createStyledButton("Очистить таблицу", new Color(220, 20, 60));
        deleteButton = createStyledButton("Удалить выбранное", new Color(178, 34, 34));

        buttonPanel.add(simulateButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(Box.createHorizontalStrut(20));

        controlPanel.add(buttonPanel, BorderLayout.WEST);

        statusLabel = new JLabel("Готово к работе");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        controlPanel.add(statusLabel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.NORTH);

        modePanel = new JPanel();
        modePanel.setLayout(new BoxLayout(modePanel, BoxLayout.Y_AXIS));
        modePanel.setBorder(BorderFactory.createTitledBorder("Режимы работы"));
        modePanel.setPreferredSize(new Dimension(200, 450));

        modeGroup = new ButtonGroup();

        selectModeButton = new JRadioButton("Выбор/Перемещение", true);
        selectModeButton.setActionCommand("SELECT");
        selectModeButton.addActionListener(e -> {
            getCurrentPanel().setMode(PetriPanel.Mode.SELECT);
            statusLabel.setText("Режим: Выбор и перемещение элементов");
        });

        addPlaceModeButton = new JRadioButton("Добавить позицию");
        addPlaceModeButton.setActionCommand("ADD_PLACE");
        addPlaceModeButton.addActionListener(e -> {
            getCurrentPanel().setMode(PetriPanel.Mode.ADD_PLACE);
            statusLabel.setText("Режим: Добавление позиции - кликните на поле");
        });

        addTransitionModeButton = new JRadioButton("Добавить переход");
        addTransitionModeButton.setActionCommand("ADD_TRANSITION");
        addTransitionModeButton.addActionListener(e -> {
            getCurrentPanel().setMode(PetriPanel.Mode.ADD_TRANSITION);
            statusLabel.setText("Режим: Добавление перехода - кликните на поле");
        });

        addArcModeButton = new JRadioButton("Добавить дугу");
        addArcModeButton.setActionCommand("ADD_ARC");
        addArcModeButton.addActionListener(e -> {
            getCurrentPanel().setMode(PetriPanel.Mode.ADD_ARC);
            statusLabel.setText("Режим: Добавление дуги - выберите начальный и конечный элементы");
        });

        deleteModeButton = new JRadioButton("Удалить элемент");
        deleteModeButton.setActionCommand("DELETE");
        deleteModeButton.addActionListener(e -> {
            getCurrentPanel().setMode(PetriPanel.Mode.DELETE);
            statusLabel.setText("Режим: Удаление - кликните на элемент для удаления");
        });

        modeGroup.add(selectModeButton);
        modeGroup.add(addPlaceModeButton);
        modeGroup.add(addTransitionModeButton);
        modeGroup.add(addArcModeButton);
        modeGroup.add(deleteModeButton);

        modePanel.add(Box.createVerticalStrut(10));
        modePanel.add(selectModeButton);
        modePanel.add(Box.createVerticalStrut(10));
        modePanel.add(addPlaceModeButton);
        modePanel.add(Box.createVerticalStrut(10));
        modePanel.add(addTransitionModeButton);
        modePanel.add(Box.createVerticalStrut(10));
        modePanel.add(addArcModeButton);
        modePanel.add(Box.createVerticalStrut(10));
        modePanel.add(deleteModeButton);
        modePanel.add(Box.createVerticalGlue());

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Информация"));
        infoPanel.setPreferredSize(new Dimension(200, 250));

        JLabel infoLabel = new JLabel("<html>"
                + "<h3>Инструкция:</h3>"
                + "<p><b>Выбор/Перемещение:</b><br>Клик - выделить<br>Перетаскивание - переместить</p>"
                + "<p><b>Добавление позиции:</b><br>Клик - добавить в указанное место</p>"
                + "<p><b>Добавление перехода:</b><br>Клик - добавить в указанное место</p>"
                + "<p><b>Добавление дуги:</b><br>1. Клик на начальный элемент<br>2. Клик на конечный элемент</p>"
                + "<p><b>Удаление:</b><br>Клик на элемент для удаления</p>"
                + "<p><b>Изменение меток:</b><br>Двойной клик по позиции</p>"
                + "</html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoPanel.add(infoLabel);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(modePanel, BorderLayout.NORTH);
        leftPanel.add(infoPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Таблица достижимых состояний"));

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reachabilityTable = new JTable(tableModel);
        reachabilityTable.setRowHeight(25);
        reachabilityTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        tableModel.addColumn("Шаг");
        tableModel.addColumn("Состояние сети");
        tableModel.addColumn("Активные переходы");
        tableModel.addColumn("Время");

        JScrollPane tableScroll = new JScrollPane(reachabilityTable);
        tableScroll.setPreferredSize(new Dimension(1400, 180));
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.SOUTH);

        setupEventHandlers();

        simulationTimer = new Timer(1500, e -> performSimulationStep());
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void loadBasicNetwork() {
        basicPanel.addPlace("Ожидание_чтения", 100, 150, 3, new Color(173, 216, 230));
        basicPanel.addPlace("Чтение_данных", 300, 150, 0, new Color(144, 238, 144));
        basicPanel.addPlace("Ожидание_записи", 100, 250, 2, new Color(255, 182, 193));
        basicPanel.addPlace("Запись_данных", 300, 250, 0, new Color(255, 160, 122));
        basicPanel.addPlace("Ресурс_доступен", 500, 200, 1, new Color(221, 160, 221));

        basicPanel.addTransition("Начать_чтение", 200, 150, new Color(100, 149, 237));
        basicPanel.addTransition("Начать_запись", 200, 250, new Color(255, 127, 80));
        basicPanel.addTransition("Закончить_чтение", 400, 150, new Color(60, 179, 113));
        basicPanel.addTransition("Закончить_запись", 400, 250, new Color(199, 21, 133));

        // Дуги
        basicPanel.addArc("Ожидание_чтения", "Начать_чтение");
        basicPanel.addArc("Начать_чтение", "Чтение_данных");
        basicPanel.addArc("Ресурс_доступен", "Начать_чтение");
        basicPanel.addArc("Ожидание_записи", "Начать_запись");
        basicPanel.addArc("Начать_запись", "Запись_данных");
        basicPanel.addArc("Ресурс_доступен", "Начать_запись");
        basicPanel.addArc("Чтение_данных", "Закончить_чтение");
        basicPanel.addArc("Закончить_чтение", "Ожидание_чтения");
        basicPanel.addArc("Закончить_чтение", "Ресурс_доступен");
        basicPanel.addArc("Запись_данных", "Закончить_запись");
        basicPanel.addArc("Закончить_запись", "Ожидание_записи");
        basicPanel.addArc("Закончить_запись", "Ресурс_доступен");
    }

    private void loadAdvancedNetwork() {
        advancedPanel.addPlace("Буфер_ввода1", 50, 100, 2, new Color(173, 216, 230));
        advancedPanel.addPlace("Буфер_ввода2", 50, 200, 2, new Color(135, 206, 250));
        advancedPanel.addPlace("Канал_данных", 200, 150, 0, new Color(152, 251, 152));
        advancedPanel.addPlace("Процессор1", 350, 100, 0, new Color(255, 182, 193));
        advancedPanel.addPlace("Процессор2", 350, 150, 0, new Color(255, 160, 122));
        advancedPanel.addPlace("Процессор3", 350, 200, 0, new Color(221, 160, 221));
        advancedPanel.addPlace("Буфер_вывода", 500, 150, 0, new Color(240, 230, 140));

        advancedPanel.addTransition("Прием_данных1", 150, 100, new Color(70, 130, 180));
        advancedPanel.addTransition("Прием_данных2", 150, 200, new Color(70, 130, 180));
        advancedPanel.addTransition("Распределение", 300, 150, new Color(60, 179, 113));
        advancedPanel.addTransition("Отправка_результата", 450, 150, new Color(255, 69, 0));

        advancedPanel.addArc("Буфер_ввода1", "Прием_данных1");
        advancedPanel.addArc("Буфер_ввода2", "Прием_данных2");
        advancedPanel.addArc("Прием_данных1", "Канал_данных");
        advancedPanel.addArc("Прием_данных2", "Канал_данных");
        advancedPanel.addArc("Канал_данных", "Распределение");
        advancedPanel.addArc("Распределение", "Процессор1");
        advancedPanel.addArc("Распределение", "Процессор2");
        advancedPanel.addArc("Распределение", "Процессор3");
        advancedPanel.addArc("Процессор1", "Отправка_результата");
        advancedPanel.addArc("Процессор2", "Отправка_результата");
        advancedPanel.addArc("Процессор3", "Отправка_результата");
        advancedPanel.addArc("Отправка_результата", "Буфер_вывода");
    }

    private void setupEventHandlers() {
        simulateButton.addActionListener(e -> {
            if (simulationTimer.isRunning()) {
                simulationTimer.stop();
                simulateButton.setText("▶ Запустить Симуляцию");
                statusLabel.setText("Симуляция остановлена");
            } else {
                simulationStep = 0;
                tableModel.setRowCount(0);
                simulationTimer.start();
                simulateButton.setText("⏸ Остановить Симуляцию");
                statusLabel.setText("Симуляция запущена...");
            }
        });

        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Очистить таблицу достижимости?",
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                tableModel.setRowCount(0);
                simulationStep = 0;
                statusLabel.setText("Таблица очищена");
            }
        });

        deleteButton.addActionListener(e -> {
            PetriPanel currentPanel = getCurrentPanel();
            currentPanel.deleteSelectedElement();
            statusLabel.setText("Выбранный элемент удален");
        });

        tabbedPane.addChangeListener(e -> {
            statusLabel.setText("Активна вкладка: " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
            updateModeButtons();
        });
    }

    private PetriPanel getCurrentPanel() {
        return (PetriPanel) tabbedPane.getSelectedComponent();
    }

    private void updateModeButtons() {
        PetriPanel currentPanel = getCurrentPanel();
        PetriPanel.Mode currentMode = currentPanel.getCurrentMode();

        switch (currentMode) {
            case SELECT:
                selectModeButton.setSelected(true);
                break;
            case ADD_PLACE:
                addPlaceModeButton.setSelected(true);
                break;
            case ADD_TRANSITION:
                addTransitionModeButton.setSelected(true);
                break;
            case ADD_ARC:
                addArcModeButton.setSelected(true);
                break;
            case DELETE:
                deleteModeButton.setSelected(true);
                break;
            case MOVE:
                selectModeButton.setSelected(true);
                break;
        }
    }

    private void performSimulationStep() {
        PetriPanel currentPanel = getCurrentPanel();

        currentPanel.resetAllColors();

        ArrayList<Transition> enabledTransitions = currentPanel.getEnabledTransitions();

        if (enabledTransitions.isEmpty()) {
            simulationTimer.stop();
            simulateButton.setText("▶ Запустить Симуляцию");
            statusLabel.setText("Симуляция завершена - нет доступных переходов");
            JOptionPane.showMessageDialog(this,
                    "Симуляция завершена!\nВсего шагов: " + simulationStep,
                    "Завершение",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Random rand = new Random();
        Transition selectedTransition = enabledTransitions.get(rand.nextInt(enabledTransitions.size()));

        selectedTransition.setActiveColor(new Color(255, 215, 0)); // Золотой цвет

        for (Place p : currentPanel.getInputPlaces(selectedTransition.name)) {
            p.setActiveColor(new Color(255, 140, 0)); // Оранжевый
        }

        currentPanel.fireTransition(selectedTransition);

        String marking = currentPanel.getCurrentMarking();
        String activeTransitions = currentPanel.getActiveTransitionsNames();

        String timeStamp = String.format("%02d:%02d",
                (simulationStep / 60) % 60,
                simulationStep % 60);
        tableModel.addRow(new Object[]{
                simulationStep + 1,
                marking,
                activeTransitions,
                timeStamp
        });

        if (reachabilityTable.getRowCount() > 0) {
            reachabilityTable.scrollRectToVisible(
                    reachabilityTable.getCellRect(reachabilityTable.getRowCount()-1, 0, true)
            );
        }

        simulationStep++;
        currentPanel.repaint();
        statusLabel.setText(String.format("Шаг %d выполнен. Активен переход: %s",
                simulationStep, selectedTransition.name));

        if (simulationStep >= 15) {
            simulationTimer.stop();
            simulateButton.setText("▶ Запустить Симуляцию");
            statusLabel.setText("Симуляция завершена (максимум 15 шагов)");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            PetriNetSimulator simulator = new PetriNetSimulator();
            simulator.setLocationRelativeTo(null);
            simulator.setVisible(true);
        });
    }
}