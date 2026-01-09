package ru.zolotuhin.ModelInfoProcess.Lab5;

public class Lab5Main {
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Лабораторная работа 5");
        System.out.println("Проверка генератора псевдослучайных чисел");
        System.out.println("Метод Монте-Карло");
        System.out.println("=".repeat(60));

        Task1 task1 = new Task1();
        Task2 task2 = new Task2();
        Task3 task3 = new Task3();

        task1.execute();
        task2.execute();
        task3.execute();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Лабораторная работа завершена");
        System.out.println("=".repeat(60));
    }
}
