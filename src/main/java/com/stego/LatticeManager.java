package com.stego;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class LatticeManager {

    // 0. IMPORTANT: Register the Bouncy Castle PQC Provider
    // This tells Java how to do "Lattice Math" (Kyber)
    static {
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    // 1. Generate a fresh Pair of Lattice Keys (Kyber-768)
    public static KeyPair generateLatticeKeyPair() throws Exception {
        // Use "KYBER" algorithm from "BCPQC" (Bouncy Castle Post-Quantum) provider
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("KYBER", "BCPQC");
        
        // Kyber768 is the NIST standard (Security Level 3)
        // It replaces "secp256r1" from your old code
        keyGen.initialize(KyberParameterSpec.kyber768);
        
        return keyGen.generateKeyPair();
    }

    // 2. Convert a Key object into a String (Logic remains the same!)
    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // 3. Convert a String back into a Public Key
    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(keyStr);
        // Note: We request "KYBER" from "BCPQC" here
        KeyFactory keyFactory = KeyFactory.getInstance("KYBER", "BCPQC");
        return keyFactory.generatePublic(new X509EncodedKeySpec(byteKey));
    }

    // 4. Convert a String back into a Private Key
    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(keyStr);
        KeyFactory keyFactory = KeyFactory.getInstance("KYBER", "BCPQC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(byteKey));
    }

    public static KeyPair generateKeyPairAndPrint() throws Exception {
        KeyPair pair = generateLatticeKeyPair();

        System.out.println("--- Lattice Public Key ---");
        System.out.println(keyToString(pair.getPublic()));

        System.out.println("--- Lattice Private Key ---");
        System.out.println(keyToString(pair.getPrivate()));

        return pair;
    }

    // --- Main Method for Testing ---
    public static void main(String[] args) {
        try {
            System.out.println("Generating Kyber (Lattice) Keys...");
            KeyPair pair = generateLatticeKeyPair();

            String pubStr = keyToString(pair.getPublic());
            String privStr = keyToString(pair.getPrivate());

            // Notice: These keys will be MUCH longer text strings than your ECC keys!
            System.out.println("--- Public Key (Share this!) ---");
            System.out.println(pubStr);
            System.out.println("Length: " + pubStr.length() + " chars");

            System.out.println("\n--- Private Key (Keep Secret!) ---");
            System.out.println(privStr);
            System.out.println("Length: " + privStr.length() + " chars");

            // Validation Test
            PublicKey reconstructedPub = stringToPublicKey(pubStr);
            System.out.println("\nAlgorithm used: " + reconstructedPub.getAlgorithm());
            System.out.println("✅ Test: Lattice Key reconstruction successful.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
