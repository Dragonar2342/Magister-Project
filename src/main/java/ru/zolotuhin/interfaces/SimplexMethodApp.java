package ru.zolotuhin.interfaces;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;

public class SimplexMethodApp extends JFrame {
    private JSpinner varSpinner;
    private JSpinner eqSpinner;
    private JTable inputTable;
    private JTable resultTable;
    private JButton solveButton;
    private JButton clearButton;
    private JButton updateButton;

    public SimplexMethodApp() {
        setTitle("Симплекс-метод");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Инициализация компонентов
        varSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        eqSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        inputTable = new JTable();
        resultTable = new JTable();

        // Настройка интерфейса
        setupUI();

        // Первоначальное обновление таблицы
        updateTableStructure();
    }

    private void setupUI() {
        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.add(new JLabel("Количество переменных:"));
        controlPanel.add(varSpinner);
        controlPanel.add(new JLabel("Количество ограничений:"));
        controlPanel.add(eqSpinner);

        updateButton = new JButton("Обновить таблицу");
        updateButton.addActionListener(e -> updateTableStructure());
        controlPanel.add(updateButton);

        add(controlPanel, BorderLayout.NORTH);

        // Основная панель с таблицами
        JPanel tablePanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Таблица ввода
        JScrollPane inputScrollPane = new JScrollPane(inputTable);
        inputScrollPane.setBorder(BorderFactory.createTitledBorder("Исходная таблица"));
        tablePanel.add(inputScrollPane);

        // Таблица результатов
        JScrollPane resultScrollPane = new JScrollPane(resultTable);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder("Результат"));
        tablePanel.add(resultScrollPane);

        add(tablePanel, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = new JPanel();
        solveButton = new JButton("Решить");
        solveButton.addActionListener(e -> solveProblem());
        buttonPanel.add(solveButton);

        clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> clearTables());
        buttonPanel.add(clearButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateTableStructure() {
        int variables = (Integer) varSpinner.getValue();
        int equations = (Integer) eqSpinner.getValue();

        // Обновляем таблицу ввода
        DefaultTableModel inputModel = new DefaultTableModel(equations + 2, variables + 2) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return row > 0 && column > 0; // Запрещаем редактирование заголовков
            }
        };

        // Устанавливаем заголовки столбцов
        inputModel.setValueAt("", 0, 0); // Пустой угол
        for (int j = 1; j <= variables; j++) {
            inputModel.setValueAt("x" + j, 0, j);
        }
        inputModel.setValueAt("b", 0, variables + 1);

        // Устанавливаем метки строк
        inputModel.setValueAt("F", 1, 0);
        for (int i = 1; i <= equations; i++) {
            inputModel.setValueAt("x" + (variables + i), i + 1, 0);
        }

        inputTable.setModel(inputModel);

        // Обновляем таблицу результатов (переменные + F + ограничения)
        DefaultTableModel resultModel = new DefaultTableModel(variables + equations + 1, 2) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Заполняем заголовки строк результатов
        resultModel.setValueAt("F", 0, 0);
        for (int i = 1; i <= variables; i++) {
            resultModel.setValueAt("x" + i, i, 0);
        }
        for (int i = 1; i <= equations; i++) {
            resultModel.setValueAt("x" + (variables + i), variables + i, 0);
        }

