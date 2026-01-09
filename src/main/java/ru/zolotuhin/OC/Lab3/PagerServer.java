package ru.zolotuhin.OC.Lab3;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PagerServer {
    private static final int PORT = 12345;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("=== Сервер пейджера запущен ===");
        System.out.println("Порт: " + PORT);
        System.out.println("Ожидание подключений...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    public static void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("✅ Пользователь '" + username + "' подключился");
        System.out.println("📊 Всего онлайн: " + clients.size());
        broadcastSystemMessage("Пользователь " + username + " в сети");
    }

    public static void unregisterClient(String username) {
        clients.remove(username);
        System.out.println("❌ Пользователь '" + username + "' отключился");
        System.out.println("📊 Всего онлайн: " + clients.size());
        broadcastSystemMessage("Пользователь " + username + " вышел из сети");
    }

    public static void sendMessage(String fromUser, String toUser, String message) {
        ClientHandler targetClient = clients.get(toUser);
        if (targetClient != null) {
            targetClient.sendMessage("📨 От " + fromUser + ": " + message);
            // Подтверждение отправителю
            ClientHandler sender = clients.get(fromUser);
            if (sender != null) {
                sender.sendMessage("✅ Сообщение доставлено " + toUser);
            }
        } else {
            ClientHandler sender = clients.get(fromUser);
            if (sender != null) {
                sender.sendMessage("❌ Ошибка: Пользователь '" + toUser + "' не в сети");
            }
        }
    }

    public static String getOnlineUsers() {
        if (clients.isEmpty()) {
            return "Нет пользователей онлайн";
        }
        return "Пользователи онлайн: " + String.join(", ", clients.keySet());
    }

    private static void broadcastSystemMessage(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage("💬 " + message);
        }
    }
}
