package ru.zolotuhin.ModelInfoProcess.Lab3;

import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadStateDemo {
    private static final Object lock = new Object();
    private static final Object waitLock = new Object();
    private static final AtomicBoolean keepRunning = new AtomicBoolean(true);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Демонстрация всех состояний потока ===\n");

        Thread thread1 = new Thread(() -> {
            try {
                System.out.println("[Поток 1] Начало работы");
                System.out.println("[Поток 1] Переход в TIMED_WAITING (sleep 2000ms)");
                Thread.sleep(2000);
                System.out.println("[Поток 1] Возврат в RUNNABLE после sleep");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("1. Состояние NEW - поток создан, но не запущен:");
        System.out.println("   thread1.getState() = " + thread1.getState());

        thread1.start();
        Thread.sleep(100);

        System.out.println("\n2. После thread1.start():");
        System.out.println("   thread1.getState() = " + thread1.getState() + " (TIMED_WAITING из-за Thread.sleep())");

        Thread thread2 = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("\n[Поток 2] Захватил монитор 'lock', работает...");
                    Thread.sleep(1500);
                    System.out.println("[Поток 2] Освобождает монитор");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread thread3 = new Thread(() -> {
            System.out.println("\n[Поток 3] Пытается захватить монитор 'lock'...");
            synchronized (lock) {
                System.out.println("[Поток 3] Успешно захватил монитор");
            }
        });

        thread2.start();
        Thread.sleep(100);

        thread3.start();
        Thread.sleep(100);

        System.out.println("\n3. Состояние BLOCKED - поток ожидает освобождения монитора:");
        System.out.println("   thread3.getState() = " + thread3.getState());

        Thread thread4 = new Thread(() -> {
            synchronized (waitLock) {
                try {
                    System.out.println("\n[Поток 4] Захватил монитор 'waitLock'");
                    System.out.println("[Поток 4] Вызов wait() - переход в WAITING");
                    waitLock.wait();
                    System.out.println("[Поток 4] Получил notify(), продолжает работу");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread4.start();
        Thread.sleep(150);

        System.out.println("\n4. Состояние WAITING - поток вызвал wait():");
        System.out.println("   thread4.getState() = " + thread4.getState());

        Thread thread5 = new Thread(() -> {
            System.out.println("\n[Поток 5] Начало активных вычислений (RUNNABLE)");
            long result = 0;
            for (int i = 0; i < 10000000; i++) {
                result += i;
                if (i % 2000000 == 0 && i > 0) {
                    System.out.println("[Поток 5] Выполнено " + (i/1000000) + " млн итераций");
                }
            }
            System.out.println("[Поток 5] Вычисления завершены, результат = " + result);
        });

        thread5.start();
        Thread.sleep(50);

        System.out.println("\n5. Состояние RUNNABLE - поток выполняет активные вычисления:");
        System.out.println("   thread5.getState() = " + thread5.getState());

        Thread.sleep(1000);
        System.out.println("\n6. Вызов notify() для пробуждения потока 4:");
        synchronized (waitLock) {
            waitLock.notify();
        }

        System.out.println("\n7. Ожидание завершения всех потоков (join)...");

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        thread5.join();

        System.out.println("\n8. Состояние TERMINATED - все потоки завершены:");
        System.out.println("   thread1.getState() = " + thread1.getState());
        System.out.println("   thread2.getState() = " + thread2.getState());
        System.out.println("   thread3.getState() = " + thread3.getState());
        System.out.println("   thread4.getState() = " + thread4.getState());
        System.out.println("   thread5.getState() = " + thread5.getState());

        System.out.println("\n=== Демонстрация завершена ===");

        // 9. Дополнительно: демонстрация перехода между состояниями
        System.out.println("\n=== Схема переходов состояний потока ===");
        System.out.println("NEW → start() → RUNNABLE");
        System.out.println("RUNNABLE → sleep()/wait(timeout) → TIMED_WAITING");
        System.out.println("RUNNABLE → wait() → WAITING");
        System.out.println("RUNNABLE → попытка захвата занятого монитора → BLOCKED");
        System.out.println("Любое состояние → завершение run() → TERMINATED");
    }
}