package com.stego.gui.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data model for managing cryptographic keys used in the steganographic chat system.
 * Contains Kyber public/private keys and session keys for secure communication.
 */
public class KeysData {
    private String publicKey;
    private String privateKey;
    private String aesKey;
    private String vigenereKey;
    private String algorithm;
    private LocalDateTime createdAt;
    
    /**
     * Creates a new KeysData instance.
     * 
     * @param publicKey Kyber public key (Base64 encoded)
     * @param privateKey Kyber private key (Base64 encoded)
     * @param aesKey AES session key (Base64 encoded)
     * @param vigenereKey Vigenère cipher key
     * @param algorithm PQC algorithm used (e.g., "Kyber768")
     */
    public KeysData(String publicKey, String privateKey, String aesKey, String vigenereKey, String algorithm) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.aesKey = aesKey;
        this.vigenereKey = vigenereKey;
        this.algorithm = algorithm;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Creates KeysData from existing data with timestamp.
     */
    public KeysData(String publicKey, String privateKey, String aesKey, String vigenereKey, 
                   String algorithm, LocalDateTime createdAt) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.aesKey = aesKey;
        this.vigenereKey = vigenereKey;
        this.algorithm = algorithm;
        this.createdAt = createdAt;
    }

    // Getters
    public String getPublicKey() { return publicKey; }
    public String getPrivateKey() { return privateKey; }
    public String getAesKey() { return aesKey; }
    public String getVigenereKey() { return vigenereKey; }
    public String getAlgorithm() { return algorithm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Setters
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public void setAesKey(String aesKey) { this.aesKey = aesKey; }
    public void setVigenereKey(String vigenereKey) { this.vigenereKey = vigenereKey; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * Validates that all required keys are present and non-empty.
     * 
     * @return true if all keys are valid, false otherwise
     */
    public boolean isValid() {
        return publicKey != null && !publicKey.trim().isEmpty() &&
               privateKey != null && !privateKey.trim().isEmpty() &&
               aesKey != null && !aesKey.trim().isEmpty() &&
               vigenereKey != null && !vigenereKey.trim().isEmpty() &&
               algorithm != null && !algorithm.trim().isEmpty();
    }
    
    /**
     * Gets a formatted string representation of the creation timestamp.
     * 
     * @return Formatted timestamp string
     */
    public String getFormattedCreationTime() {
        if (createdAt == null) {
            return "Unknown";
        }
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Gets key information summary for display purposes.
     * 
     * @return Key information summary
     */
    public String getKeyInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Algorithm: ").append(algorithm != null ? algorithm : "Unknown").append("\n");
        info.append("Created: ").append(getFormattedCreationTime()).append("\n");
        info.append("Public Key Length: ").append(publicKey != null ? publicKey.length() : 0).append(" chars\n");
        info.append("Private Key Length: ").append(privateKey != null ? privateKey.length() : 0).append(" chars\n");
        info.append("AES Key Length: ").append(aesKey != null ? aesKey.length() : 0).append(" chars\n");
        info.append("Vigenère Key: ").append(vigenereKey != null ? vigenereKey : "None");
        return info.toString();
    }
    
    /**
     * Serializes the keys data to a string format for file storage.
     * Format: publicKey\nprivateKey\naesKey\nvigenereKey\nalgorithm\ntimestamp
     * 
     * @return Serialized keys data
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(publicKey != null ? publicKey : "").append("\n");
        sb.append(privateKey != null ? privateKey : "").append("\n");
        sb.append(aesKey != null ? aesKey : "").append("\n");
        sb.append(vigenereKey != null ? vigenereKey : "").append("\n");
        sb.append(algorithm != null ? algorithm : "").append("\n");
        sb.append(createdAt != null ? createdAt.toString() : LocalDateTime.now().toString());
        return sb.toString();
    }
    
    /**
     * Deserializes keys data from a string format.
     * 
     * @param data Serialized keys data
     * @return KeysData instance or null if parsing fails
     */
    public static KeysData deserialize(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] lines = data.split("\n");
            if (lines.length < 5) {
                return null;
            }
            
            String publicKey = lines[0].trim();
            String privateKey = lines[1].trim();
            String aesKey = lines[2].trim();
            String vigenereKey = lines[3].trim();
            String algorithm = lines[4].trim();
            
            LocalDateTime createdAt = LocalDateTime.now();
            if (lines.length > 5 && !lines[5].trim().isEmpty()) {
                try {
                    createdAt = LocalDateTime.parse(lines[5].trim());
                } catch (Exception e) {
                    // Use current time if parsing fails
                    createdAt = LocalDateTime.now();
                }
            }
            
            return new KeysData(publicKey, privateKey, aesKey, vigenereKey, algorithm, createdAt);
        } catch (Exception e) {
            System.err.println("Error deserializing keys data: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String toString() {
        return "KeysData{" +
                "algorithm='" + algorithm + '\'' +
                ", createdAt=" + getFormattedCreationTime() +
                ", valid=" + isValid() +
                '}';
    }
}