package ru.zolotuhin.OC.Lab4;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TaskGenerator {
    private final Random random;
    private int nextPid;

    public static class TaskGeneratorConfig {
        public int minProcesses = 2;
        public int maxProcesses = 6;
        public int minMemoryPages = 1;
        public int maxMemoryPages = 10;
        public int minOperations = 3;
        public int maxOperations = 10;
        public boolean autoTerminate = true;
        public double terminationProbability = 0.1;

        public TaskGeneratorConfig() {}

        public TaskGeneratorConfig setProcessRange(int min, int max) {
            this.minProcesses = min;
            this.maxProcesses = max;
            return this;
        }

        public TaskGeneratorConfig setMemoryRange(int min, int max) {
            this.minMemoryPages = min;
            this.maxMemoryPages = max;
            return this;
        }

        public TaskGeneratorConfig setOperationsRange(int min, int max) {
            this.minOperations = min;
            this.maxOperations = max;
            return this;
        }
    }

    public static class GeneratedTask {
        public int pid;
        public String name;
        public Color color;
        public int memoryRequirement;
        public List<Integer> memoryAccessPattern;

        public GeneratedTask(int pid, String name, Color color, int memoryReq, List<Integer> accessPattern) {
            this.pid = pid;
            this.name = name;
            this.color = color;
            this.memoryRequirement = memoryReq;
            this.memoryAccessPattern = accessPattern;
        }
    }

    public static class TaskSequence {
        public List<GeneratedTask> tasks;
        public List<TaskOperation> operations;

        public TaskSequence(List<GeneratedTask> tasks, List<TaskOperation> operations) {
            this.tasks = tasks;
            this.operations = operations;
        }
    }

    public static class TaskOperation {
        public enum Type { CREATE, ACCESS, TERMINATE }
        public Type type;
        public int pid;
        public int virtualPage;
        public String description;

        public TaskOperation(Type type, int pid, String description) {
            this(type, pid, -1, description);
        }

        public TaskOperation(Type type, int pid, int virtualPage, String description) {
            this.type = type;
            this.pid = pid;
            this.virtualPage = virtualPage;
            this.description = description;
        }
    }

    private static final String[] PROCESS_NAMES = {
            "Текстовый редактор", "Веб-браузер", "Игровой движок", "База данных",
            "Графический редактор", "Медиаплеер", "Компилятор", "Файловый менеджер",
            "Антивирус", "Виртуальная машина", "Сервер приложений", "Почтовый клиент"
    };

    private static final Color[] PROCESS_COLORS = {
            new Color(135, 206, 235),  // Голубой
            new Color(144, 238, 144),  // Светло-зеленый
            new Color(255, 182, 193),  // Розовый
            new Color(221, 160, 221),  // Фиолетовый
            new Color(255, 215, 0),    // Золотой
            new Color(240, 128, 128),  // Светло-коралловый
            new Color(173, 216, 230),  // Светло-голубой
            new Color(152, 251, 152)   // Салатовый
    };

    public TaskGenerator() {
        this.random = new Random();
        this.nextPid = 1;
    }

    public TaskGenerator(long seed) {
        this.random = new Random(seed);
        this.nextPid = 1;
    }

    public TaskSequence generateTestScenario(TaskGeneratorConfig config) {
        List<GeneratedTask> tasks = new ArrayList<>();
        List<TaskOperation> operations = new ArrayList<>();

        int numProcesses = config.minProcesses + random.nextInt(config.maxProcesses - config.minProcesses + 1);

        for (int i = 0; i < numProcesses; i++) {
            GeneratedTask task = generateRandomTask();
            tasks.add(task);

            operations.add(new TaskOperation(TaskOperation.Type.CREATE, task.pid,
                    "Создание задачи: " + task.name + " (требует " + task.memoryRequirement + " страниц)"));

            int numOperations = config.minOperations + random.nextInt(config.maxOperations - config.minOperations + 1);

            for (int j = 0; j < numOperations; j++) {
                int pageIndex = random.nextInt(task.memoryAccessPattern.size());
                int virtualPage = task.memoryAccessPattern.get(pageIndex);

                operations.add(new TaskOperation(TaskOperation.Type.ACCESS, task.pid, virtualPage,
                        task.name + ": обращение к странице " + virtualPage));

                if (config.autoTerminate && random.nextDouble() < config.terminationProbability) {
                    operations.add(new TaskOperation(TaskOperation.Type.TERMINATE, task.pid,
                            "Завершение задачи: " + task.name));
                    break;
                }
            }

            if (config.autoTerminate && operations.stream()
                    .noneMatch(op -> op.type == TaskOperation.Type.TERMINATE && op.pid == task.pid)) {
                operations.add(new TaskOperation(TaskOperation.Type.TERMINATE, task.pid,
                        "Завершение задачи: " + task.name));
            }
        }

        operations = shuffleOperations(operations);

        return new TaskSequence(tasks, operations);
    }

    private GeneratedTask generateRandomTask() {
        int pid = nextPid++;
        String name = PROCESS_NAMES[random.nextInt(PROCESS_NAMES.length)];
        Color color = PROCESS_COLORS[random.nextInt(PROCESS_COLORS.length)];

        int memoryRequirement = 1 + random.nextInt(10);

        List<Integer> accessPattern = generateAccessPattern(memoryRequirement);

        return new GeneratedTask(pid, name, color, memoryRequirement, accessPattern);
    }

    private List<Integer> generateAccessPattern(int memoryRequirement) {
        List<Integer> pattern = new ArrayList<>();

        for (int i = 0; i < memoryRequirement; i++) {
            pattern.add(i);
        }

        int extraAccesses = random.nextInt(5) + 3; // 3-7 дополнительных обращений
        for (int i = 0; i < extraAccesses; i++) {
            pattern.add(random.nextInt(memoryRequirement));
        }

        Collections.shuffle(pattern, random);

        return pattern;
    }

    private List<TaskOperation> shuffleOperations(List<TaskOperation> operations) {
        List<TaskOperation> shuffled = new ArrayList<>(operations);

        List<TaskOperation> creates = new ArrayList<>();
        List<TaskOperation> others = new ArrayList<>();

        for (TaskOperation op : shuffled) {
            if (op.type == TaskOperation.Type.CREATE) {
                creates.add(op);
            } else {
                others.add(op);
            }
        }

        Collections.shuffle(others, random);

        List<TaskOperation> result = new ArrayList<>(creates);
        result.addAll(others);

        return result;
    }

    public List<GeneratedTask> generateSimpleTasks(int count) {
        List<GeneratedTask> tasks = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            tasks.add(generateRandomTask());
        }

        return tasks;
    }

    public void reset() {
        nextPid = 1;
    }
}
