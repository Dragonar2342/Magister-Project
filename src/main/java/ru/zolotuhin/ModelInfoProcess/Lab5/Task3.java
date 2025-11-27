package ru.zolotuhin.ModelInfoProcess.Lab5;

// Task3.java
import java.util.function.Function;

public class Task3 {
    private MonteCarloCalculator monteCarlo;

    public Task3() {
        this.monteCarlo = new MonteCarloCalculator();
    }

    public void execute() {
        System.out.println("\nЗАДАНИЕ 3: Метод Монте-Карло");
        System.out.println("-".repeat(50));

        calculatePi();
        calculateIntegrals();
        calculateDoubleIntegral();
        estimateErrors();
    }

    private void calculatePi() {
        System.out.println("\n1. Вычисление числа π методом Монте-Карло:");

        int[] sampleSizes = {1000, 5000, 10000, 50000, 100000};

        System.out.println("Объем выборки | Приближение π |   Ошибка   | Относит. ошибка");
        System.out.println("--------------|---------------|------------|----------------");

        for (int n : sampleSizes) {
            double piEstimate = monteCarlo.calculatePi(n);
            double absoluteError = Math.abs(piEstimate - Math.PI);
            double relativeError = (absoluteError / Math.PI) * 100;

            System.out.printf("    %6d    |   %10.6f  | %9.6f |     %6.3f%%%n",
                    n, piEstimate, absoluteError, relativeError);
        }

        System.out.printf("\nТочное значение π: %.10f%n", Math.PI);
    }

    private void calculateIntegrals() {
        System.out.println("\n2. Вычисление определенных интегралов:");

        Function<Double, Double> squareFunc = x -> x * x;
        double exact1 = 1.0 / 3;

        System.out.println("x^2 dx от 0 до 1:");
        calculateAndPrintIntegral(squareFunc, 0, 1, exact1, "x^2");

        Function<Double, Double> sinFunc = x -> Math.sin(x);
        double exact2 = 2.0;

        System.out.println("\nsin(x)dx от 0 до π:");
        calculateAndPrintIntegral(sinFunc, 0, Math.PI, exact2, "sin(x)");

        Function<Double, Double> expFunc = x -> Math.exp(x);
        double exact3 = Math.E - 1;

        System.out.println("\ne^x dx от 0 до 1:");
        calculateAndPrintIntegral(expFunc, 0, 1, exact3, "e^x");
    }

    private void calculateDoubleIntegral() {
        System.out.println("\n3. Вычисление двойного интеграла:");

        Function<Double[], Double> doubleFunc = point -> point[0] * point[1];
        double[] a = {0.0, 0.0};
        double[] b = {1.0, 1.0};

        System.out.println("xy dx dy от [0,0] до [1,1]:");
        System.out.println("Объем выборки | Приближение |   Ошибка   | Относит. ошибка");
        System.out.println("--------------|-------------|------------|----------------");

        int[] sampleSizes = {1000, 5000, 10000, 50000};
        double exact = 0.25;

        for (int n : sampleSizes) {
            double integralEstimate = monteCarlo.calculateDoubleIntegral(doubleFunc, a, b, n);
            double absoluteError = Math.abs(integralEstimate - exact);
            double relativeError = (absoluteError / exact) * 100;

            System.out.printf("    %6d    |  %9.6f  | %9.6f |     %6.3f%%%n",
                    n, integralEstimate, absoluteError, relativeError);
        }

        System.out.printf("Точное значение: %.6f%n", exact);
    }

    private void estimateErrors() {
        System.out.println("\n4. Оценка погрешности метода Монте-Карло:");

        System.out.println("Оценка погрешности для вычисления π:");
        System.out.println("Объем выборки | Доверит. интервал 95%");
        System.out.println("--------------|----------------------");

        int[] sampleSizes = {1000, 5000, 10000};
        int trials = 20;

        for (int n : sampleSizes) {
            double error = monteCarlo.estimateError(n, trials, 0.95);
            System.out.printf("    %6d    |       ±%.6f%n", n, error);
        }
    }

    private void calculateAndPrintIntegral(Function<Double, Double> function,
                                           double a, double b, double exact, String funcName) {
        System.out.println("Объем выборки | Приближение |   Ошибка   | Относит. ошибка");
        System.out.println("--------------|-------------|------------|----------------");

        int[] sampleSizes = {1000, 5000, 10000, 50000};

        for (int n : sampleSizes) {
            double integralEstimate = monteCarlo.calculateIntegral(function, a, b, n);
            double absoluteError = Math.abs(integralEstimate - exact);
            double relativeError = (absoluteError / exact) * 100;

            System.out.printf("    %6d    |  %9.6f  | %9.6f |     %6.3f%%%n",
                    n, integralEstimate, absoluteError, relativeError);
        }

        System.out.printf("Точное значение %s dx: %.6f%n", funcName, exact);
    }
}
