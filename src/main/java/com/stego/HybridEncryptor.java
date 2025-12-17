package com.stego;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.PublicKey;
import java.util.Base64;
import java.security.SecureRandom;

public class HybridEncryptor {

    // Updated to use Kyber (Lattice) Public Key instead of ECC
    public static String encryptAESKey(String targetKeyToHide, PublicKey receiverPubKey) throws Exception {
        
        // 1. Generate a temporary random AES "Session Key"
        byte[] sessionKeyBytes = new byte[16];
        new SecureRandom().nextBytes(sessionKeyBytes);
        SecretKey sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");

        // 2. Encrypt (Wrap) this Session Key using Kyber (Lattice Logic)
        // We use the "KYBER" algorithm from the BCPQC provider
        Cipher kyberCipher = Cipher.getInstance("KYBER", "BCPQC");
        kyberCipher.init(Cipher.WRAP_MODE, receiverPubKey);
        byte[] wrappedSessionKey = kyberCipher.wrap(sessionKey);

        // 3. Encrypt the Target Data (Your Vigenere/AES key) using the Session Key
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]); 
        aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey, iv);
        
        byte[] encryptedTargetData = aesCipher.doFinal(targetKeyToHide.getBytes());

        // 4. Return Format: WRAPPED_KEY : ENCRYPTED_DATA
        String wrappedKeyB64 = Base64.getEncoder().encodeToString(wrappedSessionKey);
        String encryptedDataB64 = Base64.getEncoder().encodeToString(encryptedTargetData);

        return wrappedKeyB64 + ":" + encryptedDataB64;
    }
}