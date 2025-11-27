package ru.zolotuhin.ModelInfoProcess.Lab5;

// DistributionGenerator.java
import java.util.Random;

public class DistributionGenerator {
    private Random random;

    public DistributionGenerator() {
        this.random = new Random();
    }

    public DistributionGenerator(long seed) {
        this.random = new Random(seed);
    }

    public double generateNormal(double mu, double sigma) {
        double sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += random.nextDouble();
        }
        return mu + sigma * (sum - 6);
    }

    public double generateExponential(double lambda) {
        return -Math.log(random.nextDouble()) / lambda;
    }

    public int generatePoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);

        return k - 1;
    }

    public double generateUniform() {
        return random.nextDouble();
    }

    public double generateUniform(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
}
