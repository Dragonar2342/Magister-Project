package ru.zolotuhin.ModelInfoProcess.Lab4;

import java.util.*;

public class Lab4Demo {
    public static void main(String[] args) {
        System.out.println("=== МОДЕЛИРОВАНИЕ ЗАДАЧИ ЧТЕНИЯ/ЗАПИСИ СЕТЯМИ ПЕТРИ ===");

        // Создаем сеть Петри с ограничением в 3 читателя
        PetriNet net = new PetriNet(3);

        System.out.println("Начальное состояние:");
        net.printState();

        // Симуляция различных сценариев
        System.out.println("\n=== СЦЕНАРИЙ 1: Читатели получают доступ ===");
        simulateReaders(net);

        System.out.println("\n=== СЦЕНАРИЙ 2: Писатель блокирует доступ ===");
        simulateWriter(net);

        System.out.println("\n=== СЦЕНАРИЙ 3: Смешанный доступ ===");
        simulateMixedAccess(net);

        // Построение дерева достижимости
        System.out.println("\n=== ПОСТРОЕНИЕ ДЕРЕВА ДОСТИЖИМОСТИ ===");
        ReachabilityTree tree = new ReachabilityTree(net);
        tree.printTree();

        // Анализ свойств сети
        analyzeNetworkProperties(net);
    }

    private static void simulateReaders(PetriNet net) {
        System.out.println("Попытка запуска нескольких читателей:");
        net.fireTransition("StartRead");
        net.fireTransition("StartRead");
        net.fireTransition("StartRead");

        System.out.println("Попытка запуска четвертого читателя (должна быть заблокирована):");
        net.fireTransition("StartRead"); // Не сработает - достигнут лимит

        System.out.println("Завершение чтения:");
        net.fireTransition("EndRead");
        net.fireTransition("EndRead");
        net.fireTransition("EndRead");
    }

    private static void simulateWriter(PetriNet net) {
        System.out.println("Писатель запрашивает доступ:");
        net.fireTransition("StartWrite");

        System.out.println("Попытка чтения во время записи (должна быть заблокирована):");
        net.fireTransition("StartRead"); // Не сработает

        System.out.println("Завершение записи:");
        net.fireTransition("EndWrite");
    }

    private static void simulateMixedAccess(PetriNet net) {
        System.out.println("Смешанный сценарий:");
        net.fireTransition("StartRead");
        net.fireTransition("StartRead");

        System.out.println("Попытка записи при активных читателях (должна быть заблокирована):");
        net.fireTransition("StartWrite"); // Не сработает

        net.fireTransition("EndRead");
        net.fireTransition("EndRead");

        net.fireTransition("StartWrite"); // Теперь сработает
        net.fireTransition("EndWrite");
    }

    private static void analyzeNetworkProperties(PetriNet net) {
        System.out.println("\n=== АНАЛИЗ СВОЙСТВ СЕТИ ПЕТРИ ===");

        // Проверка безопасности (ограниченности)
        System.out.println("Проверка безопасности:");
        Map<String, Integer> state = net.getState();
        boolean isSafe = state.values().stream().allMatch(tokens -> tokens <= 1);
        System.out.println("Сеть " + (isSafe ? "безопасна" : "не безопасна"));

        // Проверка сохранения
        int totalTokens = state.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("Общее количество меток: " + totalTokens);

        // Проверка активности
        System.out.println("Проверка активности переходов:");
        String[] transitions = {"StartRead", "EndRead", "StartWrite", "EndWrite"};
        for (String transition : transitions) {
            PetriNet testNet = new PetriNet(3);
            testNet.places.putAll(state);
            boolean enabled = testNet.fireTransition(transition);
            System.out.println("Переход " + transition + ": " +
                    (enabled ? "активен" : "не активен"));
        }
    }
}
