package ru.zolotuhin.OC.Lab5;

import java.io.IOException;
import java.util.List;

/**
 * Сервис для работы с информацией о дисках и разделах
 */
public class DiskAnalysisService {
    private final DiskInfoProvider diskInfoProvider;
    private final FileSystemAnalyzer fileSystemAnalyzer;

    public DiskAnalysisService(DiskInfoProvider diskInfoProvider,
                               FileSystemAnalyzer fileSystemAnalyzer) {
        this.diskInfoProvider = diskInfoProvider;
        this.fileSystemAnalyzer = fileSystemAnalyzer;
    }

    public void analyzeAndDisplay() throws IOException {
        System.out.println();

        System.out.println("ФИЗИЧЕСКИЕ ДИСКИ:");
        System.out.println("-".repeat(80));
        List<PhysicalDisk> disks = diskInfoProvider.getPhysicalDisks();

        for (PhysicalDisk disk : disks) {
            displayPhysicalDiskInfo(disk);
        }

        System.out.println();

        System.out.println(" РАЗДЕЛЫ ДИСКА:");
        System.out.println("-".repeat(80));
        List<DiskPartition> partitions = diskInfoProvider.getDiskPartitions();

        for (PhysicalDisk disk : disks) {
            System.out.println("\nДиск #" + disk.getIndex() + ":");
            System.out.println("─".repeat(78));

            boolean hasPartitions = false;
            for (DiskPartition partition : partitions) {
                if (partition.getDiskIndex().equals(disk.getIndex())) {
                    displayPartitionInfo(partition);
                    hasPartitions = true;
                }
            }
            if (!hasPartitions) {
                System.out.println(" Нет разделов                                                                      │");
            }
        }

        System.out.println("\n АНАЛИЗ ФАЙЛОВЫХ СИСТЕМ:");
        System.out.println("-".repeat(80));
        analyzeFileSystems(partitions);
    }

    private void displayPhysicalDiskInfo(PhysicalDisk disk) {
        System.out.println("Диск #" + disk.getIndex() + ":");
        System.out.println("  Модель: " + disk.getModel());
        System.out.println("  Размер: " + disk.getSize());
        System.out.println("  Таблица разделов: " + disk.getPartitionStyle());
        System.out.println("  Интерфейс: " + disk.getInterfaceType());
        System.out.println("  Серийный номер: " + disk.getSerialNumber());
        System.out.println();
    }

    private void displayPartitionInfo(DiskPartition partition) {
        System.out.printf(" Раздел #%s  \n",
                partition.getPartitionIndex());

        System.out.printf("   Тип: %-12s\n   Размер: %-10s\n   Смещение: %-10s\n   Буква: %-4s\n   ФС: %-8s \n",
                partition.getType(),
                partition.getSize(),
                partition.getOffset(),
                partition.getDriveLetter(),
                partition.getFileSystem());

        if (!partition.getVolumeName().equals("N/A")) {
            System.out.printf("   Метка тома: %-64s \n",
                    partition.getVolumeName().length() > 60 ?
                            partition.getVolumeName().substring(0, 57) + "..." :
                            partition.getVolumeName());
        }

        if (!partition.getFreeSpace().equals("N/A") && !partition.getTotalSpace().equals("N/A")) {
            System.out.printf("   Свободно: %s из %s %s\n",
                    partition.getFreeSpace(),
                    partition.getTotalSpace(),
                    " ".repeat(45 - partition.getFreeSpace().length() - partition.getTotalSpace().length()));
        }

        System.out.println("─".repeat(78));
    }

    private void analyzeFileSystems(List<DiskPartition> partitions) {
        int ntfsCount = 0;
        int fat32Count = 0;
        int otherFsCount = 0;
        int unknownFsCount = 0;

        for (DiskPartition partition : partitions) {
            String fsType = partition.getFileSystem();
            if (!fsType.equals("N/A")) {
                System.out.println("\n" + fileSystemAnalyzer.analyzeFileSystem(fsType));
                System.out.println(fileSystemAnalyzer.getFileSystemDetails(fsType));

                // Статистика
                switch (fsType.toUpperCase()) {
                    case "NTFS":
                        ntfsCount++;
                        break;
                    case "FAT32":
                        fat32Count++;
                        break;
                    default:
                        otherFsCount++;
                }
            } else {
                unknownFsCount++;
            }
        }
    }
}
