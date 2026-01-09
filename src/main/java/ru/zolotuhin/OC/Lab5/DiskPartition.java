package ru.zolotuhin.OC.Lab5;

/**
 * Модель раздела диска
 */
public class DiskPartition {
    private final String diskIndex;
    private final String partitionIndex;
    private final String type;
    private final String size;
    private final String offset;
    private final String driveLetter;
    private final String fileSystem;
    private final String volumeName;
    private final String freeSpace;
    private final String totalSpace;

    public DiskPartition(String diskIndex, String partitionIndex, String type,
                         String size, String offset, String driveLetter,
                         String fileSystem, String volumeName, String freeSpace, String totalSpace) {
        this.diskIndex = diskIndex;
        this.partitionIndex = partitionIndex;
        this.type = type;
        this.size = size;
        this.offset = offset;
        this.driveLetter = driveLetter;
        this.fileSystem = fileSystem;
        this.volumeName = volumeName;
        this.freeSpace = freeSpace;
        this.totalSpace = totalSpace;
    }

    public String getDiskIndex() { return diskIndex; }
    public String getPartitionIndex() { return partitionIndex; }
    public String getType() { return type; }
    public String getSize() { return size; }
    public String getOffset() { return offset; }
    public String getDriveLetter() { return driveLetter; }
    public String getFileSystem() { return fileSystem; }
    public String getVolumeName() { return volumeName; }
    public String getFreeSpace() { return freeSpace; }
    public String getTotalSpace() { return totalSpace; }
}
