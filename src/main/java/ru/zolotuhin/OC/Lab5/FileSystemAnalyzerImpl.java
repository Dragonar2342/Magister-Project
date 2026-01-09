package ru.zolotuhin.OC.Lab5;

/**
 * Анализатор файловых систем на основе лекционных материалов
 */
public class FileSystemAnalyzerImpl implements FileSystemAnalyzer {

    @Override
    public String analyzeFileSystem(String fsType) {
        if (fsType == null || fsType.equals("N/A")) {
            return "Неизвестная файловая система";
        }

        switch (fsType.toUpperCase()) {
            case "NTFS":
                return "Файловая система NTFS (New Technology File System)";
            case "FAT32":
                return "Файловая система FAT32 (File Allocation Table)";
            case "FAT":
            case "FAT16":
                return "Файловая система FAT/FAT16";
            case "FAT12":
                return "Файловая система FAT12";
            case "EXFAT":
                return "Файловая система exFAT (Extended FAT)";
            case "REFS":
                return "Файловая система ReFS (Resilient File System)";
            case "APFS":
                return "Файловая система APFS (Apple File System)";
            case "EXT4":
                return "Файловая система EXT4 (Fourth Extended Filesystem)";
            case "EXT3":
                return "Файловая система EXT3 (Third Extended Filesystem)";
            case "EXT2":
                return "Файловая система EXT2 (Second Extended Filesystem)";
            default:
                return "Файловая система: " + fsType;
        }
    }

    @Override
    public String getFileSystemDetails(String fsType) {
        if (fsType == null || fsType.equals("N/A")) {
            return "Нет информации о файловой системе";
        }

        String fsUpper = fsType.toUpperCase();

        switch (fsUpper) {
            case "NTFS":
                return "Характеристики NTFS (из лекции):\n" +
                        "   • 64-разрядные адреса\n" +
                        "   • Размер кластера: 512 байт - 64 КБ\n" +
                        "   • Максимальный размер файла: 16 EB\n" +
                        "   • Журналируемая файловая система\n" +
                        "   • Поддержка сжатия и шифрования\n" +
                        "   • Контроль доступа (ACL)";
            case "FAT32":
                return "Характеристики FAT32 (из лекции):\n" +
                        "   • 32-разрядные адреса (28 бит)\n" +
                        "   • Размер кластера: 512 байт - 32 КБ\n" +
                        "   • Максимальный размер файла: 4 ГБ\n" +
                        "   • Максимальный размер тома: 2 ТБ\n" +
                        "   • Не журналируемая\n" +
                        "   • Поддержка длинных имен";
            case "FAT":
            case "FAT16":
                return "Характеристики FAT16 (из лекции):\n" +
                        "   • 16-разрядные адреса\n" +
                        "   • Размер кластера: 512 байт - 32 КБ\n" +
                        "   • Максимальный размер файла: 2 ГБ\n" +
                        "   • Максимальный размер тома: 2 ГБ\n" +
                        "   • Ограниченная поддержка";
            case "EXFAT":
                return "Характеристики exFAT:\n" +
                        "   • Разработана для флеш-накопителей\n" +
                        "   • Максимальный размер файла: 16 EB\n" +
                        "   • Максимальный размер тома: 128 PB\n" +
                        "   • Поддержка больших файлов";
            default:
                return "Общая информация:\n" +
                        "   • Тип файловой системы: " + fsType + "\n";
        }
    }
}