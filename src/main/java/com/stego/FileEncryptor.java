package com.stego;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import java.security.*;

public class FileEncryptor {

    // Entropy threshold for cipher selection (bits per character)
    // Words with entropy > THRESHOLD use AES-256 (High Security)
    // Words with entropy <= THRESHOLD use Vigenère (Standard Security)
    private static final double ENTROPY_THRESHOLD = 2.5;

    /**
     * Calculates Shannon entropy of a text string.
     * Higher entropy indicates more randomness/importance.
     * Reference: ACM TOMM 2025 - "Content-Aware Selective Encryption"
     * 
     * @param text The text to analyze
     * @return Shannon entropy in bits per character
     */
    public static double calculateEntropy(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        // Count character frequencies
        int[] charCounts = new int[256]; // ASCII character set
        int totalChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || !Character.isWhitespace(c)) {
                charCounts[c & 0xFF]++;
                totalChars++;
            }
        }
        
        if (totalChars == 0) {
            return 0.0;
        }
        
        // Calculate Shannon entropy: H(X) = -Σ p(x) * log2(p(x))
        double entropy = 0.0;
        for (int count : charCounts) {
            if (count > 0) {
                double frequency = (double) count / totalChars;
                entropy -= frequency * (Math.log(frequency) / Math.log(2));
            }
        }
        
        return entropy;
    }
    
    public static class Output{
        String finalOutput;
        String aesKey;
        String vigenereKey;

        Output(String finalOutput,String aesKey, String vigenereKey){
            this.finalOutput=finalOutput;
            this.aesKey=aesKey;
            this.vigenereKey=vigenereKey;
        }
    }

    public static Output encryptText(String content) throws Exception{
            StringBuilder finalOutput = new StringBuilder();
            String[] words = content.split("\\s+");

            String vigenereKey = generateVigenereKey(5);   
            String aesKey = generateAESKey();             

            System.out.println("\nGenerated Vigenere Key: " + vigenereKey);
            System.out.println("Generated AES Key: " + aesKey);

            for (String word : words) {
                String encryptedWord;
                double entropy = calculateEntropy(word);

                // Entropy-based cipher selection (replaces naive length-based check)
                // High entropy -> AES-256 (High Security), Low entropy -> Vigenère (Standard Security)
                if (entropy > ENTROPY_THRESHOLD) {
                    encryptedWord = aesEncrypt(word, aesKey);
                    System.out.println("Word: " + word + " [Entropy: " + String.format("%.2f", entropy) + "] -> (AES-256) -> " + encryptedWord);
                } else {
                    encryptedWord = vigenereCipher(word, vigenereKey);
                    System.out.println("Word: " + word + " [Entropy: " + String.format("%.2f", entropy) + "] -> (Vigenère) -> " + encryptedWord);
                }

                finalOutput.append(encryptedWord).append(" ");
            }

            System.out.println("\n--- Final Encrypted Message ---");
            System.out.println(finalOutput.toString().trim());

            return new Output(finalOutput.toString().trim(), aesKey, vigenereKey);


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
        // Decode the Base64 key
        byte[] keyBytes = Base64.getDecoder().decode(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]); // Using zero IV for simplicity
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
    }
}
