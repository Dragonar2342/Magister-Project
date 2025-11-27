package ru.zolotuhin.OC.Lab2;

import java.util.List;
import java.util.Random;

public class MultitaskingDemo {
    public static void main(String[] args) {
        System.out.println("ДЕМОНСТРАЦИЯ СИСТЕМЫ С КВАНТОМ ВРЕМЕНИ И ЗАДЕРЖКАМИ МЕЖДУ ЭТАПАМИ");
        System.out.println("=================================================================\n");
        Random rand = new Random();

        int taskCount = rand.nextInt(10 - 2) + 2;
        int minStages = 2;
        int maxStages = 4;
        int minStageTime = 1000;
        int maxStageTime = 30000;
        int timeQuantum = 1000;
        int delay = 2000;

        MultitaskingSystem system = new MultitaskingSystem(timeQuantum, delay);

        List<Task> tasks = TaskGenerator.generateRandomTasks(
                taskCount, minStages, maxStages, minStageTime, maxStageTime);
        system.addTasks(tasks);

        system.run();
    }
}
