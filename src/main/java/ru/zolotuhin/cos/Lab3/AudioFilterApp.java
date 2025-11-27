package ru.zolotuhin.cos.Lab3;

import javax.swing.*;

public class AudioFilterApp {
    public static void main(String[] args) {
        String filePath = "C:\\GitHub\\Magister\\src\\main\\java\\ru\\zolotuhin\\cos\\Lab3\\";
        String filePathOut = "C:\\GitHub\\Magister\\src\\main\\java\\ru\\zolotuhin\\cos\\Lab3\\music\\";
        SwingUtilities.invokeLater(() -> {
            try {
                // Создаем главное окно
                JFrame frame = new JFrame("Audio Filter Analysis");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1024, 768);

                // Создаем панель с вкладками
                WavFileHandler.tabbedPane = new JTabbedPane();
                frame.add(WavFileHandler.tabbedPane);

                // 1. Создание фрагментов аудио
                int fragmentSize = (int)Math.pow(2, 20);
                AudioProcessor extractor = new AudioFragmentExtractor(fragmentSize);

                extractor.processAudio(filePath + "bass.wav", filePathOut +"bass_fragment.wav", "Bass");
                extractor.processAudio(filePath + "soprano.wav", filePathOut + "soprano_fragment.wav", "Soprano");

                // 2. Фильтрация с помощью БПФ
                AudioProcessor lowPassFFT = new FFTFilter(600, false);
                lowPassFFT.processAudio(filePathOut + "bass_fragment.wav", filePathOut + "bass_lowpass_fft.wav", "Bass");

                AudioProcessor highPassFFT = new FFTFilter(500, true);
                highPassFFT.processAudio(filePathOut + "soprano_fragment.wav", filePathOut + "soprano_highpass_fft.wav", "Soprano");

                // 3. КИХ фильтры
                AudioProcessor lowPassFIR41 = new FIRFilter(600, false, 41, FIRFilter.WindowType.RECTANGULAR);
                lowPassFIR41.processAudio(filePathOut + "bass_fragment.wav", filePathOut + "bass_lowpass_fir41.wav", "Bass");

                AudioProcessor lowPassFIR501 = new FIRFilter(600, false, 501, FIRFilter.WindowType.RECTANGULAR);
                lowPassFIR501.processAudio(filePathOut + "bass_fragment.wav", filePathOut + "bass_lowpass_fir501.wav", "Bass");

                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}