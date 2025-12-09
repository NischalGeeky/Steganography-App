package com.stego;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import com.stego.FileEncryptor.Output;

import java.security.*;


public class Main {

    
    private static final String KEY_FILE = "keys.enc";
    private static final String ENC_FILE = "encrypted_text.txt";
    private static final String MESSAGE_FILE = "message.txt";
    private static final String IMAGE_FILE = "image.png";
    private static final String OUTPUT_IMAGE_FULE = "output.png";

    public static void main(String[] args) {
        try {
            // 1. Read file
            String content = new String(Files.readAllBytes(Paths.get(MESSAGE_FILE)));

            System.out.println("--- Original Text ---");
            System.out.println(content);

            // 3. Encrypt text
            Output output =  FileEncryptor.encryptText(content, ENC_FILE);

            // 4. Get ECC keys (receiver)
            KeyPair pub = ECCManager.generateECCKeyPair();
            PublicKey receiverPub = pub.getPublic();
            PrivateKey receiverPriv = pub.getPrivate();

            String keysOut=receiverPub.toString().trim()+'\n'+receiverPriv.toString().trim();

            // 5. Hybrid-encrypt BOTH session keys
            String hybridAES = HybridEncryptor.encryptAESKey(output.aesKey, receiverPub);
            String hybridVigenere = HybridEncryptor.encryptAESKey(output.vigenereKey, receiverPub);

            keysOut+='\n'+hybridAES+'\n'+hybridVigenere;

            System.out.println("\n--- Hybrid Encrypted AES Key ---");
            System.out.println(hybridAES);

            System.out.println("\n--- Hybrid Encrypted Vigenere Key ---");
            System.out.println(hybridVigenere);

            Files.write(Paths.get(KEY_FILE), keysOut.getBytes());
            System.out.println("Saved Keys in: "+KEY_FILE);

            ImageStego.encode(IMAGE_FILE, OUTPUT_IMAGE_FULE, output.finalOutput); 

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
