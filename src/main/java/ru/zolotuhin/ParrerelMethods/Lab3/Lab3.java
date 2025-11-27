package ru.zolotuhin.ParrerelMethods.Lab3;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.*;

public class Lab3 {
    private static final int THRESHOLD = 50;
    private static final int WINDOW_SIZE = 600;
    private static final int THREAD_COUNT = 8;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Выберите изображение");

                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

                    BufferedImage originalImage = ImageIO.read(selectedFile);

                    new ImageProcessingWorker(originalImage).execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при загрузке изображения: " + e.getMessage());
            }
        });
    }

    /*
    Класс для обнаружения контуров
     */
    public static EdgeDetectionResult detectEdgesWithSobel(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage grayImage = convertToGray(original);

        BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = edgeImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();



        processEdgesWithThreads(grayImage, edgeImage, THREAD_COUNT);

        return new EdgeDetectionResult(edgeImage, THREAD_COUNT);
    }

    /*
    Конвертация в чёрно-белое изображение
     */
    private static BufferedImage convertToGray(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        int stripHeight = height / THREAD_COUNT;

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int startY = i * stripHeight;
            final int endY = (i == THREAD_COUNT - 1) ? height : (i + 1) * stripHeight;

            executor.execute(() -> {
                convertStripToGray(original, grayImage, 0, width, startY, endY);
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return grayImage;
    }

    /*
    Преобразование полосы изображения в чёрно-белый
     */
    private static void convertStripToGray(BufferedImage source, BufferedImage dest,
                                           int startX, int endX, int startY, int endY) {
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Формула для преобразования в градации серого
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                int grayRGB = (gray << 16) | (gray << 8) | gray;

                dest.setRGB(x, y, grayRGB);
            }
        }
    }

    /*
    Обработка контуров с использованием потоков
     */
    private static void processEdgesWithThreads(BufferedImage grayImage, BufferedImage edgeImage, int numThreads) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();


        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        int stripHeight = height / numThreads;

        for (int i = 0; i < numThreads; i++) {
            final int startY = i * stripHeight;
            final int endY = (i == numThreads - 1) ? height : (i + 1) * stripHeight;

            executor.execute(() -> {
                processEdgeStrip(grayImage, edgeImage, 0, width, startY, endY);
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    Обработка полосы для обнаружения контуров
     */
    private static void processEdgeStrip(BufferedImage grayImage, BufferedImage edgeImage,
                                         int startX, int endX, int startY, int endY) {
        int[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        int actualStartY = Math.max(startY, 1);
        int actualEndY = Math.min(endY, grayImage.getHeight() - 1);
        int actualStartX = Math.max(startX, 1);
        int actualEndX = Math.min(endX, grayImage.getWidth() - 1);

        for (int y = actualStartY; y < actualEndY; y++) {
            for (int x = actualStartX; x < actualEndX; x++) {
                int gradientX = 0;
                int gradientY = 0;

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int pixel = (grayImage.getRGB(x + i, y + j) >> 16) & 0xFF;
                        gradientX += pixel * sobelX[i + 1][j + 1];
                        gradientY += pixel * sobelY[i + 1][j + 1];
                    }
                }

                int gradientMagnitude = (int)Math.sqrt(gradientX * gradientX + gradientY * gradientY);

                if (gradientMagnitude > THRESHOLD) {
                    edgeImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
    }

    /*
    Класс реализующий графический интерфейс
     */
    static class ImageProcessingWorker extends SwingWorker<ProcessingResult, Void> {
        private final BufferedImage originalImage;

        public ImageProcessingWorker(BufferedImage originalImage) {
            this.originalImage = originalImage;
        }

        @Override
        protected ProcessingResult doInBackground() {
            long startTime = System.currentTimeMillis();
            EdgeDetectionResult result = detectEdgesWithSobel(originalImage);
            long endTime = System.currentTimeMillis();

            return new ProcessingResult(result.edgeImage, endTime - startTime, result.threadsUsed);
        }

        @Override
        protected void done() {
            try {
                ProcessingResult result = get();
                BufferedImage edgeImage = result.edgeImage;
                long processingTime = result.processingTime;
                int threadsUsed = result.threadsUsed;

                JFrame frame = new JFrame("Детектор контуров");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(WINDOW_SIZE, WINDOW_SIZE);
                frame.setLocationRelativeTo(null);

                JTabbedPane tabbedPane = new JTabbedPane();

                JPanel originalPanel = createImagePanel(originalImage, "Оригинальное изображение");
                tabbedPane.addTab("Оригинал", originalPanel);

                JPanel edgePanel = createImagePanel(edgeImage, "Контуры на белом фоне");
                tabbedPane.addTab("Контуры", edgePanel);

                JPanel comparisonPanel = createComparisonPanel(originalImage, edgeImage, processingTime, threadsUsed);
                tabbedPane.addTab("Сравнение", comparisonPanel);

                JPanel threadsPanel = createThreadsInfoPanel(threadsUsed, processingTime);
                tabbedPane.addTab("Производительность", threadsPanel);

                frame.getContentPane().add(tabbedPane);
                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при обработке изображения: " + e.getMessage());
            }
        }

        private JPanel createImagePanel(BufferedImage image, String title) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder(title));

            Image scaledImage = getScaledImage(image, WINDOW_SIZE - 50, WINDOW_SIZE - 100);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            panel.add(imageLabel, BorderLayout.CENTER);

            JLabel infoLabel = new JLabel(
                    String.format("Размер: %dx%d пикселей", image.getWidth(), image.getHeight())
            );
            infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(infoLabel, BorderLayout.SOUTH);

            return panel;
        }

        private JPanel createComparisonPanel(BufferedImage original, BufferedImage edges,
                                             long processingTime, int threadsUsed) {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel imagePanel = new JPanel(new GridLayout(1, 2, 10, 10));

            Image scaledOriginal = getScaledImage(original, (WINDOW_SIZE - 100) / 2, WINDOW_SIZE - 150);
            Image scaledEdges = getScaledImage(edges, (WINDOW_SIZE - 100) / 2, WINDOW_SIZE - 150);

            JLabel originalLabel = new JLabel(new ImageIcon(scaledOriginal));
            originalLabel.setBorder(BorderFactory.createTitledBorder("Оригинал"));

            JLabel edgesLabel = new JLabel(new ImageIcon(scaledEdges));
            edgesLabel.setBorder(BorderFactory.createTitledBorder("Контуры"));

            imagePanel.add(originalLabel);
            imagePanel.add(edgesLabel);

            mainPanel.add(imagePanel, BorderLayout.CENTER);

            JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));
            infoPanel.setBorder(BorderFactory.createTitledBorder("Информация о обработке"));

            JLabel timeLabel = new JLabel(
                    String.format("Время обработки: %d мс", processingTime)
            );
            timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            timeLabel.setFont(new Font("Arial", Font.BOLD, 14));

            JLabel sizeLabel = new JLabel(
                    String.format("Размер изображения: %dx%d пикселей",
                            original.getWidth(), original.getHeight())
            );
            sizeLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel threadsLabel = new JLabel(
                    String.format("Количество потоков: %d", threadsUsed)
            );
            threadsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            threadsLabel.setForeground(Color.BLUE);
            threadsLabel.setFont(new Font("Arial", Font.BOLD, 14));

            JLabel modeLabel = new JLabel(
                    "Режим: FixedThreadPool с разделением на горизонтальные полосы"
            );
            modeLabel.setHorizontalAlignment(SwingConstants.CENTER);

            infoPanel.add(timeLabel);
            infoPanel.add(sizeLabel);
            infoPanel.add(threadsLabel);
            infoPanel.add(modeLabel);

            mainPanel.add(infoPanel, BorderLayout.SOUTH);

            return mainPanel;
        }

        private JPanel createThreadsInfoPanel(int threadsUsed, long processingTime) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JPanel infoPanel = new JPanel(new GridLayout(6, 1, 10, 10));
            infoPanel.setBorder(BorderFactory.createTitledBorder("Информация о многопоточности"));

            JLabel titleLabel = new JLabel("Статистика использования потоков", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            titleLabel.setForeground(Color.DARK_GRAY);

            JLabel threadsLabel = new JLabel(
                    String.format("Количество потоков: %d", threadsUsed),
                    SwingConstants.CENTER
            );
            threadsLabel.setFont(new Font("Arial", Font.BOLD, 14));
            threadsLabel.setForeground(Color.BLUE);

            JLabel timeLabel = new JLabel(
                    String.format("Общее время обработки: %d мс", processingTime),
                    SwingConstants.CENTER
            );
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

            JLabel availableLabel = new JLabel(
                    String.format("Доступно ядер: %d", Runtime.getRuntime().availableProcessors()),
                    SwingConstants.CENTER
            );
            availableLabel.setFont(new Font("Arial", Font.PLAIN, 14));

            JLabel efficiencyLabel = new JLabel(
                    String.format("Эффективность: %.1f потоков на ядро",
                            (double) threadsUsed / Runtime.getRuntime().availableProcessors()),
                    SwingConstants.CENTER
            );
            efficiencyLabel.setFont(new Font("Arial", Font.PLAIN, 14));

            infoPanel.add(titleLabel);
            infoPanel.add(threadsLabel);
            infoPanel.add(timeLabel);
            infoPanel.add(availableLabel);
            infoPanel.add(efficiencyLabel);

            panel.add(infoPanel, BorderLayout.CENTER);

            return panel;
        }

        private Image getScaledImage(BufferedImage image, int maxWidth, int maxHeight) {
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            double widthRatio = (double) maxWidth / originalWidth;
            double heightRatio = (double) maxHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            return image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        }
    }

    static class ProcessingResult {
        final BufferedImage edgeImage;
        final long processingTime;
        final int threadsUsed;

        ProcessingResult(BufferedImage edgeImage, long processingTime, int threadsUsed) {
            this.edgeImage = edgeImage;
            this.processingTime = processingTime;
            this.threadsUsed = threadsUsed;
        }
    }

    static class EdgeDetectionResult {
        final BufferedImage edgeImage;
        final int threadsUsed;

        EdgeDetectionResult(BufferedImage edgeImage, int threadsUsed) {
            this.edgeImage = edgeImage;
            this.threadsUsed = threadsUsed;
        }
    }
}
