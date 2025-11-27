package ru.zolotuhin.interfaces;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;

public class SimplexGUI extends JFrame {
    private final JSpinner varSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    private final JSpinner constSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    private final JTable inputTable = new JTable();
    private final JTable resultTable = new JTable();
    private final JTable iterationTable = new JTable();
    private final JRadioButton maxRadio = new JRadioButton("Maximize", true);
    private final JTextArea consoleArea = new JTextArea();
    private double[][] finalTable;
    private int[] basis;

    public SimplexGUI() {
        setTitle("Решатель Симплекс-методом");
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(new JLabel("Переменные:"));
        controlPanel.add(varSpinner);
        controlPanel.add(new JLabel("Ограничения:"));
        controlPanel.add(constSpinner);
        controlPanel.add(maxRadio);
        controlPanel.add(new JRadioButton("Минимизация"));

        JButton updateButton = new JButton("Обновить таблицу");
        updateButton.addActionListener(e -> updateTable());
        controlPanel.add(updateButton);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("Входная таблица:"), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputTable), BorderLayout.CENTER);

        JPanel iterationPanel = new JPanel(new BorderLayout());
        iterationPanel.add(new JLabel("Финальная таблица итераций:"), BorderLayout.NORTH);
        iterationPanel.add(new JScrollPane(iterationTable), BorderLayout.CENTER);

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.add(new JLabel("Решение:"), BorderLayout.NORTH);
        resultPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(new JLabel("Консоль вывода:"), BorderLayout.NORTH);
        consoleArea.setEditable(false);
        consolePanel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);

        JButton solveButton = new JButton("Решить");
        solveButton.addActionListener(e -> solveProblem());

        JSplitPane upperSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, iterationPanel);
        upperSplit.setResizeWeight(0.5);

        JSplitPane middleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperSplit, resultPanel);
        middleSplit.setResizeWeight(0.7);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, middleSplit, consolePanel);
        mainSplit.setResizeWeight(0.8);

        add(controlPanel, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(solveButton, BorderLayout.SOUTH);

        updateTable();
        loadExample();
    }

    private void loadExample() {
        varSpinner.setValue(2);
        constSpinner.setValue(3);
        maxRadio.setSelected(true);
        updateTable();

        DefaultTableModel model = (DefaultTableModel) inputTable.getModel();

        model.setValueAt(1.0, 0, 1);
        model.setValueAt(2.0, 0, 2);

        model.setValueAt(1.0, 1, 1);
        model.setValueAt(1.0, 1, 2);
        model.setValueAt(1.0, 1, 5);

        // -x1 + x2 ≥ -1 → x1 - x2 ≤ 1
        model.setValueAt(1.0, 2, 1);
        model.setValueAt(-1.0, 2, 2);
        model.setValueAt(1.0, 2, 5);

        // x1 + 3x2 ≤ 6
        model.setValueAt(1.0, 3, 1);
        model.setValueAt(3.0, 3, 2);
        model.setValueAt(6.0, 3, 5);
    }

    private void updateTable() {
        int variables = (int) varSpinner.getValue();
        int constraints = (int) constSpinner.getValue();

        String[] columnNames = new String[variables + constraints + 2];
        columnNames[0] = "Type";
        for (int i = 1; i <= variables; i++) {
            columnNames[i] = "x" + i;
        }
        for (int i = 1; i <= constraints; i++) {
            columnNames[variables + i] = "s" + i;
        }
        columnNames[variables + constraints + 1] = "Свободные";

        DefaultTableModel model = new DefaultTableModel(columnNames, constraints + 1) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Double.class;
            }
        };

        inputTable.setModel(model);
        resultTable.setModel(new DefaultTableModel());
        iterationTable.setModel(new DefaultTableModel());
        consoleArea.setText("");
        finalTable = null;
    }

    private void solveProblem() {
        try {
            consoleArea.setText("");
            int variables = (int) varSpinner.getValue();
            int constraints = (int) constSpinner.getValue();

            double[] c = new double[variables];
            for (int i = 0; i < variables; i++) {
                Object value = inputTable.getValueAt(0, i + 1);
                c[i] = value != null ? Double.parseDouble(value.toString()) : 0;
            }

            double[][] A = new double[constraints][variables];
            double[] b = new double[constraints];
            for (int i = 0; i < constraints; i++) {
                for (int j = 0; j < variables; j++) {
                    Object value = inputTable.getValueAt(i + 1, j + 1);
                    A[i][j] = value != null ? Double.parseDouble(value.toString()) : 0;
                }
                Object rhsValue = inputTable.getValueAt(i + 1, variables + constraints + 1);
                b[i] = rhsValue != null ? Double.parseDouble(rhsValue.toString()) : 0;
            }

            SimplexResult result = simplexMethod(c, A, b);
            if (!maxRadio.isSelected()) {
                result = new SimplexResult(result.solution, -result.value);
            }

            displayResult(result);
            displayFinalIteration();

        } catch (Exception ex) {
            consoleArea.append("Ошибка: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayResult(SimplexResult result) {
        String[] columnNames = {"Переменная", "Значение"};
        DefaultTableModel model = new DefaultTableModel(columnNames, result.solution.length + 1);

        for (int i = 0; i < result.solution.length; i++) {
            model.setValueAt("x" + (i + 1), i, 0);
            model.setValueAt(result.solution[i], i, 1);
        }
        model.setValueAt("Значение целевой функции", result.solution.length, 0);
        model.setValueAt(result.value, result.solution.length, 1);

        resultTable.setModel(model);
    }

    private double[][] deepCopy(double[][] matrix) {
        return Arrays.stream(matrix).map(double[]::clone).toArray(double[][]::new);
    }

    private void displayFinalIteration() {
        if (finalTable == null) return;

        int variables = (int) varSpinner.getValue();
        int constraints = (int) constSpinner.getValue();

        String[] columnNames = new String[variables + constraints + 2];
        columnNames[0] = "Базис";
        for (int i = 1; i <= variables; i++) {
            columnNames[i] = "x" + i;
        }
        for (int i = 1; i <= constraints; i++) {
            columnNames[variables + i] = "s" + i;
        }
        columnNames[variables + constraints + 1] = "Свободные";

        Object[][] tableData = new Object[finalTable.length][];
        for (int i = 0; i < finalTable.length; i++) {
            tableData[i] = new Object[finalTable[i].length];
            for (int j = 0; j < finalTable[i].length; j++) {
                tableData[i][j] = finalTable[i][j];
            }
        }

        DefaultTableModel model = new DefaultTableModel(tableData, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Double.class;
            }
        };

        iterationTable.setModel(model);
    }

    private SimplexResult simplexMethod(double[] c, double[][] A, double[] b) {
        int m = A.length;
        int n = A[0].length;
        double[][] table = new double[m + 1][n + m + 1];

        for (int i = 0; i < m; i++) {
            System.arraycopy(A[i], 0, table[i], 0, n);
            table[i][n + i] = 1;
            table[i][n + m] = b[i];
        }

        System.arraycopy(c, 0, table[m], 0, n);
        for (int j = 0; j < n; j++) {
            table[m][j] *= -1;
        }

        basis = new int[m];
        for (int i = 0; i < m; i++) {
            basis[i] = n + i;
        }

        int iteration = 0;
        while (true) {
            iteration++;
            consoleArea.append("\nИтерация " + iteration + ":\n");
            printTableToConsole(table);

            boolean optimal = true;
            for (int j = 0; j < n + m; j++) {
                if (table[m][j] < -1e-10) {
                    optimal = false;
                    break;
                }
            }
            if (optimal) {
                consoleArea.append("\nОптимальное решение найдено!\n");
                this.finalTable = deepCopy(table);
                break;
            }

            int entering = -1;
            double min = 0;
            for (int j = 0; j < n + m; j++) {
                if (table[m][j] < min) {
                    min = table[m][j];
                    entering = j;
                }
            }

            boolean unbounded = true;
            for (int i = 0; i < m; i++) {
                if (table[i][entering] > 1e-10) {
                    unbounded = false;
                    break;
                }
            }
            if (unbounded) {
                throw new RuntimeException("Проблема нерешаема");
            }

            int exiting = -1;
            double minRatio = Double.MAX_VALUE;
            for (int i = 0; i < m; i++) {
                if (table[i][entering] > 1e-10) {
                    double ratio = table[i][n + m] / table[i][entering];
                    if (ratio < minRatio) {
                        minRatio = ratio;
                        exiting = i;
                    }
                }
            }

            consoleArea.append("Входящий: " + (entering < n ? "x" + (entering + 1) : "s" + (entering - n + 1)) +
                    ", Выходящий: s" + (exiting + 1) + "\n");

            basis[exiting] = entering;

            double pivot = table[exiting][entering];
            for (int j = 0; j < n + m + 1; j++) {
                table[exiting][j] /= pivot;
            }

            for (int i = 0; i < m + 1; i++) {
                if (i != exiting) {
                    double factor = table[i][entering];
                    for (int j = 0; j < n + m + 1; j++) {
                        table[i][j] -= factor * table[exiting][j];
                    }
                }
            }
        }

        double[] solution = new double[n];
        for (int i = 0; i < m; i++) {
            if (basis[i] < n) {
                solution[basis[i]] = table[i][n + m];
            }
        }

        return new SimplexResult(solution, table[m][n + m]);
    }

    private void printTableToConsole(double[][] table) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : table) {
            for (double val : row) {
                sb.append(String.format("%8.2f", val));
            }
            sb.append("\n");
        }
        consoleArea.append(sb.toString());
    }

    private static class SimplexResult {
        final double[] solution;
        final double value;

        SimplexResult(double[] solution, double value) {
            this.solution = solution;
            this.value = value;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimplexGUI gui = new SimplexGUI();
            gui.setVisible(true);
        });
    }
}