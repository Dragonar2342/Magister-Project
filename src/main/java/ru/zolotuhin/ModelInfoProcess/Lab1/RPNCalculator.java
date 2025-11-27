package ru.zolotuhin.ModelInfoProcess.Lab1;

import java.util.EmptyStackException;

public class RPNCalculator {
    private final Stack stack;

    public RPNCalculator() {
        this.stack = new Stack(100);
    }

    public void compute(String expr) throws ArithmeticException, EmptyStackException {
        System.out.println("Вычисление выражения: " + expr);
        System.out.println("Ввод\tОперация\tСтек после");

        for (String token : expr.split("\\s+")) {
            if (token.isEmpty()) continue;

            System.out.print(token + "\t");

            switch (token) {
                case "+":
                    System.out.print("Сложить\t\t");
                    checkOperandsCount(2);
                    int bAdd = stack.pop();
                    int aAdd = stack.pop();
                    stack.push(aAdd + bAdd);
                    break;

                case "-":
                    System.out.print("Вычесть\t\t");
                    checkOperandsCount(2);
                    int bSub = stack.pop();
                    int aSub = stack.pop();
                    stack.push(aSub - bSub);
                    break;

                case "*":
                    System.out.print("Умножить\t");
                    checkOperandsCount(2);
                    int bMul = stack.pop();
                    int aMul = stack.pop();
                    stack.push(aMul * bMul);
                    break;

                case "/":
                    System.out.print("Делить\t\t");
                    checkOperandsCount(2);
                    int divisor = stack.pop();
                    if (divisor == 0) {
                        throw new ArithmeticException("Деление на ноль");
                    }
                    int dividend = stack.pop();
                    stack.push(dividend / divisor);
                    break;

                case "^":
                    System.out.print("Степень\t\t");
                    checkOperandsCount(2);
                    int exponent = stack.pop();
                    int base = stack.pop();
                    stack.push((int) Math.pow(base, exponent));
                    break;

                default:
                    System.out.print("Положить\t");
                    try {
                        double value = Double.parseDouble(token);
                        stack.push((int) value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Неверный токен: '" + token + "'");
                    }
                    break;
            }

            System.out.println(stackToString());
        }

        if (stack.count() != 1) {
            throw new IllegalStateException("Некорректное выражение: в стеке осталось " + stack.count() + " элементов");
        }

        System.out.println("Финальный ответ: " + stack.pop());
    }

    private void checkOperandsCount(int required) {
        if (stack.count() < required) {
            throw new EmptyStackException();
        }
    }

    private String stackToString() {
        if (stack.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        Stack temp = new Stack(stack.count());
        int count = stack.count();

        for (int i = 0; i < count; i++) {
            temp.push(stack.peek());
            stack.pop();
        }

        for (int i = 0; i < count; i++) {
            int value = temp.pop();
            stack.push(value);
            sb.append(value);
            if (i < count - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    public int evaluate(String expr) {
        Stack evalStack = new Stack(100);

        for (String token : expr.split("\\s+")) {
            if (token.isEmpty()) continue;

            switch (token) {
                case "+":
                    int b1 = evalStack.pop();
                    int a1 = evalStack.pop();
                    evalStack.push(a1 + b1);
                    break;
                case "-":
                    int b2 = evalStack.pop();
                    int a2 = evalStack.pop();
                    evalStack.push(a2 - b2);
                    break;
                case "*":
                    int b3 = evalStack.pop();
                    int a3 = evalStack.pop();
                    evalStack.push(a3 * b3);
                    break;
                case "/":
                    int divisor = evalStack.pop();
                    if (divisor == 0) {
                        throw new ArithmeticException("Деление на ноль");
                    }
                    int dividend = evalStack.pop();
                    evalStack.push(dividend / divisor);
                    break;
                case "^":
                    int exponent = evalStack.pop();
                    int base = evalStack.pop();
                    evalStack.push((int) Math.pow(base, exponent));
                    break;
                default:
                    try {
                        evalStack.push(Integer.parseInt(token));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Неверный токен: '" + token + "'");
                    }
                    break;
            }
        }

        if (evalStack.count() != 1) {
            throw new IllegalStateException("Некорректное выражение");
        }

        return evalStack.pop();
    }
}
