package com.stego;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileDecryptor {

    public static String decryptText(String encryptedContent, String aesKey, String vigenereKey) throws Exception {
        StringBuilder decryptedOutput = new StringBuilder();
        String[] encryptedWords = encryptedContent.split("\\s+");

        for (String word : encryptedWords) {
            String decryptedWord;
            
            // Check if the word is Base64 encoded (AES encrypted)
            // Base64 strings typically end with '=' padding and contain only valid Base64 characters
            if (isBase64(word)) {
                try {
                    decryptedWord = aesDecrypt(word, aesKey);
                    System.out.println("Word: " + word + " -> (AES-256) -> " + decryptedWord);
                } catch (Exception e) {
                    // If AES decryption fails, fall back to Vigenère
                    decryptedWord = vigenereDecrypt(word, vigenereKey);
                    System.out.println("Word: " + word + " -> (Vigenère fallback) -> " + decryptedWord);
                }
            } else {
                // Non-Base64 strings are Vigenère encrypted
                decryptedWord = vigenereDecrypt(word, vigenereKey);
                System.out.println("Word: " + word + " -> (Vigenère) -> " + decryptedWord);
            }
            
            decryptedOutput.append(decryptedWord).append(" ");
        }
        return decryptedOutput.toString().trim();
    }
    
    /**
     * Checks if a string is likely Base64 encoded.
     * Base64 strings contain only A-Z, a-z, 0-9, +, /, and = (padding)
     */
    private static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // Check for Base64 characteristics:
        // 1. Contains '=' padding OR is long enough to be Base64
        // 2. Contains only valid Base64 characters
        // 3. Length is multiple of 4 (after padding)
        boolean hasEquals = str.contains("=");
        boolean hasValidChars = str.matches("^[A-Za-z0-9+/=]*$");
        boolean hasProperLength = str.length() % 4 == 0;
        boolean isLongEnough = str.length() >= 16; // AES encrypted words are typically longer
        
        // A string is likely Base64 if it has proper Base64 characteristics
        return hasValidChars && hasProperLength && (hasEquals || isLongEnough);
    }

    public static String caesarDecrypt(String text, int shift) {
        return FileEncryptor.caesarCipher(text, 26 - shift);
    }

    public static String vigenereDecrypt(String text, String key) {
        StringBuilder res = new StringBuilder();
        key = key.toUpperCase();
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                char base = Character.isLowerCase(c) ? 'a' : 'A';
                int shift = (c - base) - (key.charAt(j % key.length()) - 'A');
                if (shift < 0) shift += 26;
                res.append((char) (shift + base));
                j++;
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    public static String aesDecrypt(String encryptedValue, String key) throws Exception {
        // Decode the Base64 key
        byte[] keyBytes = Base64.getDecoder().decode(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)));
    }
}