        resultTable.setModel(resultModel);
    }

    private void solveProblem() {
        try {
            int variables = (Integer) varSpinner.getValue();
            int equations = (Integer) eqSpinner.getValue();

            // Создаем симплекс-таблицу
            double[][] simplexTable = new double[equations + 1][variables + equations + 2];

            // Заполняем целевую функцию (из строки F)
            for (int j = 1; j <= variables; j++) {
                Object value = inputTable.getValueAt(1, j);
                simplexTable[0][j-1] = (value == null || value.toString().isEmpty()) ? 0 :
                        -Double.parseDouble(value.toString());
            }

            // Заполняем ограничения
            for (int i = 1; i <= equations; i++) {
                for (int j = 1; j <= variables; j++) {
                    Object value = inputTable.getValueAt(i + 1, j);
                    simplexTable[i][j-1] = (value == null || value.toString().isEmpty()) ? 0 :
                            Double.parseDouble(value.toString());
                }

                // Slack-переменные
                simplexTable[i][variables + i - 1] = 1;

                // Правая часть ограничения
                Object bValue = inputTable.getValueAt(i + 1, variables + 1);
                simplexTable[i][variables + equations + 1] = (bValue == null || bValue.toString().isEmpty()) ? 0 :
                        Double.parseDouble(bValue.toString());
            }

            // Решаем задачу
            SimplexSolver solver = new SimplexSolver();
            double[] solution = solver.solve(simplexTable, variables, equations);

            // Выводим результаты
            displayResults(solution, variables, equations);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayResults(double[] solution, int variables, int equations) {
        DefaultTableModel resultModel = (DefaultTableModel) resultTable.getModel();
        DecimalFormat df = new DecimalFormat("0.#####");

        // Очищаем предыдущие результаты
        for (int i = 0; i < resultModel.getRowCount(); i++) {
            resultModel.setValueAt("", i, 1);
        }

        // Заполняем новые результаты
        resultModel.setValueAt(df.format(solution[0]), 0, 1); // F
        for (int i = 1; i <= variables; i++) {
            resultModel.setValueAt(df.format(solution[i]), i, 1); // x1, x2, ...
        }

        // Slack-переменные (если нужно их отображать)
        for (int i = 1; i <= equations; i++) {
            resultModel.setValueAt(df.format(solution[variables + i]), variables + i, 1);
        }
    }

    private void clearTables() {
        DefaultTableModel inputModel = (DefaultTableModel) inputTable.getModel();
        DefaultTableModel resultModel = (DefaultTableModel) resultTable.getModel();

        // Очищаем значения, но сохраняем структуру
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            for (int j = 0; j < inputModel.getColumnCount(); j++) {
                inputModel.setValueAt("", i, j);
            }
        }

        for (int i = 0; i < resultModel.getRowCount(); i++) {
            resultModel.setValueAt("", i, 1);
        }

        // Восстанавливаем заголовки
        updateTableStructure();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimplexMethodApp app = new SimplexMethodApp();
            app.setVisible(true);
        });
    }
}


class SimplexSolver {
    public double[] solve(double[][] initialTable, int variables, int equations) {
        int totalVars = variables + equations;
        double[][] table = new double[equations + 1][totalVars + 2];

        // Копируем данные
        for (int i = 0; i <= equations; i++) {
            System.arraycopy(initialTable[i], 0, table[i], 0, initialTable[i].length);
        }

        // Основной цикл симплекс-метода
        while (true) {
            // 1. Нахождение ведущего столбца
            int pivotCol = findPivotColumn(table);
            if (pivotCol == -1) break;

            // 2. Нахождение ведущей строки
            int pivotRow = findPivotRow(table, pivotCol);
            if (pivotRow == -1) throw new RuntimeException("Функция не ограничена");

            // 3. Преобразование Жордана-Гаусса
            pivot(table, pivotRow, pivotCol);
        }

        return extractSolution(table, variables, equations);
    }

    private int findPivotColumn(double[][] table) {
        int pivotCol = -1;
        double min = 0;

        for (int j = 0; j < table[0].length - 1; j++) {
            if (table[0][j] < min) {
                min = table[0][j];
                pivotCol = j;
            }
        }

        return pivotCol;
    }

    private int findPivotRow(double[][] table, int col) {
        int pivotRow = -1;
        double minRatio = Double.MAX_VALUE;

        for (int i = 1; i < table.length; i++) {
            if (table[i][col] > 0) {
                double ratio = table[i][table[0].length - 1] / table[i][col];
                if (ratio < minRatio) {
                    minRatio = ratio;
                    pivotRow = i;
                }
            }
        }

        return pivotRow;
    }

    private void pivot(double[][] table, int row, int col) {
        double pivotValue = table[row][col];
        for (int j = 0; j < table[0].length; j++) {
            table[row][j] /= pivotValue;
        }

        for (int i = 0; i < table.length; i++) {
            if (i != row && table[i][col] != 0) {
                double factor = table[i][col];
                for (int j = 0; j < table[0].length; j++) {
                    table[i][j] -= factor * table[row][j];
                }
            }
        }
    }

    private double[] extractSolution(double[][] table, int variables, int equations) {
        int totalVars = variables + equations;
        double[] solution = new double[variables + 1];

        // Значение целевой функции
        solution[0] = table[0][totalVars];

        // Значения переменных
        for (int j = 0; j < variables; j++) {
            boolean isBasic = false;
            int basicRow = -1;

            for (int i = 1; i <= equations; i++) {
                if (Math.abs(table[i][j] - 1) < 1e-6) {
                    if (basicRow == -1) {
                        basicRow = i;
                        isBasic = true;
                    } else {
                        isBasic = false;
                        break;
                    }
                } else if (Math.abs(table[i][j]) > 1e-6) {
                    isBasic = false;
                    break;
                }
            }

            if (isBasic && basicRow != -1) {
                solution[j + 1] = table[basicRow][totalVars];
            } else {
                solution[j + 1] = 0;
            }
        }

        return solution;
    }
}