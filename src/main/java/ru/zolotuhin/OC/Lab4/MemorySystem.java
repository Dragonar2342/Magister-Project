package ru.zolotuhin.OC.Lab4;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MemorySystem {
    private final int pageSize;
    private final int physicalPages;
    private final Map<Integer, ProcessMemoryInfo> processes;
    private final String[] physicalMemory;
    private final Queue<Integer> fifoQueue;
    private final List<MemoryEvent> eventLog;

    public static class ProcessMemoryInfo {
        public int pid;
        public String name;
        public Map<Integer, Integer> pageTable;
        public Color color;
        public int memoryRequirement;
        public int pagesAllocated;

        public ProcessMemoryInfo(int pid, String name, Color color, int memoryReq) {
            this.pid = pid;
            this.name = name;
            this.color = color;
            this.memoryRequirement = memoryReq;
            this.pageTable = new HashMap<>();
            this.pagesAllocated = 0;
        }
    }

    public static class MemoryEvent {
        public enum Type { CREATE_PROCESS, ACCESS_MEMORY, TERMINATE_PROCESS, PAGE_FAULT, PAGE_REPLACEMENT }
        public Type type;
        public int processId;
        public int virtualPage;
        public int physicalFrame;
        public String description;
        public long timestamp;

        public MemoryEvent(Type type, int processId, String description) {
            this(type, processId, -1, -1, description);
        }

        public MemoryEvent(Type type, int processId, int virtualPage, int physicalFrame, String description) {
            this.type = type;
            this.processId = processId;
            this.virtualPage = virtualPage;
            this.physicalFrame = physicalFrame;
            this.description = description;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class MemoryStats {
        public int totalPages;
        public int usedPages;
        public int freePages;
        public int pageFaults;
        public int pageReplacements;
        public double memoryUtilization;

        public MemoryStats(int total, int used, int free, int faults, int replacements) {
            this.totalPages = total;
            this.usedPages = used;
            this.freePages = free;
            this.pageFaults = faults;
            this.pageReplacements = replacements;
            this.memoryUtilization = total > 0 ? (double) used / total * 100 : 0;
        }
    }

    public MemorySystem(int pageSize, int physicalPages) {
        this.pageSize = pageSize;
        this.physicalPages = physicalPages;
        this.processes = new HashMap<>();
        this.physicalMemory = new String[physicalPages];
        this.fifoQueue = new LinkedList<>();
        this.eventLog = new ArrayList<>();

        for (int i = 0; i < physicalPages; i++) {
            physicalMemory[i] = null;
        }

        logEvent(new MemoryEvent(MemoryEvent.Type.CREATE_PROCESS, 0,
                "Инициализация системы памяти. Страниц: " + physicalPages + ", Размер: " + pageSize + " байт"));
    }

    public ProcessMemoryInfo createProcess(int pid, String name, Color color, int memoryRequirement) {
        if (processes.containsKey(pid)) {
            logEvent(new MemoryEvent(MemoryEvent.Type.CREATE_PROCESS, pid,
                    "Ошибка: процесс " + pid + " уже существует"));
            return null;
        }

        ProcessMemoryInfo process = new ProcessMemoryInfo(pid, name, color, memoryRequirement);
        processes.put(pid, process);

        logEvent(new MemoryEvent(MemoryEvent.Type.CREATE_PROCESS, pid,
                "Создан процесс: " + name + " (PID: " + pid + "), требуется " + memoryRequirement + " страниц"));

        return process;
    }

    public boolean allocateMemory(int pid, int virtualPage) {
        if (!processes.containsKey(pid)) {
            logEvent(new MemoryEvent(MemoryEvent.Type.ACCESS_MEMORY, pid, virtualPage, -1,
                    "Ошибка: процесс не существует"));
            return false;
        }

        ProcessMemoryInfo process = processes.get(pid);

        if (process.pageTable.containsKey(virtualPage)) {
            int frame = process.pageTable.get(virtualPage);
            logEvent(new MemoryEvent(MemoryEvent.Type.ACCESS_MEMORY, pid, virtualPage, frame,
                    "Попадание в память: страница " + virtualPage + " → кадр " + frame));
            return true;
        }

        logEvent(new MemoryEvent(MemoryEvent.Type.PAGE_FAULT, pid, virtualPage, -1,
                "Page fault: страница " + virtualPage + " отсутствует в памяти"));

        int freeFrame = findFreeFrame();

        if (freeFrame != -1) {
            process.pageTable.put(virtualPage, freeFrame);
            physicalMemory[freeFrame] = "P" + pid + ":V" + virtualPage;
            fifoQueue.offer(freeFrame);
            process.pagesAllocated++;

            logEvent(new MemoryEvent(MemoryEvent.Type.ACCESS_MEMORY, pid, virtualPage, freeFrame,
                    "Выделен кадр " + freeFrame + " для страницы " + virtualPage));
            return true;
        } else {
            return performPageReplacement(pid, virtualPage);
        }
    }

    private int findFreeFrame() {
        for (int i = 0; i < physicalPages; i++) {
            if (physicalMemory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private boolean performPageReplacement(int pid, int virtualPage) {
        if (fifoQueue.isEmpty()) {
            logEvent(new MemoryEvent(MemoryEvent.Type.PAGE_REPLACEMENT, pid, virtualPage, -1,
                    "Ошибка: очередь FIFO пуста"));
            return false;
        }

        int frameToReplace = fifoQueue.poll();
        logEvent(new MemoryEvent(MemoryEvent.Type.PAGE_REPLACEMENT, pid, virtualPage, frameToReplace,
                "Замещение: вытесняем кадр " + frameToReplace));

        removePageFromFrame(frameToReplace);

        ProcessMemoryInfo process = processes.get(pid);
        process.pageTable.put(virtualPage, frameToReplace);
        physicalMemory[frameToReplace] = "P" + pid + ":V" + virtualPage;
        fifoQueue.offer(frameToReplace);
        process.pagesAllocated++;

        return true;
    }

    private void removePageFromFrame(int frame) {
        for (ProcessMemoryInfo process : processes.values()) {
            Iterator<Map.Entry<Integer, Integer>> it = process.pageTable.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> entry = it.next();
                if (entry.getValue() == frame) {
                    it.remove();
                    process.pagesAllocated--;
                    break;
                }
            }
        }
    }

    public boolean terminateProcess(int pid) {
        if (!processes.containsKey(pid)) {
            logEvent(new MemoryEvent(MemoryEvent.Type.TERMINATE_PROCESS, pid,
                    "Ошибка: процесс " + pid + " не существует"));
            return false;
        }

        ProcessMemoryInfo process = processes.get(pid);

        for (int frame : process.pageTable.values()) {
            physicalMemory[frame] = null;
        }

        fifoQueue.removeIf(frame -> physicalMemory[frame] == null);

        processes.remove(pid);

        logEvent(new MemoryEvent(MemoryEvent.Type.TERMINATE_PROCESS, pid,
                "Процесс завершен: " + process.name + ", освобождено " + process.pagesAllocated + " страниц"));

        return true;
    }

    public MemoryStats getMemoryStats() {
        int usedPages = 0;
        for (String frame : physicalMemory) {
            if (frame != null) usedPages++;
        }

        int pageFaults = 0;
        int pageReplacements = 0;
        for (MemoryEvent event : eventLog) {
            if (event.type == MemoryEvent.Type.PAGE_FAULT) pageFaults++;
            if (event.type == MemoryEvent.Type.PAGE_REPLACEMENT) pageReplacements++;
        }

        return new MemoryStats(
                physicalPages,
                usedPages,
                physicalPages - usedPages,
                pageFaults,
                pageReplacements
        );
    }

    public String[] getPhysicalMemorySnapshot() {
        return Arrays.copyOf(physicalMemory, physicalMemory.length);
    }

    public List<ProcessMemoryInfo> getActiveProcesses() {
        return new ArrayList<>(processes.values());
    }

    public List<MemoryEvent> getEventLog() {
        return new ArrayList<>(eventLog);
    }

    public int getPhysicalPages() {
        return physicalPages;
    }

    public int getPageSize() {
        return pageSize;
    }

    private void logEvent(MemoryEvent event) {
        eventLog.add(event);
    }

    public void clearMemory() {
        for (int i = 0; i < physicalPages; i++) {
            physicalMemory[i] = null;
        }

        processes.clear();

        fifoQueue.clear();

        logEvent(new MemoryEvent(MemoryEvent.Type.CREATE_PROCESS, 0,
                "Память очищена"));
    }
}
