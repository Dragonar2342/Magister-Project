package ru.zolotuhin.ParrerelMethods.Lab8;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MessageQueueManager {
    private final ConcurrentHashMap<String, BlockingQueue<Message>> queues;
    private final ConcurrentHashMap<String, Long> lastActivityTimes;
    private final SimpleDateFormat timeFormat;
    private ServerSocket serverSocket;
    private final int port;
    private volatile boolean running = true;

    private static final int QUEUE_CAPACITY = 100;
    private static final int DEFAULT_PORT = 5555;
    private int totalMessagesProcessed = 0;

    public MessageQueueManager(int port) throws IOException {
        this.port = port;
        queues = new ConcurrentHashMap<>();
        lastActivityTimes = new ConcurrentHashMap<>();
        timeFormat = new SimpleDateFormat("HH:mm:ss");

        serverSocket = new ServerSocket(port);

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║       MessageQueueManager - Центральный сервер      ║");
        System.out.println("║        Порт: " + port + "                                   ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        logMessage("Сервер запущен. Ожидание подключений компонентов...");

        // Запускаем сервер
        startServer();

        // Предварительная регистрация компонентов
        registerDefaultComponents();

        // Запускаем консольный интерфейс
        startConsoleInterface();
    }

    private void startServer() {
        new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();

                } catch (IOException e) {
                    if (running) {
                        logMessage("Ошибка сервера: " + e.getMessage());
                    }
                }
            }
        }, "Server-Thread").start();
    }

    private class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String command = in.readLine();
                if (command != null) {
                    String response = processCommand(command);
                    out.println(response);
                }

            } catch (IOException e) {
                logMessage("Клиент отключился: " + socket.getInetAddress());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Игнорируем
                }
            }
        }

        private String processCommand(String command) {
            try {
                String[] parts = command.split("\\|");
                String cmd = parts[0];

                switch (cmd) {
                    case "REGISTER":
                        ensureComponentRegistered(parts[1]);
                        return "OK|Компонент зарегистрирован";

                    case "PRODUCE":
                        String target = parts[1];
                        String from = parts[2];
                        String content = parts[3];
                        MessageType type = MessageType.valueOf(parts[4]);

                        Message msg = new Message(from, content, type);
                        boolean success = produce(target, msg);
                        return success ? "OK|Сообщение отправлено" : "ERROR|Очередь переполнена";

                    case "CONSUME":
                        Message consumed = consume(parts[1]);
                        if (consumed != null) {
                            return "MESSAGE|" + consumed.getFrom() + "|" +
                                    consumed.getContent() + "|" +
                                    consumed.getType() + "|" +
                                    consumed.getTimestamp();
                        }
                        return "NO_MESSAGE";

                    case "GET_SIZE":
                        int size = getQueueSize(parts[1]);
                        return "SIZE|" + size;

                    case "GET_ACTIVITY":
                        long time = getLastActivityTime(parts[1]);
                        return "ACTIVITY|" + time;

                    case "CLEAR":
                        clearQueue(parts[1]);
                        return "OK|Очередь очищена";

                    case "GET_COMPONENTS":
                        return "COMPONENTS|" + String.join(",", getRegisteredComponents());

                    case "PING":
                        return "PONG";

                    default:
                        return "ERROR|Неизвестная команда";
                }
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }
    }

    private void registerDefaultComponents() {
        String[] defaultComponents = {
                "Autopilot", "Camera", "ParkingSpot",
                "Brake", "Steering", "StatusWindow"
        };

        for (String comp : defaultComponents) {
            ensureComponentRegistered(comp);
        }
    }

    public void ensureComponentRegistered(String componentName) {
        if (!queues.containsKey(componentName)) {
            queues.put(componentName, new LinkedBlockingQueue<>(QUEUE_CAPACITY));
            lastActivityTimes.put(componentName, System.currentTimeMillis());
            logMessage("Зарегистрирован компонент: " + componentName);
        }
    }

    public boolean produce(String targetComponent, Message message) {
        ensureComponentRegistered(targetComponent);
        ensureComponentRegistered(message.getFrom());

        BlockingQueue<Message> queue = queues.get(targetComponent);
        if (queue != null) {
            boolean success = queue.offer(message);

            if (success) {
                lastActivityTimes.put(message.getFrom(), System.currentTimeMillis());
                lastActivityTimes.put(targetComponent, System.currentTimeMillis());

                String timestamp = timeFormat.format(new Date(message.getTimestamp()));
                System.out.printf("[%s] [OUT] %s -> %s: %s%n",
                        timestamp, message.getFrom(), targetComponent, message.getContent());

                totalMessagesProcessed++;
                return true;
            } else {
                logMessage("ОШИБКА: Очередь " + targetComponent + " переполнена!");
                return false;
            }
        }
        return false;
    }

    public Message consume(String componentName) {
        try {
            BlockingQueue<Message> queue = queues.get(componentName);
            if (queue != null) {
                Message message = queue.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    lastActivityTimes.put(componentName, System.currentTimeMillis());

                    String timestamp = timeFormat.format(new Date(message.getTimestamp()));
                    System.out.printf("[%s] [IN]  %s <- %s: %s%n",
                            timestamp, componentName, message.getFrom(), message.getContent());

                    totalMessagesProcessed++;
                    return message;
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public int getQueueSize(String componentName) {
        BlockingQueue<Message> queue = queues.get(componentName);
        return queue != null ? queue.size() : 0;
    }

    public long getLastActivityTime(String componentName) {
        Long time = lastActivityTimes.get(componentName);
        return time != null ? time : 0;
    }

    public boolean isComponentActive(String componentName) {
        Long lastTime = lastActivityTimes.get(componentName);
        if (lastTime == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastTime) < 10000;
    }

    public void clearQueue(String componentName) {
        BlockingQueue<Message> queue = queues.get(componentName);
        if (queue != null) {
            queue.clear();
            logMessage("Очередь " + componentName + " очищена");
        }
    }

    public Set<String> getRegisteredComponents() {
        return new TreeSet<>(queues.keySet());
    }

    private void logMessage(String message) {
        String timestamp = timeFormat.format(new Date());
        System.out.println("[" + timestamp + "] " + message);
    }

    private void startConsoleInterface() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);

            while (running) {
                try {
                    System.out.print("\nMQM> ");
                    String command = scanner.nextLine();

                    if (command != null && !command.trim().isEmpty()) {
                        handleCommand(command);
                    }

                } catch (Exception e) {
                    // Игнорируем
                }
            }

            scanner.close();
        }, "Console-Interface").start();
    }

    private void handleCommand(String command) {
        String[] parts = command.trim().split("\\s+");

        if (parts.length == 0) return;

        switch (parts[0].toLowerCase()) {
            case "list":
                printQueueStatus();
                break;

            case "stats":
                printStatistics();
                break;

            case "clear":
                if (parts.length > 1) {
                    clearQueue(parts[1]);
                } else {
                    logMessage("Использование: clear <имя_компонента>");
                }
                break;

            case "exit":
                shutdown();
                break;

            case "help":
                System.out.println("\n=== КОМАНДЫ ===");
                System.out.println("list    - Показать состояние очередей");
                System.out.println("stats   - Показать статистику");
                System.out.println("clear   - Очистить очередь");
                System.out.println("exit    - Завершить работу");
                System.out.println("help    - Показать справку");
                break;

            default:
                logMessage("Неизвестная команда");
        }
    }

    private void printQueueStatus() {
        System.out.println("\n=== СОСТОЯНИЕ ОЧЕРЕДЕЙ ===");

        if (queues.isEmpty()) {
            System.out.println("Очереди пусты");
            return;
        }

        List<String> sortedComponents = new ArrayList<>(queues.keySet());
        Collections.sort(sortedComponents);

        for (String component : sortedComponents) {
            int size = getQueueSize(component);
            long lastActive = getLastActivityTime(component);
            long secondsAgo = (System.currentTimeMillis() - lastActive) / 1000;
            boolean active = isComponentActive(component);

            String status = active ? "АКТИВЕН" : "НЕАКТИВЕН";
            String timeInfo = secondsAgo < 60 ?
                    secondsAgo + " сек назад" :
                    (secondsAgo / 60) + " мин назад";

            System.out.printf("%-15s: сообщений=%3d, статус=%-10s, активность=%s%n",
                    component, size, status, timeInfo);
        }
    }

    private void printStatistics() {
        System.out.println("\n=== СТАТИСТИКА ===");
        System.out.println("Всего сообщений: " + totalMessagesProcessed);
        System.out.println("Компонентов: " + queues.size());
        System.out.println("Порт: " + port);
    }

    public void shutdown() {
        logMessage("Завершение работы...");
        running = false;

        try {
            serverSocket.close();
        } catch (IOException e) {
            // Игнорируем
        }

        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            int port = DEFAULT_PORT;
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }

            new MessageQueueManager(port);

        } catch (Exception e) {
            System.err.println("Ошибка запуска: " + e.getMessage());
        }
    }
}