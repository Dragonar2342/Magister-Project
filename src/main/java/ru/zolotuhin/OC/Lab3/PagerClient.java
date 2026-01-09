package ru.zolotuhin.OC.Lab3;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class PagerClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=== Клиент пейджера ===");
        System.out.println("Подключение к серверу " + SERVER_HOST + ":" + SERVER_PORT);

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            // Поток для приема сообщений от сервера
            Thread messageReceiver = new Thread(new MessageReceiver(in));
            messageReceiver.setDaemon(true);
            messageReceiver.start();

            System.out.println("Подключение установлено!");
            System.out.println("Для справки введите /help");

            // Основной цикл отправки сообщений
            while (true) {
                String userInput = scanner.nextLine().trim();

                if (userInput.equalsIgnoreCase("/quit")) {
                    out.println("/quit");
                    System.out.println("Завершение работы...");
                    break;
                }

                out.println(userInput);
            }

        } catch (UnknownHostException e) {
            System.err.println("❌ Ошибка: Сервер не найден: " + SERVER_HOST);
        } catch (ConnectException e) {
            System.err.println("❌ Ошибка: Не удалось подключиться к серверу");
            System.err.println("Убедитесь, что сервер запущен на порту " + SERVER_PORT);
        } catch (IOException e) {
            System.err.println("❌ Ошибка ввода-вывода: " + e.getMessage());
        }

        System.out.println("Клиент завершил работу");
    }

    static class MessageReceiver implements Runnable {
        private BufferedReader in;

        public MessageReceiver(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(message);
                    System.out.print("> "); // Приглашение для ввода
                }
            } catch (IOException e) {
                System.err.println("Соединение с сервером разорвано");
            }
        }
    }
}
