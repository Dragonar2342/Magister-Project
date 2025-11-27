package ru.zolotuhin.OC.Lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Утилита для генерации случайных задач
 */
public class TaskGenerator {
    private static Random random = new Random();

    private static String[] taskNames = {
            "Вычисление матриц", "Сортировка данных", "Анализ текста", "Графическая обработка",
            "Сетевое соединение", "База данных", "Резервное копирование", "Компиляция проекта",
            "Тестирование системы", "Обработка изображений", "Шифрование данных", "Статистический анализ"
    };

    private static String[] stageTypes = {
            "Инициализация", "Загрузка данных", "Обработка", "Вычисления",
            "Анализ", "Сортировка", "Фильтрация", "Валидация",
            "Оптимизация", "Сохранение", "Финализация", "Очистка"
    };

    /**
     * Генерация списка случайных задач
     */
    public static List<Task> generateRandomTasks(int count, int minStages, int maxStages,
                                                 int minStageTime, int maxStageTime) {
        List<Task> tasks = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String taskName = taskNames[random.nextInt(taskNames.length)] + " #" + (i + 1);
            Task task = new Task(taskName, 0);

            int numStages = random.nextInt(maxStages - minStages + 1) + minStages;

            for (int j = 0; j < numStages; j++) {
                String stageName = stageTypes[random.nextInt(stageTypes.length)] + " " + (j + 1);
                int stageTime = random.nextInt(maxStageTime - minStageTime + 1) + minStageTime;
                task.addStage(stageName, stageTime);
            }

            tasks.add(task);
        }

        return tasks;
    }
}
