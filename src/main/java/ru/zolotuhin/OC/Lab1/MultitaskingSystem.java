package ru.zolotuhin.OC.Lab1;

import java.util.*;

/**
 * Модель системы мультипрограммирования с задержками между этапами
 */
public class MultitaskingSystem {
    private Queue<Task> readyQueue;
    private List<Task> waitingTasks;
    private List<Task> completedTasks;
    private Task currentTask;

    private int systemTime;
    private boolean running;
    private int fixedDelay;

    private int tasksCompleted;
    private int totalWaitTime;
    private int totalTurnaroundTime;

    private TaskExecution execution;
    private Map<Task, Integer> taskQueueEntryTime;

    public MultitaskingSystem(int fixedDelay) {
        this.readyQueue = new LinkedList<>();
        this.waitingTasks = new ArrayList<>();
        this.completedTasks = new ArrayList<>();
        this.currentTask = null;
        this.systemTime = 0;
        this.running = false;
        this.fixedDelay = fixedDelay;
        this.tasksCompleted = 0;
        this.totalWaitTime = 0;
        this.totalTurnaroundTime = 0;
        this.execution = new TaskExecution();
        this.taskQueueEntryTime = new HashMap<>();
    }

    /**
     * Добавление списка задач в систему
     */
    public void addTasks(List<Task> tasks) {
        for (Task task : tasks) {
            addTask(task);
        }
    }

    /**
     * Добавление одной задачи в систему
     */
    public void addTask(Task task) {
        task.setState(TaskState.READY, systemTime);
        readyQueue.add(task);
        taskQueueEntryTime.put(task, systemTime);
        System.out.printf("Время %d: %s добавлена в систему%n",
                systemTime, task.getName());
        System.out.printf("  Детали: %d этапов, общее время выполнения: %d мс, задержка между этапами: %d мс%n",
                task.getStages().size(), task.getTotalExecutionTime(), fixedDelay);
    }

    /**
     * Запуск системы
     */
    public void run() {
        System.out.println("\n=== ЗАПУСК СИСТЕМЫ МУЛЬТИПРОГРАММИРОВАНИЯ ===");
        running = true;

        while (running) {
            processDelayedTasks();

            if (currentTask == null && !readyQueue.isEmpty()) {
                scheduleNextTask();
            }

            if (currentTask != null) {
                executeCurrentTask();
            }

            if (readyQueue.isEmpty() && waitingTasks.isEmpty() && currentTask == null) {
                running = false;
                System.out.println("\nВсе задачи завершены");
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                running = false;
            }
        }

        printStatistics();
        System.out.println("=== СИСТЕМА ЗАВЕРШИЛА РАБОТУ ===");
    }

    /**
     * Выбор следующей задачи для выполнения
     */
    private void scheduleNextTask() {
        if (readyQueue.isEmpty()) return;

        Task task = readyQueue.peek();

        Integer queueEntryTime = taskQueueEntryTime.get(task);
        if (queueEntryTime == null) {
            queueEntryTime = systemTime;
            taskQueueEntryTime.put(task, systemTime);
        }

        if (queueEntryTime < systemTime) {
            execution.logQueueWaiting(task, queueEntryTime, systemTime);
        }

        currentTask = readyQueue.poll();
        taskQueueEntryTime.remove(currentTask);
        currentTask.setState(TaskState.RUNNING, systemTime);

        System.out.printf("Время %d: %s выбрана для выполнения (ожидала в очереди %d мс)%n",
                systemTime, currentTask.getName(), systemTime - queueEntryTime);
    }

