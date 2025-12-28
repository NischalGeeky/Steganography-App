package com.stego;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import com.stego.FileEncryptor.Output;

public class Main {

    private static final String MESSAGE_FILE = "message.txt";
    private static final String IMAGE_FILE = "image.png";
    private static final String ENC_FILE = "encrypted_text.txt";
    private static final String KEY_FILE = "keys.enc";
    private static final String OUTPUT_IMAGE_FILE = "output.png";

    public static void main(String[] args) {
        try {
            System.out.println("--- 1. Reading Message ---");
            if (!Files.exists(Paths.get(MESSAGE_FILE))) {
                System.out.println("❌ Create message.txt first!");
                return;
            }
            String content = new String(Files.readAllBytes(Paths.get(MESSAGE_FILE)));

            // --- 2. Hybrid Encryption (Text) ---
            System.out.println("\n--- 2. Encrypting Text (Hybrid) ---");
            Output output = FileEncryptor.encryptText(content);
            Files.write(Paths.get(ENC_FILE), output.finalOutput.getBytes());

            // --- 3. Lattice Keys (Kyber) ---
            System.out.println("\n--- 3. Generating Lattice Keys (Kyber) ---");
            // Replaced ECCManager with LatticeManager
            KeyPair latticePair = LatticeManager.generateLatticeKeyPair();
            PublicKey receiverPub = latticePair.getPublic();
            PrivateKey receiverPriv = latticePair.getPrivate();

            String keysOut = LatticeManager.keyToString(receiverPub) + "\n" + LatticeManager.keyToString(receiverPriv);

            // --- 4. Encrypt Session Keys (Lattice) ---
            System.out.println("\n--- 4. Wrapping Keys with Kyber ---");
            // Now calling the updated HybridEncryptor
            String secureAES = HybridEncryptor.encryptAESKey(output.aesKey, receiverPub);
            String secureVigenere = HybridEncryptor.encryptAESKey(output.vigenereKey, receiverPub);

            keysOut += "\n" + secureAES + "\n" + secureVigenere;
            Files.write(Paths.get(KEY_FILE), keysOut.getBytes());

            // --- 5. Split-Payload Orchestration (DCT) ---
            // Reference: ACM CCS 2025 - "Split Unlearning"
            System.out.println("\n--- 5. Split-Payload Orchestration (DCT) ---");
            
            String encryptedText = output.finalOutput;
            int totalLen = encryptedText.length();
            
            // Split payload into 3 logical chunks: Header, Body, Metadata
            int chunkSize = (totalLen + 2) / 3; // Divide into 3 parts with rounding
            String chunk1 = encryptedText.substring(0, Math.min(chunkSize, totalLen));
            String chunk2 = totalLen > chunkSize ? encryptedText.substring(chunkSize, Math.min(2 * chunkSize, totalLen)) : "";
            String chunk3 = totalLen > 2 * chunkSize ? encryptedText.substring(2 * chunkSize) : "";
            
            System.out.println("Chunk 1 (Header) length: " + chunk1.length());
            System.out.println("Chunk 2 (Body) length: " + chunk2.length());
            System.out.println("Chunk 3 (Metadata) length: " + chunk3.length());
            
            // Load the base image
            BufferedImage img = ImageIO.read(new File(IMAGE_FILE));
            
            // Embed Chunk 1 in Red Channel (low frequency)
            System.out.println("Embedding Chunk 1 in RED channel...");
            ImageStego.encode(IMAGE_FILE, OUTPUT_IMAGE_FILE + ".tmp1", chunk1, output.vigenereKey, ImageStego.CHANNEL_RED);
            img = ImageIO.read(new File(OUTPUT_IMAGE_FILE + ".tmp1"));
            
            // Embed Chunk 2 in Green Channel (mid frequency)
            if (!chunk2.isEmpty()) {
                System.out.println("Embedding Chunk 2 in GREEN channel...");
                // Create a temporary file with the current state
                ImageIO.write(img, "png", new File(OUTPUT_IMAGE_FILE + ".tmp2"));
                ImageStego.encode(OUTPUT_IMAGE_FILE + ".tmp2", OUTPUT_IMAGE_FILE + ".tmp1", chunk2, output.vigenereKey, ImageStego.CHANNEL_GREEN);
                img = ImageIO.read(new File(OUTPUT_IMAGE_FILE + ".tmp1"));
            }
            
            // Embed Chunk 3 in Blue Channel (high frequency)
            if (!chunk3.isEmpty()) {
                System.out.println("Embedding Chunk 3 in BLUE channel...");
                // Create a temporary file with the current state
                ImageIO.write(img, "png", new File(OUTPUT_IMAGE_FILE + ".tmp2"));
                ImageStego.encode(OUTPUT_IMAGE_FILE + ".tmp2", OUTPUT_IMAGE_FILE, chunk3, output.vigenereKey, ImageStego.CHANNEL_BLUE);
            } else {
                // If chunk3 is empty, just save the current image
                ImageIO.write(img, "png", new File(OUTPUT_IMAGE_FILE));
            }
            
            // Clean up temporary files
            new File(OUTPUT_IMAGE_FILE + ".tmp1").delete();
            new File(OUTPUT_IMAGE_FILE + ".tmp2").delete();
            
            System.out.println("\n✅ Done! Check output.png");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}