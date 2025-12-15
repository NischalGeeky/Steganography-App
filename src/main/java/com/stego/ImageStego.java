package com.stego;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class ImageStego{

    private static final int BLOCK = 8;
    private static final int U = 3;
    private static final int V = 4;

    public static void encode(String inputImage, String outputImage, String message) throws Exception {
        BufferedImage img = ImageIO.read(new File(inputImage));
        BufferedImage gray = toGrayscale(img);

        byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        int totalBits = msg.length * 8 + 32;

        int blocksX = gray.getWidth() / BLOCK;
        int blocksY = gray.getHeight() / BLOCK;

        if (totalBits > blocksX * blocksY) {
            throw new RuntimeException("Message too large for image");
        }

        int bitIndex = 0;

        for (int i = 31; i >= 0; i--) {
            int bit = (msg.length >>> i) & 1;
            embedNextBit(gray, bitIndex++, bit);
        }

        for (byte b : msg) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >>> i) & 1;
                embedNextBit(gray, bitIndex++, bit);
            }
        }

        ImageIO.write(gray, "png", new File(outputImage));
        System.out.println("DCT stego written to: " + outputImage);
    }

    private static void embedNextBit(BufferedImage img, int bitIndex, int bit) {
        int blocksPerRow = img.getWidth() / BLOCK;

        int blockX = bitIndex % blocksPerRow;
        int blockY = bitIndex / blocksPerRow;

        double[][] block = getBlock(img, blockX * BLOCK, blockY * BLOCK);
        shift(block, -128);

        double[][] dct = dct(block);
        embedBit(dct, bit);

        double[][] idct = idct(dct);
        shift(idct, 128);

        writeBlock(img, blockX * BLOCK, blockY * BLOCK, idct);
    }

    private static void embedBit(double[][] dct, int bit) {
        int coeff = (int) Math.round(dct[U][V]);
        if ((coeff & 1) != bit) {
            coeff += (coeff >= 0) ? 1 : -1;
        }
        dct[U][V] = coeff;
    }

    // ===================== DCT =====================
    private static double[][] dct(double[][] block) {
        double[][] out = new double[BLOCK][BLOCK];

        for (int u = 0; u < BLOCK; u++) {
            for (int v = 0; v < BLOCK; v++) {
                double sum = 0;
                for (int x = 0; x < BLOCK; x++) {
                    for (int y = 0; y < BLOCK; y++) {
                        sum += block[x][y] *
                                Math.cos((2 * x + 1) * u * Math.PI / 16) *
                                Math.cos((2 * y + 1) * v * Math.PI / 16);
                    }
                }
                double cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                double cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                out[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return out;
    }

    private static double[][] idct(double[][] block) {
        double[][] out = new double[BLOCK][BLOCK];

        for (int x = 0; x < BLOCK; x++) {
            for (int y = 0; y < BLOCK; y++) {
                double sum = 0;
                for (int u = 0; u < BLOCK; u++) {
                    for (int v = 0; v < BLOCK; v++) {
                        double cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                        double cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                        sum += cu * cv * block[u][v] *
                                Math.cos((2 * x + 1) * u * Math.PI / 16) *
                                Math.cos((2 * y + 1) * v * Math.PI / 16);
                    }
                }
                out[x][y] = 0.25 * sum;
            }
        }
        return out;
    }

    // ===================== IMAGE HELPERS =====================
    private static double[][] getBlock(BufferedImage img, int sx, int sy) {
        double[][] block = new double[BLOCK][BLOCK];
        for (int x = 0; x < BLOCK; x++) {
            for (int y = 0; y < BLOCK; y++) {
                int rgb = img.getRGB(sx + x, sy + y);
                block[x][y] = rgb & 0xFF;
            }
        }
        return block;
    }

    private static void writeBlock(BufferedImage img, int sx, int sy, double[][] block) {
        for (int x = 0; x < BLOCK; x++) {
            for (int y = 0; y < BLOCK; y++) {
                int v = clamp((int) Math.round(block[x][y]));
                int rgb = (v << 16) | (v << 8) | v;
                img.setRGB(sx + x, sy + y, rgb);
            }
        }
    }

    private static void shift(double[][] block, int v) {
        for (int x = 0; x < BLOCK; x++)
            for (int y = 0; y < BLOCK; y++)
                block[x][y] += v;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static BufferedImage toGrayscale(BufferedImage img) {
        BufferedImage g = new BufferedImage(
                img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        g.getGraphics().drawImage(img, 0, 0, null);
        return g;
    }
}