    /**
     * Выполнение текущей задачи
     */
    private void executeCurrentTask() {
        TaskStage currentStage = currentTask.getCurrentStage();

        if (currentStage == null) {
            completeCurrentTask();
            return;
        }

        int stageStartTime = systemTime;

        int stageDuration = currentStage.getDuration();
        currentTask.execute(stageDuration);
        systemTime += stageDuration;

        execution.logStageExecution(currentTask, currentStage, stageStartTime,
                systemTime, "Выполняется", "STAGE");

        System.out.printf("Время %d: %s завершила этап '%s' (%d мс)%n",
                systemTime, currentTask.getName(), currentStage.getName(), stageDuration);

        if (currentStage.isCompleted()) {
            if (currentTask.hasNextStage()) {
                int delayStartTime = systemTime;
                int delayEndTime = systemTime + fixedDelay;

                execution.logStageExecution(currentTask, null, delayStartTime,
                        delayEndTime, "Ожидание", "DELAY");

                System.out.printf("Время %d: %s переходит к следующему этапу после задержки %d мс%n",
                        systemTime, currentTask.getName(), fixedDelay);

                handleStageTransition();
            } else {
                completeCurrentTask();
            }
        }
    }

    /**
     * Обработка перехода между этапами задачи с задержкой
     */
    private void handleStageTransition() {
        Task completedStageTask = currentTask;
        completedStageTask.moveToNextStage();
        TaskStage nextStage = completedStageTask.getCurrentStage();

        System.out.printf("Время %d: %s переходит к этапу '%s' после задержки %d мс%n",
                systemTime, completedStageTask.getName(), nextStage.getName(), fixedDelay);

        completedStageTask.setState(TaskState.WAITING, systemTime);
        completedStageTask.setDelayEndTime(systemTime + fixedDelay);
        waitingTasks.add(completedStageTask);

        currentTask = null;
    }

    /**
     * Завершение текущей задачи
     */
    private void completeCurrentTask() {
        currentTask.setState(TaskState.TERMINATED, systemTime);

        execution.logTaskCompletion(currentTask, systemTime);

        System.out.printf("Время %d: %s ПОЛНОСТЬЮ ЗАВЕРШЕНА%n",
                systemTime, currentTask.getName());

        tasksCompleted++;
        totalTurnaroundTime += (systemTime - currentTask.getArrivalTime());
        totalWaitTime += currentTask.getTotalWaitTime();

        completedTasks.add(currentTask);
        currentTask = null;
    }

    /**
     * Обработка задач в состоянии задержки
     */
    private void processDelayedTasks() {
        Iterator<Task> iterator = waitingTasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();

            if (systemTime >= task.getDelayEndTime()) {
                task.setState(TaskState.READY, systemTime);
                readyQueue.add(task);
                taskQueueEntryTime.put(task, systemTime);
                iterator.remove();
            }
        }
    }

    /**
     * Вывод статистики
     */
    private void printStatistics() {
        System.out.println("\n=== СТАТИСТИКА СИСТЕМЫ ===");
        System.out.printf("Общее время работы: %d мс%n", systemTime);
        System.out.printf("Выполнено задач: %d%n", tasksCompleted);

        double avgTurnaround = 0;
        double avgWait = 0;

        if (tasksCompleted > 0) {
            avgTurnaround = (double) totalTurnaroundTime / tasksCompleted;
            avgWait = (double) totalWaitTime / tasksCompleted;

            System.out.printf("Среднее время выполнения: %.2f мс%n", avgTurnaround);
            System.out.printf("Среднее время ожидания: %.2f мс%n", avgWait);
        }

        execution.saveStatistics(systemTime, tasksCompleted, avgTurnaround, avgWait, fixedDelay, completedTasks);
        execution.saveToExcel("task_execution_report.xlsx");

        System.out.println("\nДетальная информация по задачам:");
        Collections.sort(completedTasks, (t1, t2) -> Integer.compare(t1.getId(), t2.getId()));

        for (Task task : completedTasks) {
            int turnaround = task.getCompletionTime() - task.getArrivalTime();
            int waitTime = task.getTotalWaitTime();
            System.out.printf("  %s | Этапов: %d | Оборот: %d мс | Ожидание: %d мс%n",
                    task.getName(), task.getStages().size(), turnaround, waitTime);
        }
    }
}