package ru.zolotuhin.ModelInfoProcess.Lab1;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Scanner;

public class Stack {
    private int capacity;
    private int[] stackArray;
    private int top;

    public Stack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Емкость стека должна быть положительным числом");
        }
        this.capacity = capacity;
        this.stackArray = new int[capacity];
        this.top = -1;
    }

    public Stack() {
        this.capacity = 8;
        this.stackArray = new int[capacity];
        this.top = -1;
    }

    public void push(int element) {
        if (isFull()) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Хотите увеличить размер стека? Введите 1 если согласны.");
            while (true) {
                if (scanner.nextInt() == 1) {
                    capacity++;
                    int[] temp = new int[capacity];
                    System.arraycopy(stackArray, 0, temp, 0, temp.length - 1);
                    System.out.print(Arrays.toString(temp));


                    stackArray = new int[capacity];
                    System.arraycopy(temp, 0, stackArray, 0, stackArray.length);
                    stackArray[stackArray.length - 1] = element;
                    break;
                } else {
                    throw new IllegalStateException("Стек переполнен. Невозможно добавить элемент: " + element);
                }

            }
        }
        stackArray[++top] = element;
    }

    public int pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return stackArray[top--];
    }

    public int peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return stackArray[top];
    }

    public int count() {
        return top + 1;
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public boolean isFull() {
        return top == capacity - 1;
    }

    public int top() {
        return peek();
    }

    public void display() {
        if (isEmpty()) {
            System.out.println("Стек пуст");
            return;
        }

        System.out.println("Стек содержит: ");
        for (int i = 0; i <= top; i++) {
            System.out.println((i+1) + ": " + stackArray[i]);
        }
        System.out.println();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i <= top; i++) {
            sb.append(stackArray[i]);
            if (i < top) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}