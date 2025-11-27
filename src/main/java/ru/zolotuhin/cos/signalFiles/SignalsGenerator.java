package ru.zolotuhin.cos.signalFiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SignalsGenerator {
    private String filePath = "C:\\GitHub\\Magister\\src\\main\\java\\ru\\zolotuhin\\cos\\signalFiles\\";

    public List<Double> readerFile(String fileName) {
        List<Double> signals = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextInt()) {
                signals.add((double) scanner.nextInt());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return signals;
    }

    public List<Double> getCardioSignals() throws FileNotFoundException {
        return readerFile(filePath+"cardio04.txt");
    }

    public List<Double> getReoSignals() throws FileNotFoundException {
        return readerFile(filePath+"reo04.txt");
    }

    public List<Double> getSpiroSignals() throws FileNotFoundException {
        return readerFile(filePath+"spiro04.txt");
    }

    public List<Double> getVelaSignals() throws FileNotFoundException {
        return readerFile(filePath+"velo04.txt");
    }
}
