package ru.zolotuhin.OC.Lab2;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Класс для создания отчета по выполнению задач в формате Excel
 */
public class TaskExecution {
    private List<TaskExecutionRecord> executionRecords;
    private Map<Integer, TaskStatistics> taskStatistics;
    private int totalSystemTime;
    private int tasksCompleted;
    private double avgTurnaroundTime;
    private double avgWaitTime;
    private int fixedDelay;

    public TaskExecution(int fixedDelay) {
        this.executionRecords = new ArrayList<>();
        this.taskStatistics = new HashMap<>();
        this.fixedDelay = fixedDelay;
    }

    /**
     * Добавление записи о выполнении этапа задачи
     */
    public void addExecutionRecord(int taskId, String taskName, String stageName,
                                   int startTime, int endTime, int duration, String state) {
        executionRecords.add(new TaskExecutionRecord(taskId, taskName, stageName,
                startTime, endTime, duration, state));
    }

    /**
     * Добавление статистики по задаче
     */
    public void addTaskStatistics(int taskId, String taskName, int turnaroundTime,
                                  int executionTime, int waitTime, int delayTime) {
        taskStatistics.put(taskId, new TaskStatistics(taskName, turnaroundTime,
                executionTime, waitTime, delayTime));
    }

    /**
     * Установка общей статистики системы
     */
    public void setSystemStatistics(int totalSystemTime, int tasksCompleted,
                                    double avgTurnaroundTime, double avgWaitTime) {
        this.totalSystemTime = totalSystemTime;
        this.tasksCompleted = tasksCompleted;
        this.avgTurnaroundTime = avgTurnaroundTime;
        this.avgWaitTime = avgWaitTime;
    }

    /**
     * Создание отчета в формате Excel
     */
    public void generateReport(String filename) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Лист с детальными данными выполнения
            createExecutionDataSheet(workbook);

            // Лист со статистикой
            createStatisticsSheet(workbook);

            // Сохранение файла
            try (FileOutputStream outputStream = new FileOutputStream(filename)) {
                workbook.write(outputStream);
            }

            System.out.println("Отчет успешно создан: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка при создании отчета: " + e.getMessage());
        }
    }

    private void createExecutionDataSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Данные выполнения");

        // Создание стилей
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID задачи", "Название задачи", "Этап",
                "Начало (мс)", "Конец (мс)", "Длительность (мс)", "Состояние"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Данные
        int rowNum = 1;
        for (TaskExecutionRecord record : executionRecords) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(record.taskId);
            row.createCell(1).setCellValue(record.taskName);
            row.createCell(2).setCellValue(record.stageName);
            row.createCell(3).setCellValue(record.startTime);
            row.createCell(4).setCellValue(record.endTime);
            row.createCell(5).setCellValue(record.duration);
            row.createCell(6).setCellValue(record.state);

            // Применение стиля к ячейкам
            for (int i = 0; i < 7; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // Авто-размер колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createStatisticsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Статистика");

        // Создание стилей
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);

        int rowNum = 0;

        // Общая статистика системы
        Row paramHeaderRow = sheet.createRow(rowNum++);
        paramHeaderRow.createCell(0).setCellValue("Параметр");
        paramHeaderRow.createCell(1).setCellValue("Значение");
        paramHeaderRow.getCell(0).setCellStyle(headerStyle);
        paramHeaderRow.getCell(1).setCellStyle(headerStyle);

        Row totalTimeRow = sheet.createRow(rowNum++);
        totalTimeRow.createCell(0).setCellValue("Общее время работы системы");
        totalTimeRow.createCell(1).setCellValue(totalSystemTime + " мс");

        Row tasksCountRow = sheet.createRow(rowNum++);
        tasksCountRow.createCell(0).setCellValue("Количество выполненных задач");
        tasksCountRow.createCell(1).setCellValue(tasksCompleted);

        Row avgTurnaroundRow = sheet.createRow(rowNum++);
        avgTurnaroundRow.createCell(0).setCellValue("Среднее время выполнения");
        avgTurnaroundRow.createCell(1).setCellValue(String.format("%.2f мс", avgTurnaroundTime));

        Row avgWaitRow = sheet.createRow(rowNum++);
        avgWaitRow.createCell(0).setCellValue("Среднее время ожидания");
        avgWaitRow.createCell(1).setCellValue(String.format("%.2f мс", avgWaitTime));

        Row delayRow = sheet.createRow(rowNum++);
        delayRow.createCell(0).setCellValue("Фиксированная задержка между этапами");
        delayRow.createCell(1).setCellValue(fixedDelay + " мс");

        rowNum++; // Пустая строка

        // Детальная статистика по задачам
        Row detailHeaderRow = sheet.createRow(rowNum++);
        detailHeaderRow.createCell(0).setCellValue("Детальная статистика по задачам:");
        detailHeaderRow.getCell(0).setCellStyle(boldStyle);

        for (Map.Entry<Integer, TaskStatistics> entry : taskStatistics.entrySet()) {
            TaskStatistics stats = entry.getValue();
            Row taskRow = sheet.createRow(rowNum++);

            String taskInfo = String.format("%s (ID: %d)", stats.taskName, entry.getKey());
            String statsInfo = String.format("Оборот: %d мс, Выполнение: %d мс, Ожидание: %d мс, Задержки: %d мс",
                    stats.turnaroundTime, stats.executionTime, stats.waitTime, stats.delayTime);

            taskRow.createCell(0).setCellValue(taskInfo);
            taskRow.createCell(1).setCellValue(statsInfo);
        }

        // Применение стилей к данным
        for (int i = 1; i < rowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int j = 0; j <= 1; j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null && cell.getCellStyle().getIndex() == 0) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
        }

        // Авто-размер колонок
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Внутренний класс для хранения записей выполнения
     */
    private static class TaskExecutionRecord {
        int taskId;
        String taskName;
        String stageName;
        int startTime;
        int endTime;
        int duration;
        String state;

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
    }

    /**
     * Внутренний класс для хранения статистики по задаче
     */
    private static class TaskStatistics {
        String taskName;
        int turnaroundTime;
        int executionTime;
        int waitTime;
        int delayTime;

        public TaskStatistics(String taskName, int turnaroundTime, int executionTime,
                              int waitTime, int delayTime) {
            this.taskName = taskName;
            this.turnaroundTime = turnaroundTime;
            this.executionTime = executionTime;
            this.waitTime = waitTime;
            this.delayTime = delayTime;
        }
    }
}
