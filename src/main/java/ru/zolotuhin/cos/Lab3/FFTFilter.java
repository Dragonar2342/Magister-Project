package ru.zolotuhin.cos.Lab3;

public class FFTFilter extends WavFileHandler implements AudioProcessor {
    private double cutoffFrequency;
    private boolean isHighPass;
    private String filterName;

    public FFTFilter(double cutoffFrequency, boolean isHighPass) {
        this.cutoffFrequency = cutoffFrequency;
        this.isHighPass = isHighPass;
        this.filterName = isHighPass ? "ВЧ" : "НЧ";
    }

    @Override
    public void processAudio(String inputPath, String outputPath, String soundName) throws Exception {
        short[] audioData = readAudioData(inputPath);
        double[] samples = new double[audioData.length];

        for (int i = 0; i < audioData.length; i++) {
            samples[i] = audioData[i];
        }

        addChartToTabbedPane(
                "Исходный сигнал (" + filterName + " " + cutoffFrequency + " Гц) " + soundName,
                "Исходный аудиосигнал",
                samples,
                "Отсчеты",
                "Амплитуда"
        );

        Complex[] spectrum = FFT.fft(toComplexArray(samples));

        double[] magnitudeSpectrum = new double[spectrum.length / 2];
        for (int i = 0; i < magnitudeSpectrum.length; i++) {
            magnitudeSpectrum[i] = spectrum[i].abs();
        }
        addChartToTabbedPane(
                "Спектр (" + filterName + " " + cutoffFrequency + " Гц) " + soundName,
                "Амплитудный спектр сигнала",
                magnitudeSpectrum,
                "Частота (Гц)",
                "Амплитуда"
        );

        int cutoffBin = (int)(cutoffFrequency * spectrum.length / SAMPLE_RATE);
        for (int i = 0; i < spectrum.length; i++) {
            if (isHighPass) {
                if (i < cutoffBin || i > spectrum.length - cutoffBin) {
                    spectrum[i] = new Complex(0, 0);
                }
            } else {
                if (i > cutoffBin && i < spectrum.length - cutoffBin) {
                    spectrum[i] = new Complex(0, 0);
                }
            }
        }

        Complex[] filtered = FFT.ifft(spectrum);
        short[] filteredAudio = new short[audioData.length];
        for (int i = 0; i < filteredAudio.length; i++) {
            filteredAudio[i] = (short)filtered[i].re();
        }

        writeAudioData(outputPath, filteredAudio);

        addChartToTabbedPane(
                "Отфильтрованный сигнал (" + filterName + " " + cutoffFrequency + " Гц) " + soundName,
                "Отфильтрованный аудиосигнал",
                toDoubleArray(filteredAudio),
                "Отсчеты",
                "Амплитуда"
        );
    }

    private Complex[] toComplexArray(double[] samples) {
        Complex[] complex = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++) {
            complex[i] = new Complex(samples[i], 0);
        }
        return complex;
    }

    private double[] toDoubleArray(short[] samples) {
        double[] doubles = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            doubles[i] = samples[i];
        }
        return doubles;
    }
}
