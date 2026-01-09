package ru.zolotuhin.OC.Lab5;

import java.io.IOException;
import java.util.List;

/**
 * Интерфейс для получения информации о дисках
 */
public interface DiskInfoProvider {
    List<PhysicalDisk> getPhysicalDisks() throws IOException;
    List<DiskPartition> getDiskPartitions() throws IOException;
}
