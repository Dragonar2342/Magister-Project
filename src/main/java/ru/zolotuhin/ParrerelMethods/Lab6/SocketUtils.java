package ru.zolotuhin.ParrerelMethods.Lab6;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketUtils {

    public static void sendMessage(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
            System.out.println("Отправлено сообщение на " + host + ":" + port + " - " + message);
        } catch (ConnectException e) {
            System.err.println("Модуль не доступен на " + host + ":" + port + " - убедитесь, что он запущен");
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения на " + host + ":" + port + ": " + e.getMessage());
        }
    }

    public static void startServer(int port, MessageHandler handler) {
        AtomicBoolean serverRunning = new AtomicBoolean(true);

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Сервер запущен на порту " + port);

                while (serverRunning.get() && !Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(
                                 new InputStreamReader(clientSocket.getInputStream()))) {

                        String message;
                        while ((message = in.readLine()) != null) {
                            System.out.println("Получено сообщение на порту " + port + ": " + message);
                            try {
                                handler.handleMessage(message);
                            } catch (Exception e) {
                                System.err.println("Ошибка обработки сообщения: " + e.getMessage());
                            }
                        }
                    } catch (SocketException e) {
                        if (serverRunning.get()) {
                            System.err.println("Ошибка сервера на порту " + port + ": " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Не удалось запустить сервер на порту " + port + ": " + e.getMessage());
            }
        }, "Server-" + port).start();
    }

    public static boolean isPortAvailable(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public interface MessageHandler {
        void handleMessage(String message);
    }
}