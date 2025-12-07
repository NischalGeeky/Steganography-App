package com.stego;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileEncryptor {

    public static void main(String[] args) {
        try {
            // 1. Read the File
            String filePath = "message.txt";
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            System.out.println("--- Original Text ---");
            System.out.println(content);
            
            // 2. Setup Keys (Hardcoded for testing Step 1)
            String vigenereKey = "KEY"; 
            String aesKey = "1234567890123456"; // Must be 16 chars

            // 3. Process & Encrypt
            StringBuilder finalOutput = new StringBuilder();
            String[] words = content.split("\\s+"); // Split by whitespace

            for (String word : words) {
                String encryptedWord = "";

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

            System.out.println("\n--- Final Encrypted String ---");
            System.out.println(finalOutput.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
        }
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