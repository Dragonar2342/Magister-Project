package ru.zolotuhin.ModelInfoProcess.Lab2;

import java.util.EmptyStackException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Lab2Demo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== Лабораторная работа 2: Очереди ===");
            System.out.println("1. Демонстрация Очереди");
            System.out.println("2. Демонстрация Двухсторонней очереди");
            System.out.println("3. Демонстрация Стека на основе Двухсторонней очереди");
            System.out.println("4. Тестирование сценариев с пустыми/полными структурами");
            System.out.println("5. Выход");
            System.out.print("Выберите опцию: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    demoQueue();
                    break;
                case "2":
                    demoDeque();
                    break;
                case "3":
                    demoDequeStack();
                    break;
                case "4":
                    testScenarios();
                    break;
                case "5":
                    running = false;
                    System.out.println("Выход из программы...");
                    break;
                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }

        scanner.close();
    }

    private static void demoQueue() {
        System.out.println("\n=== Демонстрация работы Очереди ===");
        ArrayQueue queue = new ArrayQueue(3);

        int[] elements = {10, 20, 30};
        for (int element : elements) {
            int result = queue.enqueue(element);
            System.out.println("Добавлен элемент: " + element + " (результат: " + (result == 1 ? "успешно" : "ошибка") + ")");
            System.out.println("Текущее состояние: " + queue);
        }

        // Демонстрация расширения
        System.out.println("\nДобавление элемента в полную очередь:");
        int result = queue.enqueue(40);
        System.out.println("Результат добавления: " + (result == 1 ? "успешно" : "ошибка"));
        System.out.println("Текущее состояние: " + queue);

        // Просмотр первого элемента
        Integer firstElement = queue.peek();
        System.out.println("\nПервый элемент (peek): " + (firstElement != null ? firstElement : "null (очередь пуста)"));
        System.out.println("Количество элементов: " + queue.count());

        // Удаление элементов
        System.out.println("\nУдаляем элементы:");
        while (!queue.isEmpty()) {
            int element = queue.dequeue();
            if (element != -1) {
                System.out.println("Удален элемент: " + element);
            } else {
                System.out.println("Ошибка удаления: очередь пуста");
            }
            System.out.println("Текущее состояние: " + queue);
        }

        // Попытка удалить из пустой очереди
        System.out.println("\nПопытка удалить из пустой очереди:");
        int deleteResult = queue.dequeue();
        if (deleteResult == -1) {
            System.out.println("Ошибка удаления: очередь пуста");
        }

        // Проверка top() на пустой очереди
        System.out.println("\nПроверка top() на пустой очереди:");
        Integer topElement = queue.top();
        System.out.println("Top(): " + (topElement != null ? topElement : "null"));
    }

    private static void demoDeque() {
        System.out.println("\n=== Демонстрация работы Двухсторонней очереди ===");
        ArrayDeque deque = new ArrayDeque(3);

        // Добавление элементов в начало
        System.out.println("Добавляем элементы в начало:");
        int result1 = deque.enqueueFirst(10);
        System.out.println("Добавлен 10 в начало: " + deque + " (результат: " + (result1 == 1 ? "успешно" : "ошибка") + ")");

        int result2 = deque.enqueueFirst(20);
        System.out.println("Добавлен 20 в начало: " + deque + " (результат: " + (result2 == 1 ? "успешно" : "ошибка") + ")");

        // Добавление элементов в конец
        System.out.println("\nДобавляем элементы в конец:");
        int result3 = deque.enqueueLast(30);
        System.out.println("Добавлен 30 в конец: " + deque + " (результат: " + (result3 == 1 ? "успешно" : "ошибка") + ")");

        // Демонстрация расширения
        System.out.println("\nДобавляем еще элементы:");
        int result4 = deque.enqueueLast(40);
        System.out.println("После добавления 40: " + deque + " (результат: " + (result4 == 1 ? "успешно" : "ошибка") + ")");

        // Просмотр элементов
        System.out.println("\nПервый элемент: " + (deque.peekFirst() != null ? deque.peekFirst() : "null"));
        System.out.println("Последний элемент: " + (deque.peekLast() != null ? deque.peekLast() : "null"));
        System.out.println("Количество элементов: " + deque.count());

        // Удаление из начала
        int first = deque.dequeueFirst();
        System.out.println("\nУдаляем из начала: " + (first != -1 ? first : "ошибка удаления"));
        System.out.println("Текущее состояние: " + deque);

        // Удаление с конца
        int last = deque.dequeueLast();
        System.out.println("Удаляем с конца: " + (last != -1 ? last : "ошибка удаления"));
        System.out.println("Текущее состояние: " + deque);

        // Отображение полного состояния
        System.out.println("\nТекущее состояние дека:");
        deque.display();
    }

    private static void demoDequeStack() {
        System.out.println("\n=== Демонстрация работы Стека на основе Дека ===");
        DequeStack stack = new DequeStack(3);

        // Добавление элементов в стек
        int[] elements = {10, 20, 30};
        for (int element : elements) {
            int result = stack.push(element);
            System.out.println("Добавлен элемент: " + element + " (результат: " + (result == 1 ? "успешно" : "ошибка") + ")");
            System.out.println("Текущее состояние: " + stack);
        }

        // Демонстрация расширения
        System.out.println("\nДобавление элемента в полный стек:");
        int result = stack.push(40);
        System.out.println("Результат добавления: " + (result == 1 ? "успешно" : "ошибка"));
        System.out.println("Текущее состояние: " + stack);

        // Просмотр верхнего элемента
        Integer topElement = stack.peek();
        System.out.println("\nВерхний элемент (peek): " + (topElement != null ? topElement : "null (стек пуст)"));
        System.out.println("Количество элементов: " + stack.count());

        // Удаление элементов
        System.out.println("\nУдаляем элементы:");
        while (!stack.isEmpty()) {
            int element = stack.pop();
            if (element != -1) {
                System.out.println("Удален элемент: " + element);
            } else {
                System.out.println("Ошибка удаления: стек пуст");
            }
            System.out.println("Текущее состояние: " + stack);
        }

        // Попытка удалить из пустого стека
        System.out.println("\nПопытка удалить из пустого стека:");
        int popResult = stack.pop();
        if (popResult == -1) {
            System.out.println("Ошибка удаления: стек пуст");
        }

        // Проверка top() на пустом стеке
        System.out.println("\nПроверка top() на пустом стеке:");
        Integer stackTop = stack.top();
        System.out.println("Top(): " + (stackTop != null ? stackTop : "null"));
    }

    private static void testScenarios() {
        System.out.println("\n=== Тестирование сценариев с пустыми/полными структурами ===");

        // Тестирование очереди
        System.out.println("\n--- Тестирование Очереди ---");
        ArrayQueue queue = new ArrayQueue(2);

        // Тест 1: Добавление в полную очередь с отказом от расширения
        System.out.println("\n1. Добавление в полную очередь с отказом от расширения:");
        queue.enqueue(1);
        queue.enqueue(2);
        System.out.println("Очередь заполнена: " + queue);

        // Симулируем отказ от расширения
        System.out.println("Попытка добавить элемент 3:");
        // Здесь пользователь должен ввести 0 для отказа

        // Тест 2: Удаление из пустой очереди
        System.out.println("\n2. Удаление из пустой очереди:");
        while (!queue.isEmpty()) {
            queue.dequeue();
        }
        int dequeueResult = queue.dequeue();
        System.out.println("Результат dequeue(): " + (dequeueResult == -1 ? "-1 (ошибка)" : dequeueResult));

        // Тест 3: Top() на пустой очереди
        System.out.println("\n3. Top() на пустой очереди:");
        Integer topResult = queue.top();
        System.out.println("Результат top(): " + (topResult != null ? topResult : "null"));

        // Тестирование дека
        System.out.println("\n--- Тестирование Дека ---");
        ArrayDeque deque = new ArrayDeque(2);

        // Тест 4: Добавление в полный дек
        System.out.println("\n4. Добавление в полный дек:");
        deque.enqueueFirst(1);
        deque.enqueueLast(2);
        System.out.println("Дек заполнен: " + deque);

        // Тест 5: Удаление из пустого дека
        System.out.println("\n5. Удаление из пустого дека:");
        while (!deque.isEmpty()) {
            deque.dequeueFirst();
        }
        int dequeueFirstResult = deque.dequeueFirst();
        int dequeueLastResult = deque.dequeueLast();
        System.out.println("Результат dequeueFirst(): " + (dequeueFirstResult == -1 ? "-1 (ошибка)" : dequeueFirstResult));
        System.out.println("Результат dequeueLast(): " + (dequeueLastResult == -1 ? "-1 (ошибка)" : dequeueLastResult));

        // Тест 6: Peek на пустом деке
        System.out.println("\n6. Peek на пустом деке:");
        Integer peekFirstResult = deque.peekFirst();
        Integer peekLastResult = deque.peekLast();
        System.out.println("Результат peekFirst(): " + (peekFirstResult != null ? peekFirstResult : "null"));
        System.out.println("Результат peekLast(): " + (peekLastResult != null ? peekLastResult : "null"));

        // Тестирование стека
        System.out.println("\n--- Тестирование Стека ---");
        DequeStack stack = new DequeStack(2);

        // Тест 7: Добавление в полный стек
        System.out.println("\n7. Добавление в полный стек:");
        stack.push(1);
        stack.push(2);
        System.out.println("Стек заполнен: " + stack);

        // Тест 8: Удаление из пустого стека
        System.out.println("\n8. Удаление из пустого стека:");
        while (!stack.isEmpty()) {
            stack.pop();
        }
        int popResult = stack.pop();
        System.out.println("Результат pop(): " + (popResult == -1 ? "-1 (ошибка)" : popResult));

        // Тест 9: Top() на пустом стеке
        System.out.println("\n9. Top() на пустом стеке:");
        Integer stackTopResult = stack.top();
        System.out.println("Результат top(): " + (stackTopResult != null ? stackTopResult : "null"));
    }
}
