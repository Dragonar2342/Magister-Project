package ru.zolotuhin.OC.Lab1;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс, представляющий задачу в системе мультипрограммирования
 */
public class Task {
    private static int nextId = 1;

    private int id;
    private String name;
    private List<TaskStage> stages;
    private int currentStageIndex;
    private TaskState state;
    private int arrivalTime;
    private int startTime;
    private int completionTime;
    private int delayEndTime;
    private int totalWaitTime;

    public Task(String name, int arrivalTime) {
        this.id = nextId++;
        this.name = name;
        this.stages = new ArrayList<>();
        this.currentStageIndex = 0;
        this.state = TaskState.NEW;
        this.arrivalTime = arrivalTime;
        this.startTime = -1;
        this.completionTime = -1;
        this.delayEndTime = -1;
        this.totalWaitTime = 0;
    }

    public void addStage(String stageName, int durationMs) {
        stages.add(new TaskStage(stageName, durationMs));
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<TaskStage> getStages() { return stages; }
    public int getArrivalTime() { return arrivalTime; }
    public int getCompletionTime() { return completionTime; }
    public int getDelayEndTime() { return delayEndTime; }
    public int getTotalWaitTime() { return totalWaitTime; }

    public void setDelayEndTime(int delayEndTime) {
        this.delayEndTime = delayEndTime;
    }

    public TaskStage getCurrentStage() {
        if (currentStageIndex < stages.size()) {
            return stages.get(currentStageIndex);
        }
        return null;
    }

    public int getTotalExecutionTime() {
        int total = 0;
        for (TaskStage stage : stages) {
            total += stage.getDuration();
        }
        return total;
    }

    public int getRemainingExecutionTime() {
        int remaining = 0;
        for (int i = currentStageIndex; i < stages.size(); i++) {
            remaining += stages.get(i).getRemainingTime();
        }
        return remaining;
    }

    public boolean hasNextStage() {
        return currentStageIndex < stages.size() - 1;
    }

    public void setState(TaskState newState, int currentTime) {
        System.out.printf("Время %d: Задача %d '%s': %s -> %s%n",
                currentTime, id, name, state, newState);

        if (newState == TaskState.READY && state == TaskState.WAITING) {
            totalWaitTime += (currentTime - startTime);
        }

        this.state = newState;

        if (newState == TaskState.RUNNING && startTime == -1) {
            startTime = currentTime;
        } else if (newState == TaskState.TERMINATED) {
            completionTime = currentTime;
        } else if (newState == TaskState.WAITING) {
            startTime = currentTime;
        }
    }

    public void moveToNextStage() {
        if (hasNextStage()) {
            currentStageIndex++;
        }
    }

    public void execute(int timeMs) {
        if (currentStageIndex < stages.size()) {
            TaskStage currentStage = stages.get(currentStageIndex);
            currentStage.execute(timeMs);
        }
    }

    @Override
    public String toString() {
        return String.format("Задача %d: '%s' [%s], этап %d/%d, осталось выполнения: %dмс",
                id, name, state, currentStageIndex + 1, stages.size(), getRemainingExecutionTime());
    }
}

/**
 * Этап задачи
 */
class TaskStage {
    private String name;
    private int duration;
    private int executedTime;

    public TaskStage(String name, int duration) {
        this.name = name;
        this.duration = duration;
        this.executedTime = 0;
    }

    public String getName() { return name; }
    public int getDuration() { return duration; }
    public int getRemainingTime() { return Math.max(0, duration - executedTime); }
    public boolean isCompleted() { return executedTime >= duration; }

    public void execute(int timeMs) {
        executedTime += timeMs;
        if (executedTime > duration) {
            executedTime = duration;
        }
    }
}

/**
 * Перечисление состояний задачи
 */
enum TaskState {
    NEW("Новая"),
    READY("Готова"),
    RUNNING("Выполняется"),
    WAITING("Ожидание"),
    TERMINATED("Завершена");

    private String description;

    TaskState(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
