package com.stego;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.Cipher;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public class HybridEncryptor {

    public static String encryptAESKey(String aesKey, PublicKey receiverPubKey) throws Exception {

        KeyPair ephemeral = ECCManager.generateECCKeyPair();

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(receiverPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        byte[] aesBytes = new byte[16];
        System.arraycopy(sharedSecret, 1, aesBytes, 0, 16);
        SecretKey sharedAESKey = new SecretKeySpec(aesBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);  // Zero IV (demo)
        cipher.init(Cipher.ENCRYPT_MODE, sharedAESKey, iv);

        byte[] encryptedAESKey = cipher.doFinal(aesKey.getBytes());
        String encryptedKeyB64 = Base64.getEncoder().encodeToString(encryptedAESKey);

        String ephemeralPubStr = ECCManager.keyToString(ephemeral.getPublic());

        return ephemeralPubStr + ":" + encryptedKeyB64;
    }
}

