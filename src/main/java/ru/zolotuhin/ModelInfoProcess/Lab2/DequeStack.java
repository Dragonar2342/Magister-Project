package ru.zolotuhin.ModelInfoProcess.Lab2;

class DequeStack {
    private ArrayDeque deque;

    public DequeStack(int capacity) {
        this.deque = new ArrayDeque(capacity);
    }

    public DequeStack() {
        this(8);
    }

    public int push(int element) {
        return deque.enqueueLast(element);
    }

    public int pop() {
        int result = deque.dequeueLast();
        if (result == -1) {
            return -1; // Ошибка удаления
        }
        return result;
    }

    public Integer peek() {
        return deque.peekLast();
    }

    public int count() {
        return deque.count();
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }

    public boolean isFull() {
        return deque.isFull();
    }

    public Integer top() {
        return peek();
    }

    public void display() {
        System.out.println("Стек (на основе дека) содержит: ");
        if (deque.isEmpty()) {
            System.out.println("Стек пуст");
            return;
        }

        ArrayDeque temp = new ArrayDeque(deque.count());
        int size = deque.count();

        for (int i = 0; i < size; i++) {
            int element = deque.dequeueFirst();
            temp.enqueueLast(element);
            deque.enqueueLast(element);
        }

        for (int i = size - 1; i >= 0; i--) {
            System.out.println((size - i) + ": " + temp.dequeueFirst());
        }
    }

    @Override
    public String toString() {
        if (deque.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        int size = deque.count();

        int[] temp = new int[size];
        for (int i = 0; i < size; i++) {
            int element = deque.dequeueFirst();
            temp[i] = element;
            deque.enqueueLast(element);
        }

        for (int i = size - 1; i >= 0; i--) {
            sb.append(temp[i]);
            if (i > 0) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

