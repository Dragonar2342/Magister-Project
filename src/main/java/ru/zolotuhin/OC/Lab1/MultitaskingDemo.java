package ru.zolotuhin.OC.Lab1;

import java.util.List;
import java.util.Random;

public class MultitaskingDemo {
    public static void main(String[] args) {
        System.out.println("ДЕМОНСТРАЦИЯ СИСТЕМЫ С ЗАДЕРЖКАМИ МЕЖДУ ЭТАПАМИ");
        System.out.println("================================================\n");
        Random rand = new Random();
        long totalTime = 0;

        int taskCount = rand.nextInt(10 - 2) + 2;
        int minStages = 5;
        int maxStages = 9;
        int minStageTime = 1000;
        int maxStageTime = 8000;
        int fixedDelay = 2000;

        MultitaskingSystem system = new MultitaskingSystem(fixedDelay);

        List<Task> tasks = TaskGenerator.generateRandomTasks(
                taskCount, minStages, maxStages, minStageTime, maxStageTime);
        system.addTasks(tasks);

        for (Task task : tasks) {
            totalTime += task.getTotalExecutionTime() + fixedDelay;
        }
        System.out.println("Ожидаемое время выполнения задач:" + totalTime);
        system.run();
    }
}
