package com.stego;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ImageStego {

    private static final int N = 8; // 8x8 Blocks
    
    // Increased Robustness: We modify coefficients by this amount to survive rounding errors.
    // A value of 20 ensures the bit survives the double -> int -> double conversion.
    private static final int PERSISTENCE = 20; 

    private static final int[] COEFF_X = {3, 4, 3, 4, 2, 5, 2, 5};
    private static final int[] COEFF_Y = {3, 3, 4, 4, 2, 2, 5, 5};
    
    // Texture-Adaptive Masking threshold
    // Blocks with variance < THRESHOLD are considered "smooth" and skipped to avoid visible artifacts
    // Reference: ACM TOMM 2024 - "Enhancing Adversarial Embedding"
    private static final double VARIANCE_THRESHOLD = 200.0;
    
    /**
     * Block coordinates for sparse randomized sampling
     */
    private static class BlockCoord {
        int x, y;
        BlockCoord(int x, int y) { this.x = x; this.y = y; }
    }
    
    /**
     * Generates a deterministic seed from the Vigenère key string.
     * Reference: USENIX Security 2025 - "SparSamp"
     */
    private static long seedFromKey(String key) {
        long seed = 0;
        for (char c : key.toCharArray()) {
            seed = seed * 31 + (long) c;
        }
        return seed;
    }

    public static void encode(String inputImage, String outputImage, String message, String vigenereKey) throws Exception {
        File f = new File(inputImage);
        if (!f.exists()) throw new RuntimeException("Image not found: " + inputImage);
        
        BufferedImage img = ImageIO.read(f);
        int width = img.getWidth();
        int height = img.getHeight();

        byte[] msgBytes = message.getBytes();
        int len = msgBytes.length;
        
        byte[] data = new byte[4 + len];
        data[0] = (byte) ((len >> 24) & 0xFF);
        data[1] = (byte) ((len >> 16) & 0xFF);
        data[2] = (byte) ((len >> 8) & 0xFF);
        data[3] = (byte) (len & 0xFF);
        System.arraycopy(msgBytes, 0, data, 4, len);

        int maxBytes = (width / N) * (height / N);
        if (data.length > maxBytes) {
            throw new RuntimeException("Message too long! Need larger image.");
        }

        // Sparse Randomized Sampling: Generate shuffled block order
        // Reference: USENIX Security 2025 - "SparSamp"
        List<BlockCoord> blockCoords = new ArrayList<>();
        for (int y = 0; y <= height - N; y += N) {
            for (int x = 0; x <= width - N; x += N) {
                double[][] blueBlock = getBlueLayer(img, x, y);
                double[][] dctBlock = applyDCT(blueBlock);
                
                // Texture-Adaptive Masking: Only include textured blocks
                double variance = getBlockVariance(dctBlock);
                if (variance >= VARIANCE_THRESHOLD) {
                    blockCoords.add(new BlockCoord(x, y));
                }
            }
        }
        
        // Shuffle blocks deterministically using Vigenère key as seed
        long seed = seedFromKey(vigenereKey);
        Random rand = new Random(seed);
        for (int i = blockCoords.size() - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            BlockCoord temp = blockCoords.get(i);
            blockCoords.set(i, blockCoords.get(j));
            blockCoords.set(j, temp);
        }

        int byteIndex = 0;
        int bitIndex = 0;

        // Process blocks in randomized order
        for (BlockCoord coord : blockCoords) {
            if (byteIndex >= data.length) break;

            double[][] blueBlock = getBlueLayer(img, coord.x, coord.y);
            double[][] dctBlock = applyDCT(blueBlock);

            for (int k = 0; k < 8; k++) {
                if (byteIndex >= data.length) break;

                int bit = (data[byteIndex] >> (7 - bitIndex)) & 1;
                embedBitRobust(dctBlock, COEFF_X[k], COEFF_Y[k], bit);

                bitIndex++;
                if (bitIndex == 8) {
                    bitIndex = 0;
                    byteIndex++;
                }
            }

            double[][] idctBlock = applyIDCT(dctBlock);
            setBlueLayer(img, coord.x, coord.y, idctBlock);
        }
        ImageIO.write(img, "png", new File(outputImage));
        System.out.println("✅ DCT Stego: Saved to " + outputImage);
    }

    public static String decode(String inputImage, String vigenereKey) throws Exception {
        BufferedImage img = ImageIO.read(new File(inputImage));
        int width = img.getWidth();
        int height = img.getHeight();

        // Sparse Randomized Sampling: Generate same shuffled block order as encoding
        // Reference: USENIX Security 2025 - "SparSamp"
        List<BlockCoord> blockCoords = new ArrayList<>();
        for (int y = 0; y <= height - N; y += N) {
            for (int x = 0; x <= width - N; x += N) {
                double[][] blueBlock = getBlueLayer(img, x, y);
                double[][] dctBlock = applyDCT(blueBlock);
                
                // Texture-Adaptive Masking: Only include textured blocks (same as encoding)
                double variance = getBlockVariance(dctBlock);
                if (variance >= VARIANCE_THRESHOLD) {
                    blockCoords.add(new BlockCoord(x, y));
                }
            }
        }
        
        // Shuffle blocks deterministically using same seed as encoding
        long seed = seedFromKey(vigenereKey);
        Random rand = new Random(seed);
        for (int i = blockCoords.size() - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            BlockCoord temp = blockCoords.get(i);
            blockCoords.set(i, blockCoords.get(j));
            blockCoords.set(j, temp);
        }

        int len = 0;
        byte[] data = null;
        int byteIndex = 0;
        int bitIndex = 0;
        int currentByte = 0;
        boolean readingLen = true;

        // Process blocks in same randomized order as encoding
        for (BlockCoord coord : blockCoords) {
            double[][] blueBlock = getBlueLayer(img, coord.x, coord.y);
            double[][] dctBlock = applyDCT(blueBlock);

            for (int k = 0; k < 8; k++) {
                int bit = extractBitRobust(dctBlock, COEFF_X[k], COEFF_Y[k]);
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
                            
                            // Validate Length to prevent crash
                            if (len <= 0 || len > 200000) { 
                                System.err.println("⚠️ Error: Corruption detected in image header.");
                                return ""; 
                            }
                            data = new byte[len];
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
        return (data != null) ? new String(data) : "";
    }

    /**
     * Calculates the variance of DCT coefficients in a block.
     * High variance indicates textured areas (good for embedding).
     * Low variance indicates smooth areas (should be skipped to avoid visible artifacts).
     * Reference: ACM TOMM 2024 - "Enhancing Adversarial Embedding"
     * 
     * @param dctBlock The DCT-transformed 8x8 block
     * @return Variance of the DCT coefficients
     */
    private static double getBlockVariance(double[][] dctBlock) {
        // Calculate mean of DCT coefficients (excluding DC component at [0][0])
        double sum = 0.0;
        int count = 0;
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                if (u != 0 || v != 0) { // Exclude DC component
                    sum += Math.abs(dctBlock[u][v]);
                    count++;
                }
            }
        }
        if (count == 0) return 0.0;
        double mean = sum / count;
        
        // Calculate variance
        double sumSqDiff = 0.0;
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                if (u != 0 || v != 0) {
                    double diff = Math.abs(dctBlock[u][v]) - mean;
                    sumSqDiff += diff * diff;
                }
            }
        }
        
        return sumSqDiff / count;
    }
    
    // --- ROBUST EMBEDDING LOGIC (Quantization) ---
    private static void embedBitRobust(double[][] dct, int u, int v, int bit) {
        double val = dct[u][v];
        
        // We quantize the value to the nearest multiple of PERSISTENCE
        // If we want to hide '0', we force it to an EVEN multiple
        // If we want to hide '1', we force it to an ODD multiple
        
        double quantized = Math.round(val / PERSISTENCE);
        int parity = (int) Math.abs(quantized) % 2;
        
        if (parity != bit) {
            // Move to the nearest neighbor with correct parity
            if (val > 0) quantized += 1; // e.g. 4 becomes 5
            else quantized -= 1;
        }
        
        dct[u][v] = quantized * PERSISTENCE;
    }
    
    private static int extractBitRobust(double[][] dct, int u, int v) {
        double val = dct[u][v];
        double quantized = Math.round(val / PERSISTENCE);
        return (int) Math.abs(quantized) % 2;
    }

    // --- STANDARD MATH HELPERS ---
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
}