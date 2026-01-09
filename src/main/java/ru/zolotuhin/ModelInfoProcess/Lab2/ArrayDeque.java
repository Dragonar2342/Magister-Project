package ru.zolotuhin.ModelInfoProcess.Lab2;

import java.util.EmptyStackException;
import java.util.Scanner;

class ArrayDeque {
    private int[] dequeArray;
    private int front;
    private int rear;
    private int capacity;
    private int count;

    public ArrayDeque(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Емкость очереди должна быть положительным числом");
        }
        this.capacity = capacity;
        this.dequeArray = new int[capacity];
        this.front = -1;
        this.rear = 0;
        this.count = 0;
    }

    public ArrayDeque() {
        this(8);
    }

    public int enqueueFirst(int element) {
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

        if (front == -1) {
            front = 0;
            rear = 0;
        } else if (front == 0) {
            front = capacity - 1;
        } else {
            front = front - 1;
        }

        dequeArray[front] = element;
        count++;
        return 1;
    }

    public int enqueueLast(int element) {
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

        if (front == -1) {
            front = 0;
            rear = 0;
        } else if (rear == capacity - 1) {
            rear = 0;
        } else {
            rear = rear + 1;
        }

        dequeArray[rear] = element;
        count++;
        return 1;
    }

    public int dequeueFirst() {
        if (isEmpty()) {
            return -1;
        }

        int element = dequeArray[front];

        if (front == rear) {
            front = -1;
            rear = -1;
        } else if (front == capacity - 1) {
            front = 0;
        } else {
            front = front + 1;
        }

        count--;
        return element;
    }

    public int dequeueLast() {
        if (isEmpty()) {
            return -1;
        }

        int element = dequeArray[rear];

        if (front == rear) {
            front = -1;
            rear = -1;
        } else if (rear == 0) {
            rear = capacity - 1;
        } else {
            rear = rear - 1;
        }

        count--;
        return element;
    }

    public Integer peekFirst() {
        if (isEmpty()) {
            return null;
        }
        return dequeArray[front];
    }

    public Integer peekLast() {
        if (isEmpty()) {
            return null;
        }
        return dequeArray[rear];
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

    private void resize() {
        int newCapacity = capacity * 2;
        int[] newArray = new int[newCapacity];

        if (front <= rear) {
            System.arraycopy(dequeArray, front, newArray, 0, count);
        } else {
            System.arraycopy(dequeArray, front, newArray, 0, capacity - front);
            System.arraycopy(dequeArray, 0, newArray, capacity - front, rear + 1);
        }

        dequeArray = newArray;
        capacity = newCapacity;
        front = 0;
        rear = count - 1;

        System.out.println("Размер дека увеличен до: " + newCapacity);
    }

    public void display() {
        if (isEmpty()) {
            System.out.println("Очередь пуста");
            return;
        }

        System.out.println("Очередь содержит: ");
        if (front <= rear) {
            for (int i = front; i <= rear; i++) {
                System.out.println((i - front + 1) + ": " + dequeArray[i]);
            }
        } else {
            for (int i = front; i < capacity; i++) {
                System.out.println((i - front + 1) + ": " + dequeArray[i]);
            }
            for (int i = 0; i <= rear; i++) {
                System.out.println((capacity - front + i + 1) + ": " + dequeArray[i]);
            }
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        if (front <= rear) {
            for (int i = front; i <= rear; i++) {
                sb.append(dequeArray[i]);
                if (i < rear) {
                    sb.append(", ");
                }
            }
        } else {
            for (int i = front; i < capacity; i++) {
                sb.append(dequeArray[i]);
                sb.append(", ");
            }
            for (int i = 0; i <= rear; i++) {
                sb.append(dequeArray[i]);
                if (i < rear) {
                    sb.append(", ");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}