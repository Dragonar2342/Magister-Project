// MultitaskingSystem.java
package ru.zolotuhin.OC.Lab2;

import ru.zolotuhin.OC.Lab2.TaskExecution;

import java.util.*;

/**
 * Модель системы классического мультипрограммирования с задержками между этапами
 */
public class MultitaskingSystem {
    private Queue<Task> readyQueue;
    private List<Task> waitingTasks;
    private List<Task> completedTasks;
    private Task currentTask;

    private int systemTime;
    private boolean running;
    private int timeQuantum;
    private int delay;

    private int tasksCompleted;
    private int totalWaitTime;
    private int totalTurnaroundTime;

    private int currentTaskExecutionTime;

    private TaskExecution execution;
    private Map<ru.zolotuhin.OC.Lab2.Task, Integer> taskQueueEntryTime;


    public MultitaskingSystem(int timeQuantum, int delay) {
        this.readyQueue = new LinkedList<>();
        this.waitingTasks = new ArrayList<>();
        this.completedTasks = new ArrayList<>();
        this.currentTask = null;
        this.systemTime = 0;
        this.running = false;
        this.timeQuantum = timeQuantum;
        this.delay = delay;
        this.tasksCompleted = 0;
        this.totalWaitTime = 0;
        this.totalTurnaroundTime = 0;
        this.currentTaskExecutionTime = 0;
        this.execution = new TaskExecution(delay);
        this.taskQueueEntryTime = new HashMap<>();
    }

    public void addTasks(List<Task> tasks) {
        for (Task task : tasks) {
            addTask(task);
        }
    }

    public void addTask(Task task) {
        task.setState(TaskState.READY, systemTime);
        readyQueue.add(task);
        System.out.printf("Время %d: %s добавлена в очередь готовых задач%n",
                systemTime, task.getName());
        System.out.printf("  Детали: %d этапов, общее время: %d мс%n",
                task.getStages().size(), task.getTotalDuration());
    }

    /**
     * Запуск системы с Round-robin планированием
     */
    public void run() {
        System.out.println("=== ЗАПУСК СИСТЕМЫ МУЛЬТИПРОГРАММИРОВАНИЯ (ROUND-ROBIN) ===");
        System.out.printf("Квант времени: %d мс, Задержка между этапами: %d мс%n%n", timeQuantum, delay);

        running = true;

        while (running) {
            processWaitingTasks();

            if (currentTask == null && !readyQueue.isEmpty()) {
                scheduleNextTask();
            }

            if (currentTask != null) {
                executeCurrentTask();
            }

            // Проверка завершения работы системы
            if (readyQueue.isEmpty() && waitingTasks.isEmpty() && currentTask == null) {
                running = false;
                System.out.println("Все задачи завершены");
            }

            systemTime += timeQuantum;

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                running = false;
            }
        }

