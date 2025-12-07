package com.stego;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ECCManager {

    // 1. Generate a fresh Pair of Keys (Public & Private)
    public static KeyPair generateECCKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        // secp256r1 is the standard curve used in modern security (NIST P-256)
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        return keyGen.generateKeyPair();
    }

    // 2. Convert a Key object into a String (For the GUI)
    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // 3. Convert a String back into a Public Key
    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(keyStr);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(new X509EncodedKeySpec(byteKey));
    }

    // 4. Convert a String back into a Private Key
    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(keyStr);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(byteKey));
    }

    // --- Main Method for Testing ---
    public static void main(String[] args) {
        try {
            System.out.println("Generating ECC Keys...");
            KeyPair pair = generateECCKeyPair();

            String pubStr = keyToString(pair.getPublic());
            String privStr = keyToString(pair.getPrivate());

            System.out.println("--- Public Key (Share this!) ---");
            System.out.println(pubStr);

            System.out.println("\n--- Private Key (Keep Secret!) ---");
            System.out.println(privStr);

            // Validation Test
            PublicKey reconstructedPub = stringToPublicKey(pubStr);
            System.out.println("Algorithm used: " + reconstructedPub.getAlgorithm()); // <--- Add this
            System.out.println("\n✅ Test: Key reconstruction successful.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}