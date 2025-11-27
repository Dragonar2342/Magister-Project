package ru.zolotuhin.cos.Lab3;

public class Complex {
    private final double re;
    private final double im;

    public Complex(double real, double imag) {
        re = real;
        im = imag;
    }

    public double re() { return re; }
    public double im() { return im; }
    public double abs() { return Math.hypot(re, im); }

    public Complex plus(Complex b) {
        return new Complex(re + b.re, im + b.im);
    }

    public Complex minus(Complex b) {
        return new Complex(re - b.re, im - b.im);
    }

    public Complex times(Complex b) {
        return new Complex(re * b.re - im * b.im, re * b.im + im * b.re);
    }

    public Complex times(double alpha) {
        return new Complex(alpha * re, alpha * im);
    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }
}
