package com.stego;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;
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

            // --- 5. Image Steganography (DCT) ---
            System.out.println("\n--- 5. Embedding Data (DCT) ---");
            ImageStego.encode(IMAGE_FILE, OUTPUT_IMAGE_FILE, output.finalOutput, output.vigenereKey);
            
            System.out.println("\n Done! Check output.png");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}