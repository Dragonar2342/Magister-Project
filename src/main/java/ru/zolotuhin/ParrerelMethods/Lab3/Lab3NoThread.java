package ru.zolotuhin.ParrerelMethods.Lab3;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Lab3NoThread {
    private static final int THRESHOLD = 50;
    private static final int WINDOW_SIZE = 600;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Выберите изображение");

                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

                    BufferedImage originalImage = ImageIO.read(selectedFile);

                    long startTime = System.currentTimeMillis();
                    BufferedImage edgeImage = detectEdges(originalImage);
                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;

                    showResults(originalImage, edgeImage, processingTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при загрузке изображения: " + e.getMessage());
            }
        });
    }

    public static BufferedImage detectEdges(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage grayImage = convertToGray(original);

        BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                edgeImage.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        int[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gradientX = 0;
                int gradientY = 0;

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int pixel = (grayImage.getRGB(x + i, y + j) >> 16) & 0xFF;
                        gradientX += pixel * sobelX[i + 1][j + 1];
                        gradientY += pixel * sobelY[i + 1][j + 1];
                    }
                }

                int gradientMagnitude = (int) Math.sqrt(gradientX * gradientX + gradientY * gradientY);

                if (gradientMagnitude > THRESHOLD) {
                    edgeImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        return edgeImage;
    }

    private static BufferedImage convertToGray(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                int grayRGB = (gray << 16) | (gray << 8) | gray;

                grayImage.setRGB(x, y, grayRGB);
            }
        }

        return grayImage;
    }

    private static void showResults(BufferedImage originalImage, BufferedImage edgeImage, long processingTime) {

        JFrame frame = new JFrame("Детектор контуров (без потоков)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_SIZE, WINDOW_SIZE);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel originalPanel = createImagePanel(originalImage, "Оригинальное изображение");
        tabbedPane.addTab("Оригинал", originalPanel);

        JPanel edgePanel = createImagePanel(edgeImage, "Контуры на белом фоне");
        tabbedPane.addTab("Контуры", edgePanel);

        JPanel comparisonPanel = createComparisonPanel(originalImage, edgeImage, processingTime, false);
        tabbedPane.addTab("Сравнение", comparisonPanel);

        frame.getContentPane().add(tabbedPane);
        frame.setVisible(true);
    }

    private static JPanel createImagePanel(BufferedImage image, String title) {
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

    private static JPanel createComparisonPanel(BufferedImage original, BufferedImage edges,
                                                long processingTime, boolean withThreads) {
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

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
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

        infoPanel.add(timeLabel);
        infoPanel.add(sizeLabel);

        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private static Image getScaledImage(BufferedImage image, int maxWidth, int maxHeight) {
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
