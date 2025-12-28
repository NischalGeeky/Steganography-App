package com.stego;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.io.InputStream;

public class LatticeManager {

    // 0. IMPORTANT: Register the Bouncy Castle PQC Provider
    // This tells Java how to do "Lattice Math" (Kyber)
    static {
        Security.addProvider(new BouncyCastlePQCProvider());
    }
    
    private static final String CONFIG_FILE = "/config.properties";
    private static final String DEFAULT_ALGORITHM = "Kyber768";
    private static String cachedAlgorithm = null;
    
    /**
     * Loads the PQC algorithm configuration from config.properties.
     * Reference: ACM CCS 2024 - "Testing Side-channel Security"
     * Uses caching to avoid reloading on every call.
     * 
     * @return The configured algorithm name (e.g., "Kyber768", "Kyber1024")
     */
    private static String loadPQCAlgorithm() {
        if (cachedAlgorithm != null) {
            return cachedAlgorithm;
        }
        
        try {
            InputStream configStream = LatticeManager.class.getResourceAsStream(CONFIG_FILE);
            if (configStream == null) {
                System.out.println("⚠️ Warning: config.properties not found, using default: " + DEFAULT_ALGORITHM);
                cachedAlgorithm = DEFAULT_ALGORITHM;
                return cachedAlgorithm;
            }
            
            Properties props = new Properties();
            props.load(configStream);
            configStream.close();
            
            String algorithm = props.getProperty("PQC_ALGORITHM", DEFAULT_ALGORITHM).trim();
            System.out.println("📋 Loaded PQC Algorithm: " + algorithm);
            cachedAlgorithm = algorithm;
            return cachedAlgorithm;
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Failed to load config.properties: " + e.getMessage());
            cachedAlgorithm = DEFAULT_ALGORITHM;
            return cachedAlgorithm;
        }
    }
    
    /**
     * Gets the AlgorithmParameterSpec based on the configured algorithm.
     */
    private static AlgorithmParameterSpec getParameterSpec(String algorithm) throws Exception {
        switch (algorithm.toUpperCase()) {
            case "KYBER768":
                return KyberParameterSpec.kyber768;
            case "KYBER1024":
                return KyberParameterSpec.kyber1024;
            case "KYBER512":
                return KyberParameterSpec.kyber512;
            case "DILITHIUM":
                // Dilithium requires different algorithm name and parameter spec
                // For now, throw an exception indicating it needs separate implementation
                throw new UnsupportedOperationException(
                    "Dilithium is a digital signature algorithm and requires separate implementation. " +
                    "Use Kyber768 or Kyber1024 for key encapsulation.");
            default:
                throw new IllegalArgumentException("Unsupported PQC algorithm: " + algorithm + 
                    ". Supported: Kyber512, Kyber768, Kyber1024");
        }
    }
    
    /**
     * Gets the algorithm name for KeyPairGenerator based on the configured algorithm.
     */
    private static String getAlgorithmName(String algorithm) {
        String upper = algorithm.toUpperCase();
        if (upper.startsWith("KYBER")) {
            return "KYBER";
        } else if (upper.startsWith("DILITHIUM")) {
            return "DILITHIUM"; // Would need separate implementation
        }
        return "KYBER"; // Default
    }

    // 1. Generate a fresh Pair of Lattice Keys (Configuration-Driven)
    // Reference: ACM CCS 2024 - "Testing Side-channel Security"
    public static KeyPair generateLatticeKeyPair() throws Exception {
        // Load algorithm from configuration
        String algorithm = loadPQCAlgorithm();
        String algoName = getAlgorithmName(algorithm);
        AlgorithmParameterSpec paramSpec = getParameterSpec(algorithm);
        
        // Use configured algorithm from "BCPQC" (Bouncy Castle Post-Quantum) provider
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algoName, "BCPQC");
        keyGen.initialize(paramSpec);
        
        return keyGen.generateKeyPair();
    }

    // 2. Convert a Key object into a String (Logic remains the same!)
    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // 3. Convert a String back into a Public Key
    // Uses configured algorithm name
    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(keyStr);
        String algorithm = loadPQCAlgorithm();
        String algoName = getAlgorithmName(algorithm);
        KeyFactory keyFactory = KeyFactory.getInstance(algoName, "BCPQC");
        return keyFactory.generatePublic(new X509EncodedKeySpec(byteKey));
    }

    // 4. Convert a String back into a Private Key
    // Uses configured algorithm name
    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(keyStr);
        String algorithm = loadPQCAlgorithm();
        String algoName = getAlgorithmName(algorithm);
        KeyFactory keyFactory = KeyFactory.getInstance(algoName, "BCPQC");
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
