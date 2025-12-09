package com.stego;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import javax.crypto.KeyGenerator;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class FileEncryptor {

    
    private static final String KEY_FILE = "keys.enc";
    private static final String ENC_FILE = "encrypted_text.txt";
    private static final String MESSAGE_FILE = "message.txt";

    public static void main(String[] args) {
        try {
            // 1. Read file
            String content = new String(Files.readAllBytes(Paths.get(MESSAGE_FILE)));

            System.out.println("--- Original Text ---");
            System.out.println(content);

            String vigenereKey = generateVigenereKey(5);   
            String aesKey = generateAESKey();             

            System.out.println("\nGenerated Vigenere Key: " + vigenereKey);
            System.out.println("Generated AES Key: " + aesKey);

            // 3. Encrypt text
            StringBuilder finalOutput = new StringBuilder();
            String[] words = content.split("\\s+");

            for (String word : words) {
                String encryptedWord;

                if (word.length() <= 2) {
                    encryptedWord = caesarCipher(word, 3);
                    System.out.println("Word: " + word + " -> (Caesar) -> " + encryptedWord);
                }
                else if (word.length() <= 5) {
                    encryptedWord = vigenereCipher(word, vigenereKey);
                    System.out.println("Word: " + word + " -> (Vigenere) -> " + encryptedWord);
                }
                else {
                    encryptedWord = aesEncrypt(word, aesKey);
                    System.out.println("Word: " + word + " -> (AES) -> " + encryptedWord);
                }

                finalOutput.append(encryptedWord).append(" ");
            }

            System.out.println("\n--- Final Encrypted Message ---");
            System.out.println(finalOutput.toString().trim());
            Files.write(Paths.get(ENC_FILE), finalOutput.toString().trim().getBytes());

            String keysOut="";

            // 4. Get ECC public key (receiver)
            KeyPair pub = ECCManager.generateECCKeyPair();
            PublicKey receiverPub = pub.getPublic();
            PrivateKey receiverPriv = pub.getPrivate();

            keysOut+=receiverPub.toString()+"\n"+receiverPriv.toString()+"\n";

            // 5. Hybrid-encrypt BOTH session keys
            String hybridAES = HybridEncryptor.encryptAESKey(aesKey, receiverPub);
            String hybridVigenere = HybridEncryptor.encryptAESKey(vigenereKey, receiverPub);


            System.out.println("\n--- Hybrid Encrypted AES Key ---");
            System.out.println(hybridAES);

            System.out.println("\n--- Hybrid Encrypted Vigenere Key ---");
            System.out.println(hybridVigenere);
            
            keysOut+=hybridAES+"\n"+hybridVigenere+"\n";
            Files.write(Paths.get(KEY_FILE), keysOut.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String generateVigenereKey(int len) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        SecureRandom r = new SecureRandom();

        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }


    public static String generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        byte[] keyBytes = keyGen.generateKey().getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }


    // --- Encryption Logic (Ported from your old code) ---

    public static String caesarCipher(String text, int shift) {
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (Character.isLetter(ch)) {
                char base = Character.isLowerCase(ch) ? 'a' : 'A';
                result.append((char) ((ch - base + shift) % 26 + base));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static String vigenereCipher(String text, String key) {
        StringBuilder res = new StringBuilder();
        key = key.toUpperCase();
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                char base = Character.isLowerCase(c) ? 'a' : 'A';
                res.append((char) ((c - base + (key.charAt(j % key.length()) - 'A')) % 26 + base));
                j++;
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    public static String aesEncrypt(String value, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]); // Using zero IV for simplicity
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
    }
}
