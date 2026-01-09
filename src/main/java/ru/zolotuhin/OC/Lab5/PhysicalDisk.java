package ru.zolotuhin.OC.Lab5;

/**
 * Модель физического диска
 */
public class PhysicalDisk {
    private final String index;
    private final String model;
    private final String size;
    private final String partitionStyle;
    private final String interfaceType;
    private final String serialNumber;

    public PhysicalDisk(String index, String model, String size,
                        String partitionStyle, String interfaceType, String serialNumber) {
        this.index = index;
        this.model = model;
        this.size = size;
        this.partitionStyle = partitionStyle;
        this.interfaceType = interfaceType;
        this.serialNumber = serialNumber;
    }

    public String getIndex() { return index; }
    public String getModel() { return model; }
    public String getSize() { return size; }
    public String getPartitionStyle() { return partitionStyle; }
    public String getInterfaceType() { return interfaceType; }
    public String getSerialNumber() { return serialNumber; }
}
