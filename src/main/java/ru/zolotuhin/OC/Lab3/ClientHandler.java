package ru.zolotuhin.OC.Lab3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Аутентификация
            out.println("Добро пожаловать в систему пейджинга!");
            out.println("Введите ваше имя:");

            username = in.readLine();
            if (username == null || username.trim().isEmpty()) {
                out.println("Ошибка: Имя не может быть пустым");
                return;
            }

            username = username.trim();
            PagerServer.registerClient(username, this);

            out.println("=== Добро пожаловать, " + username + "! ===");
            out.println(PagerServer.getOnlineUsers());
            showHelp(out);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("/quit")) {
                    break;
                } else if (inputLine.equalsIgnoreCase("/help")) {
                    showHelp(out);
                } else if (inputLine.equalsIgnoreCase("/users")) {
                    out.println(PagerServer.getOnlineUsers());
                } else if (inputLine.startsWith("@")) {
                    processMessage(inputLine);
                } else {
                    out.println("❌ Неизвестная команда. Введите /help для справки");
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка обработки клиента " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void processMessage(String input) {
        try {
            String[] parts = input.split(" ", 2);
            if (parts.length < 2) {
                out.println("❌ Формат: @получатель сообщение");
                return;
            }

            String recipient = parts[0].substring(1); // Убираем @
            String message = parts[1];

            if (recipient.equals(username)) {
                out.println("❌ Нельзя отправить сообщение самому себе");
                return;
            }

            if (message.trim().isEmpty()) {
                out.println("❌ Сообщение не может быть пустым");
                return;
            }

            PagerServer.sendMessage(username, recipient, message);

        } catch (Exception e) {
            out.println("❌ Ошибка формата команды");
        }
    }

    private void showHelp(PrintWriter out) {
        out.println("=== Справка по командам ===");
        out.println("/help - показать эту справку");
        out.println("/users - список пользователей онлайн");
        out.println("@username сообщение - отправить сообщение");
        out.println("/quit - выйти из системы");
        out.println("==========================");
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void disconnect() {
        try {
            if (username != null) {
                PagerServer.unregisterClient(username);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при отключении клиента: " + e.getMessage());
        }
    }
}
