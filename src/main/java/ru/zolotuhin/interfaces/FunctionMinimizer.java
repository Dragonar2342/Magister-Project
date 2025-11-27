package ru.zolotuhin.interfaces;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import java.awt.geom.Ellipse2D;

public class FunctionMinimizer extends JFrame {
    private final JTextField aField, bField;
    private final JPanel chartPanel;
    private final XYSeries functionSeries;
    private final XYSeries intermediatePointsSeries;
    private final XYSeries finalPointSeries;
    private final JTextArea consoleArea;

    public FunctionMinimizer() {
        setTitle("Поиск минимума функции x² + 5x");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        controlPanel.add(new JLabel("Диапазон:"));
        aField = new JTextField("-10", 5);
        bField = new JTextField("5", 5);

        DocumentListener documentListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateFunctionPlot(); }
            @Override public void removeUpdate(DocumentEvent e) { updateFunctionPlot(); }
            @Override public void changedUpdate(DocumentEvent e) { updateFunctionPlot(); }
        };

        aField.getDocument().addDocumentListener(documentListener);
        bField.getDocument().addDocumentListener(documentListener);

        controlPanel.add(new JLabel("a:"));
        controlPanel.add(aField);
        controlPanel.add(new JLabel("b:"));
        controlPanel.add(bField);

        JButton goldenRatioButton = new JButton("Метод золотого сечения");
        goldenRatioButton.addActionListener(new MethodButtonListener("golden"));
        controlPanel.add(goldenRatioButton);

        JButton fibonacciButton = new JButton("Метод Фибоначчи");
        fibonacciButton.addActionListener(new MethodButtonListener("fibonacci"));
        controlPanel.add(fibonacciButton);

        JButton powellButton = new JButton("Метод Пауэлла");
        powellButton.addActionListener(new MethodButtonListener("powell"));
        controlPanel.add(powellButton);

        JButton newtonButton = new JButton("Метод Ньютона");
        newtonButton.addActionListener(new MethodButtonListener("newton"));
        controlPanel.add(newtonButton);

        JButton cubicButton = new JButton("Метод кубической аппроксимации");
        cubicButton.addActionListener(new MethodButtonListener("cubic"));
        controlPanel.add(cubicButton);

        add(controlPanel, BorderLayout.NORTH);

        functionSeries = new XYSeries("Функция");
        intermediatePointsSeries = new XYSeries("Промежуточные точки");
        finalPointSeries = new XYSeries("Найденный минимум");

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(functionSeries);
        dataset.addSeries(intermediatePointsSeries);
        dataset.addSeries(finalPointSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Функция x² + 5x", "x", "f(x)", dataset);
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Функция
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);

        // Промежуточные точки
        renderer.setSeriesPaint(1, Color.RED);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShape(1, new Ellipse2D.Double(-3, -3, 6, 6));
        renderer.setSeriesFillPaint(1, new Color(0, 0, 0, 0)); // Прозрачная заливка
        renderer.setSeriesOutlinePaint(1, Color.RED); // Красная граница

        // Финальная точка
        renderer.setSeriesPaint(2, Color.BLACK);
        renderer.setSeriesLinesVisible(2, false);
        renderer.setSeriesShapesVisible(2, true);
        renderer.setSeriesShape(2, new Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesFillPaint(2, Color.BLACK); // Черная заливка

        plot.setRenderer(renderer);

        chartPanel = new ChartPanel(chart);

        consoleArea = new JTextArea(10, 30);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                chartPanel, consoleScrollPane);
        splitPane.setResizeWeight(0.7);

        add(splitPane, BorderLayout.CENTER);

        updateFunctionPlot();
    }

    private void updateFunctionPlot() {
        try {
            functionSeries.clear();
            double a = Double.parseDouble(aField.getText());
            double b = Double.parseDouble(bField.getText());

            if (a >= b) {
                JOptionPane.showMessageDialog(this,
                        "Значение 'a' должно быть меньше 'b'",
                        "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double step = (b - a) / 100;
            for (double x = a; x <= b; x += step) {
                functionSeries.add(x, f(x));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private double f(double x) {
        return x * x + 5 * x;
    }

    private double df(double x) {
        return 2 * x + 5;
    }

    private double ddf(double x) {
        return 2;
    }

    private class MethodButtonListener implements ActionListener {
        private final String method;

        public MethodButtonListener(String method) {
            this.method = method;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                intermediatePointsSeries.clear();
                finalPointSeries.clear();
                consoleArea.setText("");

                double a = Double.parseDouble(aField.getText());
                double b = Double.parseDouble(bField.getText());
                double epsilon = 0.01;
                double minX = 0;

                consoleArea.append(String.format("%-40s%-15s%-15s%-15s%-15s\n",
                        "Метод", "Итерация", "x", "f(x)","Точность"));
                consoleArea.append("----------------------------------------------------------------\n");

                minX = switch (method) {
                    case "golden" -> goldenRatio(a, b, epsilon);
                    case "fibonacci" -> fibonacci(a, b, epsilon);
                    case "powell" -> powell(a, b, epsilon);
                    case "newton" -> newton(b, epsilon);
                    case "cubic" -> cubicApproximation(a, b, epsilon);
                    default -> minX;
                };

                finalPointSeries.add(minX, f(minX));
                chartPanel.repaint();

                consoleArea.append("----------------------------------------------------------------\n");
                consoleArea.append(String.format("Найденный минимум: x = %.6f, f(x) = %.6f\n", minX, f(minX)));

                JOptionPane.showMessageDialog(FunctionMinimizer.this,
                        String.format("Метод: %s\nМинимум: x = %.4f, f(x) = %.4f",
                                getMethodName(method), minX, f(minX)),
                        "Результат", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(FunctionMinimizer.this,
                        "Пожалуйста, введите корректные числовые значения для a и b",
                        "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String getMethodName(String method) {
            return switch (method) {
                case "golden" -> "Золотого сечения";
                case "fibonacci" -> "Фибоначчи";
                case "powell" -> "Пауэлла";
                case "newton" -> "Ньютона";
                case "cubic" -> "Кубической аппроксимации";
                default -> "";
            };
        }
    }

    private double goldenRatio(double a, double b, double epsilon) {
        double phi = (1 + Math.sqrt(5)) / 2;
        double x1, x2;
        int iteration = 0;

        while (Math.abs(b - a) > epsilon) {
            iteration++;
            x1 = b - (b - a) / phi;
            x2 = a + (b - a) / phi;

            if (f(x1) >= f(x2)) {
                a = x1;
            } else {
                b = x2;
            }

            double currentX = (a + b) / 2;
            double precision = Math.abs(x2 - x1);
            intermediatePointsSeries.add(currentX, f(currentX));
            consoleArea.append(String.format("%-40s%-15d%-15.6f%-15.6f%-15.6f\n",
                    "Метод золотого сечения", iteration, currentX, f(currentX), precision));
        }

        return (a + b) / 2;
    }

    private double fibonacci(double a, double b, double epsilon) {
        int n = 1;
        while ((b - a) / fibonacci(n) > epsilon) {
            n++;
        }

        double x1 = a + (b - a) * fibonacci(n - 2) / fibonacci(n);
        double x2 = a + (b - a) * fibonacci(n - 1) / fibonacci(n);
        double f1 = f(x1);
        double f2 = f(x2);
        int iteration = 0;

        for (int k = 1; k <= n - 1; k++) {
            iteration++;
            if (f1 < f2) {
                b = x2;
                x2 = x1;
                f2 = f1;
                x1 = a + (b - a) * fibonacci(n - k - 1) / fibonacci(n - k + 1);
                f1 = f(x1);
            } else {
                a = x1;
                x1 = x2;
                f1 = f2;
                x2 = a + (b - a) * fibonacci(n - k) / fibonacci(n - k + 1);
                f2 = f(x2);
            }
            double currentX = (a + b) / 2;
            double precision = Math.abs(x2 - x1);
            intermediatePointsSeries.add(currentX, f(currentX));
            consoleArea.append(String.format("%-40s%-15d%-15.6f%-15.6f%-15.6f\n",
                    "Метод Фибоначчи", iteration, currentX, f(currentX), precision));
        }

        return (a + b) / 2;
    }

    private int fibonacci(int n) {
        if (n <= 1) return n;
        int a = 0, b = 1, c;
        for (int i = 2; i <= n; i++) {
            c = a + b;
            a = b;
            b = c;
        }
        return b;
    }

    private double powell(double a, double b, double epsilon) {
        double x0 = a;
        double x1 = (a + b) / 2;
        double x2 = b;
        int iteration = 0;

        while (Math.abs(x2 - x0) > epsilon) {
            iteration++;
            double[][] A = {
                    {x0 * x0, x0, 1},
                    {x1 * x1, x1, 1},
                    {x2 * x2, x2, 1}
            };
            double[] B = {f(x0), f(x1), f(x2)};

            double det = A[0][0]*(A[1][1]*A[2][2] - A[1][2]*A[2][1]) -
                    A[0][1]*(A[1][0]*A[2][2] - A[1][2]*A[2][0]) +
                    A[0][2]*(A[1][0]*A[2][1] - A[1][1]*A[2][0]);

            double invDet = 1.0 / det;
            double[][] invA = new double[3][3];

            invA[0][0] = (A[1][1]*A[2][2] - A[1][2]*A[2][1]) * invDet;
            invA[0][1] = (A[0][2]*A[2][1] - A[0][1]*A[2][2]) * invDet;
            invA[0][2] = (A[0][1]*A[1][2] - A[0][2]*A[1][1]) * invDet;
            invA[1][0] = (A[1][2]*A[2][0] - A[1][0]*A[2][2]) * invDet;
            invA[1][1] = (A[0][0]*A[2][2] - A[0][2]*A[2][0]) * invDet;
            invA[1][2] = (A[0][2]*A[1][0] - A[0][0]*A[1][2]) * invDet;
            invA[2][0] = (A[1][0]*A[2][1] - A[1][1]*A[2][0]) * invDet;
            invA[2][1] = (A[0][1]*A[2][0] - A[0][0]*A[2][1]) * invDet;
            invA[2][2] = (A[0][0]*A[1][1] - A[0][1]*A[1][0]) * invDet;

            double a0 = invA[0][0]*B[0] + invA[0][1]*B[1] + invA[0][2]*B[2];
            double a1 = invA[1][0]*B[0] + invA[1][1]*B[1] + invA[1][2]*B[2];

            double xNew = -a1 / (2 * a0);

            if (xNew > x1) {
                if (f(xNew) < f(x1)) {
                    x0 = x1;
                    x1 = xNew;
                } else {
                    x2 = xNew;
                }
            } else {
                if (f(xNew) < f(x1)) {
                    x2 = x1;
                    x1 = xNew;
                } else {
                    x0 = xNew;
                }
            }

            double precision = Math.abs(x2 - x1);
            intermediatePointsSeries.add(x1, f(x1));
            consoleArea.append(String.format("%-40s%-15d%-15.6f%-15.6f%-15.6f\n",
                    "Метод Пауэлла", iteration, x1, f(x1), precision));
        }

        return x1;
    }

    private double newton(double x0, double epsilon) {
        int iteration = 0;
        while (Math.abs(df(x0)) > epsilon) {
            iteration++;
            x0 = x0 - df(x0) / ddf(x0);
            intermediatePointsSeries.add(x0, f(x0));
            double precision = Math.abs(x0 - f(x0));
            consoleArea.append(String.format("%-40s%-15d%-15.6f%-15.6f%-15.6f\n",
                    "Метод Ньютона", iteration, x0, f(x0), precision));
        }
        return x0;
    }

    private double cubicApproximation(double a, double b, double epsilon) {
        double x0 = a;
        double x1 = b;
        int iteration = 0;
        double lastX = Double.NaN;

        if (df(x0) * df(x1) >= 0) {
            consoleArea.append("Ошибка: производные на концах интервала должны иметь разные знаки\n");
            JOptionPane.showMessageDialog(this,
                    "Метод кубической аппроксимации не применим: производные на концах должны иметь разные знаки",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return Double.NaN;
        }

        while (Math.abs(x1 - x0) > epsilon) {
            iteration++;
            double precision = Math.abs(x1 - x0);
            consoleArea.append(String.format("%-40s%-15d%-15.6f%-15.6f%-15.6f\n",
                    "Метод кубической аппрокисмации", iteration, (x0 + x1)/2, f((x0 + x1)/2), precision));

            if (Math.abs(df(x0)) < epsilon) {
                return x0;
            }
            if (Math.abs(df(x1)) < epsilon) {
                return x1;
            }

            double f0 = f(x0);
            double f1 = f(x1);
            double df0 = df(x0);
            double df1 = df(x1);

            double z = 3*(f0 - f1)/(x1 - x0) + df0 + df1;
            double w = Math.sqrt(z*z - df0*df1);
            if (Double.isNaN(w)) w = 0;

            double xNew = x1 - (x1 - x0)*(df1 + w - z)/(df1 - df0 + 2*w);

            if (Math.abs(xNew - lastX) < epsilon/10) {
                xNew = (x0 + x1)/2;
                consoleArea.append("Активирована защита от зацикливания - переход к делению пополам\n");
            }
            lastX = xNew;

            if (xNew < Math.min(x0, x1) || xNew > Math.max(x0, x1)) {
                xNew = (x0 + x1)/2;
                consoleArea.append("Новое значение вышло за границы интервала - используем середину\n");
            }

            if (df(xNew) > 0) {
                x1 = xNew;
            } else if (df(xNew) < 0) {
                x0 = xNew;
            } else {
                return xNew;
            }

            intermediatePointsSeries.add(xNew, f(xNew));

            if (iteration > 100) {
                consoleArea.append("Превышено максимальное число итераций (100)\n");
                JOptionPane.showMessageDialog(this,
                        "Метод не сошелся за 100 итераций. Возвращаем текущее приближение.",
                        "Предупреждение", JOptionPane.WARNING_MESSAGE);
                return (x0 + x1)/2;
            }
        }

        double result = (x0 + x1)/2;
        return result;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FunctionMinimizer minimizer = new FunctionMinimizer();
            minimizer.setVisible(true);
        });
    }
}