package ru.zolotuhin.ModelInfoProcess.Lab5;

public class SequenceGenerator {
    private CongruentialGenerator congruentialGen;
    private DistributionGenerator distributionGen;

    public SequenceGenerator() {
        this.congruentialGen = new CongruentialGenerator(1664525, (long)Math.pow(2, 32), 123456789);
        this.distributionGen = new DistributionGenerator();
    }

    public SequenceGenerator(long a, long m, long r0, long seed) {
        this.congruentialGen = new CongruentialGenerator(a, m, r0);
        this.distributionGen = new DistributionGenerator(seed);
    }

    public double[] generateUniformSequence(int n) {
        double[] sequence = new double[n];
        for (int i = 0; i < n; i++) {
            sequence[i] = congruentialGen.multiplicativeCongruential();
        }
        return sequence;
    }

    public double[] generateExponentialSequence(double lambda, int n) {
        double[] sequence = new double[n];
        for (int i = 0; i < n; i++) {
            sequence[i] = distributionGen.generateExponential(lambda);
        }
        return sequence;
    }

    public int[] generatePoissonSequence(double lambda, int n) {
        int[] sequence = new int[n];
        for (int i = 0; i < n; i++) {
            sequence[i] = distributionGen.generatePoisson(lambda);
        }
        return sequence;
    }

    public double[] generateNormalSequence(double mu, double sigma, int n) {
        double[] sequence = new double[n];
        for (int i = 0; i < n; i++) {
            sequence[i] = distributionGen.generateNormal(mu, sigma);
        }
        return sequence;
    }
}
