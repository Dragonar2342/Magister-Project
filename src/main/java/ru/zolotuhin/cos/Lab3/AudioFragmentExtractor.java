package ru.zolotuhin.cos.Lab3;

public class AudioFragmentExtractor extends WavFileHandler implements AudioProcessor {
    private final int fragmentSize;

    public AudioFragmentExtractor(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    @Override
    public void processAudio(String inputPath, String outputPath, String soundName) throws Exception {
        short[] audioData = readAudioData(inputPath);

        int size = Math.min(fragmentSize, audioData.length);
        short[] fragment = new short[size];
        System.arraycopy(audioData, 0, fragment, 0, size);

        writeAudioData(outputPath, fragment);
    }
}