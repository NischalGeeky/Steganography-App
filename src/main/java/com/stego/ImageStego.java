package com.stego;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageStego {

    private static final int N = 8; // 8x8 Blocks
    
    // We use 8 distinct mid-frequency spots to hide 1 byte (8 bits) per block
    private static final int[] COEFF_X = {3, 4, 3, 4, 2, 5, 2, 5};
    private static final int[] COEFF_Y = {3, 3, 4, 4, 2, 2, 5, 5};

    public static void encode(String inputImage, String outputImage, String message) throws Exception {
        File f = new File(inputImage);
        if (!f.exists()) throw new RuntimeException("Image not found: " + inputImage);
        
        BufferedImage img = ImageIO.read(f);
        int width = img.getWidth();
        int height = img.getHeight();

        byte[] msgBytes = message.getBytes();
        int len = msgBytes.length;
        
        // Protocol: [4 bytes Length] + [Message]
        byte[] data = new byte[4 + len];
        data[0] = (byte) ((len >> 24) & 0xFF);
        data[1] = (byte) ((len >> 16) & 0xFF);
        data[2] = (byte) ((len >> 8) & 0xFF);
        data[3] = (byte) (len & 0xFF);
        System.arraycopy(msgBytes, 0, data, 4, len);

        // Capacity Check
        int maxBytes = (width / N) * (height / N); // 1 byte per block
        if (data.length > maxBytes) {
            throw new RuntimeException("Message too long! Need larger image or shorter text.");
        }

        int byteIndex = 0;
        int bitIndex = 0;

        // Iterate over blocks
        for (int y = 0; y <= height - N; y += N) {
            for (int x = 0; x <= width - N; x += N) {
                if (byteIndex >= data.length) break;

                // 1. Get Blue Channel & Apply DCT
                double[][] blueBlock = getBlueLayer(img, x, y);
                double[][] dctBlock = applyDCT(blueBlock);

                // 2. Embed 8 bits (1 byte) into this block
                for (int k = 0; k < 8; k++) {
                    if (byteIndex >= data.length) break;

                    int bit = (data[byteIndex] >> (7 - bitIndex)) & 1;
                    embedBit(dctBlock, COEFF_X[k], COEFF_Y[k], bit);

                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        byteIndex++;
                    }
                }

                // 3. Inverse DCT & Save
                double[][] idctBlock = applyIDCT(dctBlock);
                setBlueLayer(img, x, y, idctBlock);
            }
        }
        ImageIO.write(img, "png", new File(outputImage));
        System.out.println("✅ DCT Stego: Saved to " + outputImage);
    }

    // --- DECODE METHOD (Added so Receiver can read the message) ---
    public static String decode(String inputImage) throws Exception {
        BufferedImage img = ImageIO.read(new File(inputImage));
        int width = img.getWidth();
        int height = img.getHeight();

        int len = 0;
        byte[] data = null;
        int byteIndex = 0;
        int bitIndex = 0;
        int currentByte = 0;
        boolean readingLen = true;

        for (int y = 0; y <= height - N; y += N) {
            for (int x = 0; x <= width - N; x += N) {
                
                double[][] blueBlock = getBlueLayer(img, x, y);
                double[][] dctBlock = applyDCT(blueBlock);

                for (int k = 0; k < 8; k++) {
                    int bit = extractBit(dctBlock, COEFF_X[k], COEFF_Y[k]);
                    currentByte = (currentByte << 1) | bit;
                    bitIndex++;

                    if (bitIndex == 8) {
                        if (readingLen) {
                            if (byteIndex == 0) len |= (currentByte << 24);
                            if (byteIndex == 1) len |= (currentByte << 16);
                            if (byteIndex == 2) len |= (currentByte << 8);
                            if (byteIndex == 3) {
                                len |= currentByte;
                                readingLen = false;
                                data = new byte[len];
                                if (len <= 0 || len > 100000) return ""; 
                            }
                        } else {
                            if (byteIndex - 4 < len) {
                                data[byteIndex - 4] = (byte) currentByte;
                            } else {
                                return new String(data);
                            }
                        }
                        
                        byteIndex++;
                        bitIndex = 0;
                        currentByte = 0;
                        if (!readingLen && (byteIndex - 4) >= len) return new String(data);
                    }
                }
            }
        }
        return (data != null) ? new String(data) : "";
    }

    // --- MATH HELPERS (DCT) ---
    private static double[][] getBlueLayer(BufferedImage img, int startX, int startY) {
        double[][] block = new double[N][N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                block[y][x] = (img.getRGB(startX + x, startY + y) & 0xFF);
            }
        }
        return block;
    }

    private static void setBlueLayer(BufferedImage img, int startX, int startY, double[][] block) {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int rgb = img.getRGB(startX + x, startY + y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (int) Math.round(block[y][x]);
                if (b < 0) b = 0; if (b > 255) b = 255;
                img.setRGB(startX + x, startY + y, (r << 16) | (g << 8) | b);
            }
        }
    }

    private static double[][] applyDCT(double[][] matrix) {
        double[][] dct = new double[N][N];
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                double c1 = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                double c2 = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                for (int x = 0; x < N; x++) {
                    for (int y = 0; y < N; y++) {
                        sum += matrix[x][y] * Math.cos(((2 * x + 1) * u * Math.PI) / 16.0) * Math.cos(((2 * y + 1) * v * Math.PI) / 16.0);
                    }
                }
                dct[u][v] = 0.25 * c1 * c2 * sum;
            }
        }
        return dct;
    }

    private static double[][] applyIDCT(double[][] dctMatrix) {
        double[][] matrix = new double[N][N];
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                double sum = 0.0;
                for (int u = 0; u < N; u++) {
                    for (int v = 0; v < N; v++) {
                        double c1 = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                        double c2 = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                        sum += c1 * c2 * dctMatrix[u][v] * Math.cos(((2 * x + 1) * u * Math.PI) / 16.0) * Math.cos(((2 * y + 1) * v * Math.PI) / 16.0);
                    }
                }
                matrix[x][y] = 0.25 * sum;
            }
        }
        return matrix;
    }

    private static void embedBit(double[][] dct, int x, int y, int bit) {
        int val = (int) dct[x][y];
        val = (val / 2) * 2 + bit; 
        if (bit == 1 && val % 2 == 0) val++;
        if (bit == 0 && val % 2 != 0) val--;
        dct[x][y] = val;
    }
    
    private static int extractBit(double[][] dct, int x, int y) {
        int val = (int) Math.round(dct[x][y]);
        return Math.abs(val) % 2;
    }
}