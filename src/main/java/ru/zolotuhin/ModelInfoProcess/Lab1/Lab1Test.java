package ru.zolotuhin.ModelInfoProcess.Lab1;

import java.util.EmptyStackException;
import java.util.Scanner;

public class Lab1Test {
    public static void main(String[] args) {
        System.out.println("=== Демонстрация работы стека ===");
        Scanner scanner = new Scanner(System.in);
        Stack stack = new Stack(5);

        int[] elements = {10, 20, 30, 40, 50};
        for (int element : elements) {
            try {
                stack.push(element);
                System.out.println("Добавлен элемент: " + element);
            } catch (EmptyStackException e) {
                System.out.println("Не удалось добавить элемент " + element + " - стек переполнен");
            }
        }
        System.out.println("Верхний элемент: " + stack.peek());
        System.out.println("Количество элементов: " + stack.count());

        System.out.println("\nПопытка добавить элемент в переполненный стек:");
        try {
            stack.push(60);
        } catch (IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        System.out.println("\nТекущее состояние стека:");
        stack.display();

        System.out.println("\nУдаляем элементы:");
        while (!stack.isEmpty()) {
            int element = stack.pop();
            System.out.println("Удален элемент: " + element);
            System.out.println("Количество элементов: " + stack.count());
        }

        System.out.println("\nПопытка удалить из пустого стека:");
        try {
            stack.pop();
        } catch (EmptyStackException e) {
            System.out.println("Ошибка: " + e.getClass().getSimpleName() + " - стек пуст");
        }

        System.out.println("\nПопытка посмотреть верхний элемент пустого стека:");
        try {
            stack.peek();
        } catch (EmptyStackException e) {
            System.out.println("Ошибка: " + e.getClass().getSimpleName() + " - стек пуст");
        }


        String[] testExpressions = {
                "3 4 +",           // 7
                "5 2 -",           // 3
                "4 3 *",           // 12
                "10 2 /",          // 5
                "2 3 ^",           // 8
                "15 7 1 1 + - / 3 * 2 1 1 + + -" // 5
        };

        RPNCalculator calculator = new RPNCalculator();

        boolean running = true;
        while (running) {
            System.out.print("\nВведите выражение или команду: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Выход из программы...");
                running = false;
            }
            else if (input.isEmpty()) {
                continue;
            }
            else {
                try {
                    calculator.compute(input);
                } catch (Exception e) {
                    System.out.println("Ошибка вычисления: " + e.getMessage());
                    System.out.println("Проверьте правильность выражения и попробуйте снова.");
                }
            }
        }
    }
}
