package ru.zolotuhin.OC.Lab5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Провайдер информации о дисках для Windows (через WMI)
 */
public class WindowsDiskInfoProvider implements DiskInfoProvider {

    @Override
    public List<PhysicalDisk> getPhysicalDisks() throws IOException {
        List<PhysicalDisk> disks = new ArrayList<>();

        String command = "powershell \"Get-Disk | Select-Object Number, Model, Size, " +
                "PartitionStyle, BusType, SerialNumber | Format-List\"";

        Process process = Runtime.getRuntime().exec(command);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "CP866"))) {

            String line;
            String currentDisk = null;
            String model = "";
            String size = "";
            String partitionStyle = "";
            String interfaceType = "";
            String serialNumber = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("Number")) {
                    if (currentDisk != null) {
                        disks.add(new PhysicalDisk(currentDisk, model, size,
                                partitionStyle, interfaceType, serialNumber));
                    }
                    currentDisk = extractValue(line);
                } else if (line.startsWith("Model")) {
                    model = extractValue(line);
                } else if (line.startsWith("Size")) {
                    size = formatBytes(parseLongSafe(extractValue(line)));
                } else if (line.startsWith("PartitionStyle")) {
                    partitionStyle = extractValue(line);
                } else if (line.startsWith("BusType")) {
                    interfaceType = extractValue(line);
                } else if (line.startsWith("SerialNumber")) {
                    serialNumber = extractValue(line);
                }
            }

            if (currentDisk != null) {
                disks.add(new PhysicalDisk(currentDisk, model, size,
                        partitionStyle, interfaceType, serialNumber));
            }

            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Процесс был прерван", e);
        }

        return disks;
    }

    @Override
    public List<DiskPartition> getDiskPartitions() throws IOException {
        List<DiskPartition> partitions = new ArrayList<>();

        String command = "powershell \"Get-Partition | Select-Object DiskNumber, PartitionNumber, " +
                "Type, Size, Offset, DriveLetter | Format-List\"";

        Process process = Runtime.getRuntime().exec(command);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "CP866"))) {

            String line;
            DiskPartitionBuilder builder = new DiskPartitionBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("DiskNumber")) {
                    if (builder.isComplete()) {
                        partitions.add(builder.build());
                        builder = new DiskPartitionBuilder();
                    }
                    builder.setDiskIndex(extractValue(line));
                } else if (line.startsWith("PartitionNumber")) {
                    builder.setPartitionIndex(extractValue(line));
                } else if (line.startsWith("Type")) {
                    builder.setType(extractValue(line));
                } else if (line.startsWith("Size")) {
                    builder.setSize(formatBytes(parseLongSafe(extractValue(line))));
                } else if (line.startsWith("Offset")) {
                    builder.setOffset(formatBytes(parseLongSafe(extractValue(line))));
                } else if (line.startsWith("DriveLetter")) {
                    String driveLetter = extractValue(line);
                    builder.setDriveLetter(driveLetter.isEmpty() ? "N/A" : driveLetter);
                }
            }

            if (builder.isComplete()) {
                partitions.add(builder.build());
            }

            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Процесс был прерван", e);
        }

        enrichPartitionsWithFileSystemInfo(partitions);

        return partitions;
    }

    private void enrichPartitionsWithFileSystemInfo(List<DiskPartition> partitions) {
        for (DiskPartition partition : partitions) {
            if (!partition.getDriveLetter().equals("N/A")) {
                String driveLetter = partition.getDriveLetter();
                try {
                    String command = String.format(
                            "powershell \"Get-Volume -DriveLetter %s | Select-Object FileSystem, FileSystemLabel, SizeRemaining, Size | Format-List\"",
                            driveLetter.replace(":", "")
                    );

                    Process process = Runtime.getRuntime().exec(command);

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), "CP866"))) {

                        String line;
                        String fileSystem = "N/A";
                        String volumeName = "N/A";
                        String freeSpace = "N/A";
                        String totalSpace = "N/A";

                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("FileSystem")) {
                                fileSystem = extractValue(line);
                            } else if (line.startsWith("FileSystemLabel")) {
                                volumeName = extractValue(line);
                            } else if (line.startsWith("SizeRemaining")) {
                                String freeSpaceStr = extractValue(line);
                                if (!freeSpaceStr.equals("N/A")) {
                                    freeSpace = formatBytes(parseLongSafe(freeSpaceStr));
                                }
                            } else if (line.startsWith("Size")) {
                                String totalSpaceStr = extractValue(line);
                                if (!totalSpaceStr.equals("N/A")) {
                                    totalSpace = formatBytes(parseLongSafe(totalSpaceStr));
                                }
                            }
                        }

                        process.waitFor();

                        DiskPartition enrichedPartition = new DiskPartition(
                                partition.getDiskIndex(),
                                partition.getPartitionIndex(),
                                partition.getType(),
                                partition.getSize(),
                                partition.getOffset(),
                                partition.getDriveLetter(),
                                fileSystem,
                                volumeName,
                                freeSpace,
                                totalSpace
                        );

                        int index = partitions.indexOf(partition);
                        if (index != -1) {
                            partitions.set(index, enrichedPartition);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Не удалось получить информацию для диска " + driveLetter + ": " + e.getMessage());
                }
            }
        }
    }

    private String extractValue(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex > 0) {
            String value = line.substring(colonIndex + 1).trim();
            return value.isEmpty() ? "N/A" : value;
        }
        return "N/A";
    }

    private long parseLongSafe(String str) {
        try {
            String cleanStr = str.replaceAll("[^\\d-]", "");
            if (cleanStr.isEmpty() || cleanStr.equals("-")) {
                return 0L;
            }
            return Long.parseLong(cleanStr);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка парсинга числа: '" + str + "'");
            return 0L;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private static class DiskPartitionBuilder {
        private String diskIndex;
        private String partitionIndex;
        private String type;
        private String size;
        private String offset;
        private String driveLetter = "N/A";

        public void setDiskIndex(String diskIndex) { this.diskIndex = diskIndex; }
        public void setPartitionIndex(String partitionIndex) { this.partitionIndex = partitionIndex; }
        public void setType(String type) { this.type = type; }
        public void setSize(String size) { this.size = size; }
        public void setOffset(String offset) { this.offset = offset; }
        public void setDriveLetter(String driveLetter) { this.driveLetter = driveLetter; }

        public boolean isComplete() {
            return diskIndex != null && partitionIndex != null && type != null &&
                    size != null && offset != null;
        }

        public DiskPartition build() {
            return new DiskPartition(diskIndex, partitionIndex, type, size, offset,
                    driveLetter, "N/A", "N/A", "N/A", "N/A");
        }
    }
}
