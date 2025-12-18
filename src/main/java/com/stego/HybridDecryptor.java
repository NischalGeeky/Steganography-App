package com.stego;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.PrivateKey;
import java.util.Base64;

public class HybridDecryptor {
    public static String decryptSessionKey(String hybridData, PrivateKey latticePrivKey) throws Exception {
        String[] parts = hybridData.split(":");
        byte[] wrappedSessionKey = Base64.getDecoder().decode(parts[0]);
        byte[] encryptedTargetData = Base64.getDecoder().decode(parts[1]);

        Cipher kyberCipher = Cipher.getInstance("KYBER", "BCPQC");
        kyberCipher.init(Cipher.UNWRAP_MODE, latticePrivKey);
        SecretKey sessionKey = (SecretKey) kyberCipher.unwrap(wrappedSessionKey, "AES", Cipher.SECRET_KEY);

        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        aesCipher.init(Cipher.DECRYPT_MODE, sessionKey, iv);
        
        return new String(aesCipher.doFinal(encryptedTargetData));
    }
}