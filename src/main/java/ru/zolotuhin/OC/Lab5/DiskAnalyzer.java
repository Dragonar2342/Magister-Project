package ru.zolotuhin.OC.Lab5;

import java.io.IOException;

public class DiskAnalyzer {
    public static void main(String[] args) {
        System.out.println("=== Анализатор разделов диска для Windows 10 ===");
        try {
            DiskInfoProvider diskInfoProvider = new WindowsDiskInfoProvider();
            FileSystemAnalyzer fileSystemAnalyzer = new FileSystemAnalyzerImpl();

            DiskAnalysisService service = new DiskAnalysisService(diskInfoProvider, fileSystemAnalyzer);
            service.analyzeAndDisplay();

        } catch (IOException e) {
            System.err.println("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
