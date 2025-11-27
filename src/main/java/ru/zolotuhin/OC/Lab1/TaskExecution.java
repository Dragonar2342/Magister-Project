package ru.zolotuhin.OC.Lab1;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskExecution {
    private List<TaskExecutionRecord> records;
    private Workbook workbook;
    private Sheet dataSheet;
    private Sheet statsSheet;
    private int rowNum;

    public TaskExecution() {
        this.records = new ArrayList<>();
        this.workbook = new XSSFWorkbook();
        this.dataSheet = workbook.createSheet("Данные выполнения");
        this.statsSheet = workbook.createSheet("Статистика");
        this.rowNum = 0;

        // Создаем заголовки для листа с данными
        createDataHeaders();
    }

    /**
     * Запись выполнения этапа задачи
     */
    public void logStageExecution(Task task, TaskStage stage, int startTime, int endTime,
                                  String state, String stageType) {
        String stageName = stageType.equals("DELAY") ? "Задержка между этапами" :
                stageType.equals("WAITING") ? "Ожидание в очереди" :
                        stage.getName();

        int duration = endTime - startTime;

        TaskExecutionRecord record = new TaskExecutionRecord(
                task.getId(),
                task.getName(),
                stageName,
                startTime,
                endTime,
                duration,
                state
        );
        records.add(record);
    }

    /**
     * Запись завершения задачи
     */
    public void logTaskCompletion(Task task, int completionTime) {
        TaskExecutionRecord record = new TaskExecutionRecord(
                task.getId(),
                task.getName(),
                "Завершена",
                completionTime,
                completionTime,
                0,
                "Завершена"
        );
        records.add(record);
    }

    /**
     * Запись состояния ожидания в очереди
     */
    public void logQueueWaiting(Task task, int startTime, int endTime) {
        TaskExecutionRecord record = new TaskExecutionRecord(
                task.getId(),
                task.getName(),
                "Ожидание в очереди",
                startTime,
                endTime,
                endTime - startTime,
                "Ожидание"
        );
        records.add(record);
    }

    /**
     * Сохранение статистики системы
     */
    public void saveStatistics(int totalSystemTime, int tasksCompleted,
                               double avgTurnaround, double avgWaitTime, int fixedDelay,
                               List<Task> completedTasks) {
        // Заголовки для статистики
        Row headerRow = statsSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Параметр");
        headerRow.createCell(1).setCellValue("Значение");

        // Данные статистики
        int rowIndex = 1;
        createStatRow(rowIndex++, "Общее время работы системы", totalSystemTime + " мс");
        createStatRow(rowIndex++, "Количество выполненных задач", String.valueOf(tasksCompleted));
        createStatRow(rowIndex++, "Среднее время выполнения", String.format("%.2f мс", avgTurnaround));
        createStatRow(rowIndex++, "Среднее время ожидания", String.format("%.2f мс", avgWaitTime));
        createStatRow(rowIndex++, "Фиксированная задержка между этапами", fixedDelay + " мс");

        // Детальная статистика по задачам
        rowIndex++;
        createStatRow(rowIndex++, "Детальная статистика по задачам:", "");

        Collections.sort(completedTasks, (t1, t2) -> Integer.compare(t1.getId(), t2.getId()));
        for (Task task : completedTasks) {
            int turnaround = task.getCompletionTime() - task.getArrivalTime();
            int waitTime = task.getTotalWaitTime();
            int executionTime = task.getTotalExecutionTime();
            int delayTime = (task.getStages().size() - 1) * fixedDelay;

            createStatRow(rowIndex++,
                    String.format("  %s (ID: %d)", task.getName(), task.getId()),
                    String.format("Оборот: %d мс, Выполнение: %d мс, Ожидание: %d мс, Задержки: %d мс",
                            turnaround, executionTime, waitTime, delayTime));
        }

        // Авто-размер колонок
        statsSheet.autoSizeColumn(0);
        statsSheet.autoSizeColumn(1);
    }

    private void createStatRow(int rowIndex, String parameter, String value) {
        Row row = statsSheet.createRow(rowIndex);
        row.createCell(0).setCellValue(parameter);
        row.createCell(1).setCellValue(value);
    }

    /**
     * Создание заголовков для листа с данными
     */
    private void createDataHeaders() {
        Row headerRow = dataSheet.createRow(rowNum++);
        String[] headers = {
                "ID задачи", "Название задачи", "Этап",
                "Начало (мс)", "Конец (мс)", "Длительность (мс)", "Состояние"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            // Стиль для заголовков
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
    }

    /**
     * Сохранение всех записей в Excel
     */
    public void saveToExcel(String filename) {
        try {
            // Записываем все записи выполнения
            for (TaskExecutionRecord record : records) {
                Row row = dataSheet.createRow(rowNum++);

                row.createCell(0).setCellValue(record.getTaskId());
                row.createCell(1).setCellValue(record.getTaskName());
                row.createCell(2).setCellValue(record.getStageName());
                row.createCell(3).setCellValue(record.getStartTime());
                row.createCell(4).setCellValue(record.getEndTime());
                row.createCell(5).setCellValue(record.getDuration());
                row.createCell(6).setCellValue(record.getState());
            }

            // Авто-размер колонок
            for (int i = 0; i < 7; i++) {
                dataSheet.autoSizeColumn(i);
            }

            // Сохраняем файл
            FileOutputStream outputStream = new FileOutputStream(filename);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            System.out.println("Отчет сохранен в файл: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении отчета: " + e.getMessage());
        }
    }

    /**
     * Внутренний класс для хранения записей выполнения
     */
    private static class TaskExecutionRecord {
        private int taskId;
        private String taskName;
        private String stageName;
        private int startTime;
        private int endTime;
        private int duration;
        private String state;

        public TaskExecutionRecord(int taskId, String taskName, String stageName,
                                   int startTime, int endTime, int duration, String state) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.stageName = stageName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.state = state;
        }

        public int getTaskId() { return taskId; }
        public String getTaskName() { return taskName; }
        public String getStageName() { return stageName; }
        public int getStartTime() { return startTime; }
        public int getEndTime() { return endTime; }
        public int getDuration() { return duration; }
        public String getState() { return state; }
    }
}
