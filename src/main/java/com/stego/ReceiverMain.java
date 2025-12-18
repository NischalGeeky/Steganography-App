package com.stego;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;

public class ReceiverMain {
    private static final String STEGO_IMAGE = "output.png";
    private static final String KEY_FILE = "keys.enc";

    public static void main(String[] args) {
        try {
            System.out.println("--- RECEIVER STARTED ---");

            System.out.println("\n--- 1. Extracting Data (DCT) ---");
            String extractedCiphertext = ImageStego.decode(STEGO_IMAGE);
            System.out.println("Ciphertext Found: " + extractedCiphertext.substring(0, Math.min(50, extractedCiphertext.length())) + "...");

            System.out.println("\n--- 2. Decrypting Keys (Kyber) ---");
            List<String> keyLines = Files.readAllLines(Paths.get(KEY_FILE));
            PrivateKey latticePriv = LatticeManager.stringToPrivateKey(keyLines.get(1));

            String aesKey = HybridDecryptor.decryptSessionKey(keyLines.get(2), latticePriv);
            String vigenereKey = HybridDecryptor.decryptSessionKey(keyLines.get(3), latticePriv);
            
            System.out.println("Keys Recovered.");

            System.out.println("\n--- 3. Decrypting Message ---");
            String original = FileDecryptor.decryptText(extractedCiphertext, aesKey, vigenereKey);

            System.out.println("\n✅ SUCCESS! MESSAGE:");
            System.out.println("---------------------");
            System.out.println(original);
            System.out.println("---------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}