        printStatistics();
        System.out.println("=== СИСТЕМА ЗАВЕРШИЛА РАБОТУ ===");
    }

    private void scheduleNextTask() {
        if (readyQueue.isEmpty()) return;

        currentTask = readyQueue.poll();
        currentTask.setState(TaskState.RUNNING, systemTime);
        currentTaskExecutionTime = 0;

        System.out.printf("Время %d: %s выбрана для выполнения%n",
                systemTime, currentTask.getName());
    }

    private void executeCurrentTask() {
        TaskStage currentStage = currentTask.getCurrentStage();

        if (currentStage == null) {
            completeCurrentTask();
            return;
        }

        int executionStartTime = systemTime;
        int executionTime = Math.min(timeQuantum - currentTaskExecutionTime, currentStage.getRemainingTime());
        currentTask.execute(executionTime);
        currentTaskExecutionTime += executionTime;

        // Запись выполнения этапа в отчет
        execution.addExecutionRecord(currentTask.getId(), currentTask.getName(),
                currentStage.getName(), executionStartTime, systemTime,
                executionTime, "Выполняется");

        System.out.printf("Время %d: %s выполняет %s (%d/%d мс) [квант: %d/%d мс]%n",
                systemTime, currentTask.getName(), currentStage.getName(),
                currentStage.getExecutedTime(), currentStage.getDuration(),
                currentTaskExecutionTime, timeQuantum);

        if (currentStage.isCompleted()) {
            System.out.printf("Время %d: %s завершила этап '%s'%n",
                    systemTime, currentTask.getName(), currentStage.getName());

            if (currentTask.hasNextStage()) {
                handleStageTransition();
            } else {
                completeCurrentTask();
            }
        }

        else if (currentTaskExecutionTime >= timeQuantum) {
            System.out.printf("Время %d: %s исчерпала квант времени, возвращается в очередь%n",
                    systemTime, currentTask.getName());

            currentTask.setState(TaskState.READY, systemTime);
            readyQueue.add(currentTask);
            currentTask = null;
            currentTaskExecutionTime = 0;
        }
    }

    private void handleStageTransition() {
        Task completedStageTask = currentTask;
        completedStageTask.moveToNextStage();
        TaskStage nextStage = completedStageTask.getCurrentStage();

        System.out.printf("Время %d: %s переходит к этапу '%s' после задержки %d мс%n",
                systemTime, completedStageTask.getName(), nextStage.getName(), delay);

        // Запись задержки в отчет
        execution.addExecutionRecord(completedStageTask.getId(), completedStageTask.getName(),
                "Задержка между этапами", systemTime, systemTime + delay,
                delay, "Ожидание");

        completedStageTask.setState(TaskState.WAITING, systemTime);
        completedStageTask.setWaitingEndTime(systemTime + delay);
        waitingTasks.add(completedStageTask);

        currentTask = null;
        currentTaskExecutionTime = 0;
    }

    /**
     * Завершение текущей задачи
     */
    private void completeCurrentTask() {
        currentTask.setState(TaskState.TERMINATED, systemTime);
        System.out.printf("Время %d: %s ПОЛНОСТЬЮ ЗАВЕРШЕНА%n",
                systemTime, currentTask.getName());

        tasksCompleted++;
        int turnaround = systemTime - currentTask.getArrivalTime();
        int waitTime = currentTask.getStartTime() - currentTask.getArrivalTime();

        totalTurnaroundTime += turnaround;
        totalWaitTime += waitTime;

        // Расчет времени выполнения (общее время - ожидание - задержки)
        int executionTime = turnaround - waitTime - (currentTask.getStages().size() - 1) * delay;
        int delayTime = (currentTask.getStages().size() - 1) * delay;

        // Добавление статистики в отчет
        execution.addTaskStatistics(currentTask.getId(), currentTask.getName(),
                turnaround, executionTime, waitTime, delayTime);

        completedTasks.add(currentTask);
        currentTask = null;
        currentTaskExecutionTime = 0;
    }

    private void processWaitingTasks() {
        Iterator<Task> iterator = waitingTasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();

            if (systemTime >= task.getWaitingEndTime()) {
                task.setState(TaskState.READY, systemTime);
                readyQueue.add(task);
                iterator.remove();
                System.out.printf("Время %d: %s вышла из ожидания (завершена задержка)%n",
                        systemTime, task.getName());

                // Запись ожидания в очереди в отчет
                TaskStage nextStage = task.getCurrentStage();
                if (nextStage != null) {
                    execution.addExecutionRecord(task.getId(), task.getName(),
                            "Ожидание в очереди", task.getWaitingEndTime() - delay, systemTime,
                            systemTime - (task.getWaitingEndTime() - delay), "Ожидание");
                }
            }
        }
    }

    /**
     * Вывод статистики
     */
    private void printStatistics() {
        System.out.println("\n=== СТАТИСТИКА СИСТЕМЫ (ROUND-ROBIN) ===");
        System.out.printf("Общее время работы: %d мс%n", systemTime);
        System.out.printf("Выполнено задач: %d%n", tasksCompleted);

        if (tasksCompleted > 0) {
            double avgTurnaround = (double) totalTurnaroundTime / tasksCompleted;
            double avgWait = (double) totalWaitTime / tasksCompleted;

            System.out.printf("Среднее время выполнения: %.2f мс%n", avgTurnaround);
            System.out.printf("Среднее время ожидания: %.2f мс%n", avgWait);

            // Установка статистики системы для отчета
            execution.setSystemStatistics(systemTime, tasksCompleted, avgTurnaround, avgWait);
        }

        System.out.println("\nДетальная информация по задачам:");
        Collections.sort(completedTasks, Comparator.comparingInt(Task::getId));

        for (Task task : completedTasks) {
            int turnaround = task.getCompletionTime() - task.getArrivalTime();
            int waitTime = task.getStartTime() - task.getArrivalTime();
            System.out.printf("  %s | Этапов: %d | Оборот: %d мс | Ожидание: %d мс%n",
                    task.getName(), task.getStages().size(), turnaround, waitTime);
        }

        // Генерация Excel отчета
        generateExcelReport();
    }

    private void generateExcelReport() {
        String filename = "task_execution_report_robin.xlsx";
        execution.generateReport(filename);
    }
}