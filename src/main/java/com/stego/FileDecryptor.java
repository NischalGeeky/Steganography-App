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
            if (word.length() <= 2) {
                decryptedWord = caesarDecrypt(word, 3);
            } else if (word.length() <= 5) {
                decryptedWord = vigenereDecrypt(word, vigenereKey);
            } else {
                decryptedWord = aesDecrypt(word, aesKey);
            }
            decryptedOutput.append(decryptedWord).append(" ");
        }
        return decryptedOutput.toString().trim();
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
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)));
    }
}