package ru.zolotuhin.ModelInfoProcess.Lab2;

import java.util.EmptyStackException;
import java.util.Scanner;

/**
 * Реализация очереди на основе массива
 */
class ArrayQueue {
    private int[] queueArray;
    private int front;
    private int rear;
    private int capacity;
    private int count;

    public ArrayQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Емкость очереди должна быть положительным числом");
        }
        this.capacity = capacity;
        this.queueArray = new int[capacity];
        this.front = 0;
        this.rear = -1;
        this.count = 0;
    }

    public ArrayQueue() {
        this(8);
    }

    public int enqueue(int element) {
        if (isFull()) {
            System.out.println("Очередь переполнена. Хотите увеличить размер? (1 - да, 0 - нет)");
            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();

            if (choice == 1) {
                resize();
            } else {
                return -1;
            }
        }

        rear = (rear + 1) % capacity;
        queueArray[rear] = element;
        count++;
        return 1;
    }

    public int dequeue() {
        if (isEmpty()) {
            return -1;
        }

        int element = queueArray[front];
        front = (front + 1) % capacity;
        count--;
        return element;
    }

    public Integer peek() {
        if (isEmpty()) {
            return null;
        }
        return queueArray[front];
    }

    public int count() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == capacity;
    }

    public Integer top() {
        return peek();
    }

    private void resize() {
        int newCapacity = capacity * 2;
        int[] newArray = new int[newCapacity];

        for (int i = 0; i < count; i++) {
            newArray[i] = queueArray[(front + i) % capacity];
        }

        queueArray = newArray;
        capacity = newCapacity;
        front = 0;
        rear = count - 1;

        System.out.println("Размер очереди увеличен до: " + newCapacity);
    }

    public void display() {
        if (isEmpty()) {
            System.out.println("Очередь пуста");
            return;
        }

        System.out.println("Очередь содержит: ");
        for (int i = 0; i < count; i++) {
            int index = (front + i) % capacity;
            System.out.println((i + 1) + ": " + queueArray[index]);
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            int index = (front + i) % capacity;
            sb.append(queueArray[index]);
            if (i < count - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
