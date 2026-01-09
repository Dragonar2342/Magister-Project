package ru.zolotuhin.ParrerelMethods.Lab8;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketUtils {

    // Константы для подключения к MessageQueueManager
    public static final String MQ_HOST = "localhost";
    public static final int MQ_PORT = 5555;

    // Регистрация компонента в MQM
    public static void registerComponent(String componentName) {
        try {
            String response = sendToMQ("REGISTER|" + componentName);
            System.out.println(componentName + ": " + response);
        } catch (IOException e) {
            System.err.println("Не удалось зарегистрировать компонент " + componentName + ": " + e.getMessage());
        }
    }

    // Отправка сообщения через MQM
    public static boolean sendMessageViaMQ(String target, String from, String content, MessageType type) {
        try {
            String command = String.format("PRODUCE|%s|%s|%s|%s",
                    target, from, content, type);
            String response = sendToMQ(command);
            return response.startsWith("OK");
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
            return false;
        }
    }

    // Получение сообщения из MQM
    public static Message receiveMessageViaMQ(String componentName) {
        try {
            String response = sendToMQ("CONSUME|" + componentName);

            if (response.startsWith("MESSAGE|")) {
                String[] parts = response.substring(8).split("\\|");
                if (parts.length >= 4) {
                    String from = parts[0];
                    String content = parts[1];
                    MessageType type = MessageType.valueOf(parts[2]);
                    return new Message(from, content, type);
                }
            }
            return null;
        } catch (IOException e) {
            System.err.println("Ошибка получения сообщения: " + e.getMessage());
            return null;
        }
    }

    // Получение размера очереди
    public static int getQueueSize(String componentName) {
        try {
            String response = sendToMQ("GET_SIZE|" + componentName);
            if (response.startsWith("SIZE|")) {
                return Integer.parseInt(response.substring(5));
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return 0;
    }

    // Очистка очереди
    public static void clearQueue(String componentName) {
        try {
            sendToMQ("CLEAR|" + componentName);
        } catch (IOException e) {
            // Игнорируем
        }
    }

    // Проверка доступности MQM
    public static boolean isMQMAvailable() {
        try (Socket socket = new Socket(MQ_HOST, MQ_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Вспомогательный метод для отправки команд в MQM
    private static String sendToMQ(String command) throws IOException {
        try (Socket socket = new Socket(MQ_HOST, MQ_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(command);
            return in.readLine();
        }
    }

    // Существующие методы (оставляем для обратной совместимости)
    public static void sendMessage(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    public static void startServer(int port, MessageHandler handler) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(
                                 new InputStreamReader(clientSocket.getInputStream()))) {

                        String message = in.readLine();
                        if (message != null) {
                            handler.handleMessage(message);
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка обработки сообщения: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Не удалось запустить сервер: " + e.getMessage());
            }
        }, "Server-" + port).start();
    }

    public interface MessageHandler {
        void handleMessage(String message);
    }
}