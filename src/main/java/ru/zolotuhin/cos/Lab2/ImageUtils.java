package ru.zolotuhin.cos.Lab2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class ImageUtils {
    public static BufferedImage loadImage(String filename) throws IOException {
        return ImageIO.read(new File(filename));
    }

    public static BufferedImage resizeToPowerOfTwo(BufferedImage image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        int newWidth = 1;
        while (newWidth * 2 <= width && newWidth * 2 <= maxSize) {
            newWidth *= 2;
        }

        int newHeight = 1;
        while (newHeight * 2 <= height && newHeight * 2 <= maxSize) {
            newHeight *= 2;
        }

        if (width == newWidth && height == newHeight) {
            return image;
        }

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        resized.getGraphics().drawImage(image, 0, 0, newWidth, newHeight, null);
        return resized;
    }

    public static double[][] imageToArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] array = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                array[y][x] = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
            }
        }

        return array;
    }

    public static BufferedImage arrayToImage(double[][] array) {
        int height = array.length;
        int width = array[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = array[y][x];
                value = Math.max(0.0, Math.min(1.0, value));
                int gray = (int) (value * 255);
                int rgb = (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }
}
