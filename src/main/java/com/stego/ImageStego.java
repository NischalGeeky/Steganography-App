package com.stego;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageStego {
        public static void encode(String inputImage, String outputImage, String message) throws Exception {
            BufferedImage img = ImageIO.read(new File(inputImage));

            byte[] msgBytes = message.getBytes();
            int msgLen = msgBytes.length;

            int width = img.getWidth();
            int height = img.getHeight();
            int capacity = width * height * 3;  // 3 bits per pixel (R,G,B)

            if (msgLen * 8 + 32 > capacity) {
                throw new RuntimeException("Message too big to hide in this image!");
            }

            // Convert message length to 32 bits
            int bitIndex = 0;

            // Encode length first (32 bits)
            for (int i = 31; i >= 0; i--) {
                int bit = (msgLen >>> i) & 1;
                writeBit(img, bitIndex++, bit);
            }

            // Encode message bits
            for (byte b : msgBytes) {
                for (int i = 7; i >= 0; i--) {
                    int bit = (b >>> i) & 1;
                    writeBit(img, bitIndex++, bit);
                }
            }

            ImageIO.write(img, "png", new File(outputImage));
            System.out.println("Message hidden inside: " + outputImage);
        }

     private static void writeBit(BufferedImage img, int bitIndex, int bit) {
        int width = img.getWidth();

        // Each pixel carries 3 bits (R,G,B) => compute pixel index
        int pixelIndex = bitIndex / 3;
        int channel = bitIndex % 3; // 0->R, 1->G, 2->B

        int x = pixelIndex % width;
        int y = pixelIndex / width;

        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (channel == 0) {
            r = (r & 0xFE) | (bit & 1);
        } else if (channel == 1) {
            g = (g & 0xFE) | (bit & 1);
        } else {
            b = (b & 0xFE) | (bit & 1);
        }

        int newRGB = (r << 16) | (g << 8) | b;
        img.setRGB(x, y, newRGB);
    }


}
