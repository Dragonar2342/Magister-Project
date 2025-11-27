package ru.zolotuhin.ModelInfoProcess.Lab5;

// Task1.java
public class Task1 {
    private CongruentialGenerator congruentialGen;
    private DistributionGenerator distributionGen;

    public Task1() {
        this.congruentialGen = new CongruentialGenerator(1664525, (long)Math.pow(2, 32), 123456789);
        this.distributionGen = new DistributionGenerator();
    }

    public void execute() {
        System.out.println("\nЗАДАНИЕ 1: Генерация псевдослучайных чисел");
        System.out.println("-".repeat(50));

        demonstrateCongruentialMethods();
        demonstrateDistributions();
        demonstrateMiddleSquare();
    }

    private void demonstrateCongruentialMethods() {
        System.out.println("\n1. Конгруэнтные методы:");

        System.out.println("Мультипликативный алгоритм (первые 10 чисел):");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%.6f ", congruentialGen.multiplicativeCongruential());
        }
        System.out.println();

        System.out.println("Смешанный алгоритм (первые 10 чисел):");
        congruentialGen.setCurrent(123456789); // сброс состояния
        for (int i = 0; i < 10; i++) {
            System.out.printf("%.6f ", congruentialGen.mixedCongruential(1));
        }
        System.out.println();
    }

    private void demonstrateDistributions() {
        System.out.println("\n2. Генерация различных распределений:");

        System.out.println("Нормальное распределение N(0,1) (первые 10 чисел):");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%.6f ", distributionGen.generateNormal(0, 1));
        }
        System.out.println();

        System.out.println("Экспоненциальное распределение (первые 10 чисел):");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%.6f ", distributionGen.generateExponential(2.0));
        }
        System.out.println();

        System.out.println("Распределение Пуассона (первые 10 чисел):");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%d ", distributionGen.generatePoisson(3.0));
        }
        System.out.println();
    }

    private void demonstrateMiddleSquare() {
        System.out.println("\n3. Метод срединных квадратов (фон Неймана):");
        long seed = 1234;
        System.out.println("Первые 10 чисел:");
        for (int i = 0; i < 10; i++) {
            double number = CongruentialGenerator.middleSquare(seed);
            System.out.printf("%.6f ", number);
            seed = (long)(number * 10000);
        }
        System.out.println();
    }
}