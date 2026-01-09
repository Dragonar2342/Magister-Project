package ru.zolotuhin.OC.Lab5;

/**
 * Интерфейс для анализа файловых систем
 */
public interface FileSystemAnalyzer {
    String analyzeFileSystem(String fsType);
    String getFileSystemDetails(String fsType);
}
