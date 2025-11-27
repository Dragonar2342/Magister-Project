package ru.zolotuhin.ModelInfoProcess.Lab5;

public class CongruentialGenerator {
    private long a;
    private long m;
    private long current;

    public CongruentialGenerator(long a, long m, long r0) {
        this.a = a;
        this.m = m;
        this.current = r0;
    }

    public double multiplicativeCongruential() {
        current = (a * current) % m;
        return (double) current / m;
    }

    public double mixedCongruential(long c) {
        current = (a * current + c) % m;
        return (double) current / m;
    }

    public double additiveCongruential(long prev) {
        current = (current + prev) % m;
        return (double) current / m;
    }

    public static double middleSquare(long seed) {
        long square = seed * seed;
        String squareStr = String.format("%08d", square);
        int len = squareStr.length();
        int start = (len - 4) / 2;
        String middle = squareStr.substring(start, start + 4);
        return Double.parseDouble("0." + middle);
    }

    public long getCurrent() { return current; }
    public void setCurrent(long current) { this.current = current; }
}
