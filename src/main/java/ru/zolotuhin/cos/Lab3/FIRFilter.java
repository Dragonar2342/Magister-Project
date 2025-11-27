package ru.zolotuhin.cos.Lab3;

public class FIRFilter extends WavFileHandler implements AudioProcessor {
    private double cutoffFrequency;
    private boolean isHighPass;
    private int filterOrder;
    private WindowType windowType;

    public enum WindowType {
        RECTANGULAR, HAMMING, HANNING, BARTLETT, BLACKMAN
    }

    public FIRFilter(double cutoffFrequency, boolean isHighPass, int filterOrder, WindowType windowType) {
        this.cutoffFrequency = cutoffFrequency;
        this.isHighPass = isHighPass;
        this.filterOrder = filterOrder;
        this.windowType = windowType;
    }

    @Override
    public void processAudio(String inputPath, String outputPath, String soundName) throws Exception {
        short[] audioData = readAudioData(inputPath);
        double[] impulseResponse = createImpulseResponse();
        applyWindow(impulseResponse);

        addChartToTabbedPane(
                "Импульсная характеристика (N=" + filterOrder + ") " + soundName,
                "Импульсная характеристика фильтра", // Добавлен заголовок графика
                impulseResponse,
                "Отсчеты",
                "Значение"
        );

        double[] frequencyResponse = calculateFrequencyResponse(impulseResponse);
        addChartToTabbedPane(
                "АЧХ (линейная, N=" + filterOrder + ") " + soundName,
                "Амплитудно-частотная характеристика",
                frequencyResponse,
                "Частота (Гц)",
                "Амплитуда"
        );

        double[] frequencyResponseDB = new double[frequencyResponse.length];
        for (int i = 0; i < frequencyResponse.length; i++) {
            frequencyResponseDB[i] = 20 * Math.log10(frequencyResponse[i]);
        }
        addChartToTabbedPane(
                "АЧХ (децибелы, N=" + filterOrder + ") " + soundName,
                "АЧХ в децибелах",
                frequencyResponseDB,
                "Частота (Гц)",
                "dB"
        );

        short[] filteredAudio = applyFilter(audioData, impulseResponse);
        writeAudioData(outputPath, filteredAudio);

        // Отображаем результаты
        addChartToTabbedPane(
                "Исходный сигнал (N=" + filterOrder + ") " + soundName,
                "Исходный аудиосигнал",
                toDoubleArray(audioData),
                "Отсчеты",
                "Амплитуда"
        );

        addChartToTabbedPane(
                "Отфильтрованный сигнал (N=" + filterOrder + ") "+ soundName,
                "Отфильтрованный аудиосигнал",
                toDoubleArray(filteredAudio),
                "Отсчеты",
                "Амплитуда"
        );
    }

    private double[] createImpulseResponse() {
        double[] h = new double[filterOrder];
        double fc = cutoffFrequency / SAMPLE_RATE;
        int M = filterOrder - 1;

        for (int i = 0; i < filterOrder; i++) {
            if (i != M / 2) {
                h[i] = Math.sin(2 * Math.PI * fc * (i - M / 2)) / (Math.PI * (i - M / 2));
            } else {
                h[i] = 2 * fc;
            }

            // Для ВЧ фильтра вычитаем из all-pass
            if (isHighPass) {
                if (i == M / 2) {
                    h[i] = 1 - h[i];
                } else {
                    h[i] = -h[i];
                }
            }
        }

        return h;
    }

    private void applyWindow(double[] impulseResponse) {
        int M = filterOrder - 1;

        for (int i = 0; i < impulseResponse.length; i++) {
            double w = 1.0;

            switch (windowType) {
                case HAMMING:
                    w = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / M);
                    break;
                case HANNING:
                    w = 0.5 * (1 - Math.cos(2 * Math.PI * i / M));
                    break;
                case BARTLETT:
                    w = 1.0 - Math.abs(2.0 * i / M - 1.0);
                    break;
                case BLACKMAN:
                    w = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / M) +
                            0.08 * Math.cos(4 * Math.PI * i / M);
                    break;
                default:
                    break;
            }

            impulseResponse[i] *= w;
        }
    }

    private double[] calculateFrequencyResponse(double[] impulseResponse) {
        int fftSize = 1;
        while (fftSize < impulseResponse.length) {
            fftSize *= 2;
        }
        fftSize *= 4;

        Complex[] hPadded = new Complex[fftSize];

        for (int i = 0; i < fftSize; i++) {
            if (i < impulseResponse.length) {
                hPadded[i] = new Complex(impulseResponse[i], 0);
            } else {
                hPadded[i] = new Complex(0, 0);
            }
        }

        Complex[] spectrum = FFT.fft(hPadded);
        double[] magnitude = new double[fftSize / 2];

        for (int i = 0; i < magnitude.length; i++) {
            magnitude[i] = spectrum[i].abs();
        }

        return magnitude;
    }

    private short[] applyFilter(short[] audioData, double[] impulseResponse) {
        short[] filtered = new short[audioData.length];

        for (int n = 0; n < audioData.length; n++) {
            double sum = 0;

            for (int k = 0; k < impulseResponse.length; k++) {
                if (n - k >= 0) {
                    sum += impulseResponse[k] * audioData[n - k];
                }
            }

            if (sum > Short.MAX_VALUE) {
                sum = Short.MAX_VALUE;
            } else if (sum < Short.MIN_VALUE) {
                sum = Short.MIN_VALUE;
            }

            filtered[n] = (short)sum;
        }

        return filtered;
    }

    private double[] toDoubleArray(short[] samples) {
        double[] doubles = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            doubles[i] = samples[i];
        }
        return doubles;
    }
}
