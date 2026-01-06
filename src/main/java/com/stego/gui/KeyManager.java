package com.stego.gui;

import com.stego.FileEncryptor;
import com.stego.LatticeManager;
import com.stego.gui.model.KeysData;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.time.LocalDateTime;

/**
 * Manager class for handling cryptographic key operations in the GUI applications.
 * Provides key generation, loading, saving, and validation functionality.
 */
public class KeyManager {
    
    /**
     * Generates a new set of cryptographic keys including Kyber keys and session keys.
     * 
     * @return KeysData containing all generated keys
     * @throws Exception if key generation fails
     */
    public static KeysData generateKeys() throws Exception {
        try {
            // Generate Kyber key pair using existing LatticeManager
            KeyPair latticeKeyPair = LatticeManager.generateLatticeKeyPair();
            String publicKey = LatticeManager.keyToString(latticeKeyPair.getPublic());
            String privateKey = LatticeManager.keyToString(latticeKeyPair.getPrivate());
            
            // Generate session keys using existing FileEncryptor
            String aesKey = FileEncryptor.generateAESKey();
            String vigenereKey = FileEncryptor.generateVigenereKey(8); // Use 8-character key
            
            // Get algorithm from configuration (same as LatticeManager uses)
            String algorithm = getCurrentPQCAlgorithm();
            
            // Create and return KeysData
            return new KeysData(publicKey, privateKey, aesKey, vigenereKey, algorithm);
            
        } catch (Exception e) {
            throw new Exception("Failed to generate keys: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads keys from a keys.enc file.
     * 
     * @param file Keys file to load
     * @return KeysData containing loaded keys
     * @throws Exception if loading fails
     */
    public static KeysData loadKeys(File file) throws Exception {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new Exception("Invalid or unreadable key file");
        }
        
        try {
            // Read file content
            String content = new String(Files.readAllBytes(file.toPath()));
            
            // Deserialize keys data
            KeysData keysData = KeysData.deserialize(content);
            if (keysData == null) {
                throw new Exception("Invalid key file format");
            }
            
            // Validate keys
            if (!keysData.isValid()) {
                throw new Exception("Key file contains invalid or incomplete key data");
            }
            
            // Additional validation - try to reconstruct keys to ensure they're valid
            try {
                LatticeManager.stringToPublicKey(keysData.getPublicKey());
                LatticeManager.stringToPrivateKey(keysData.getPrivateKey());
            } catch (Exception e) {
                throw new Exception("Key file contains corrupted cryptographic keys: " + e.getMessage());
            }
            
            return keysData;
            
        } catch (Exception e) {
            throw new Exception("Failed to load keys from file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Saves keys to a keys.enc file.
     * 
     * @param keysData Keys to save
     * @param file Target file
     * @throws Exception if saving fails
     */
    public static void saveKeys(KeysData keysData, File file) throws Exception {
        if (keysData == null || !keysData.isValid()) {
            throw new Exception("Invalid keys data - cannot save");
        }
        
        if (file == null) {
            throw new Exception("No target file specified");
        }
        
        try {
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new Exception("Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }
            
            // Serialize and write keys data
            String serializedData = keysData.serialize();
            Files.write(file.toPath(), serializedData.getBytes());
            
            // Verify the file was written correctly
            if (!file.exists() || file.length() == 0) {
                throw new Exception("Failed to write key file or file is empty");
            }
            
        } catch (Exception e) {
            throw new Exception("Failed to save keys to file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates a keys.enc file without fully loading it.
     * 
     * @param file File to validate
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateKeyFile(File file) {
        if (file == null) {
            return new ValidationResult(false, "No file specified");
        }
        
        if (!file.exists()) {
            return new ValidationResult(false, "File does not exist");
        }
        
        if (!file.canRead()) {
            return new ValidationResult(false, "File is not readable - check permissions");
        }
        
        if (file.length() == 0) {
            return new ValidationResult(false, "File is empty");
        }
        
        if (file.length() > 1024 * 1024) { // 1MB limit
            return new ValidationResult(false, "File is too large (max 1MB for key files)");
        }
        
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            
            if (content.trim().isEmpty()) {
                return new ValidationResult(false, "File contains no data");
            }
            
            // Try to deserialize
            KeysData keysData = KeysData.deserialize(content);
            if (keysData == null) {
                return new ValidationResult(false, "Invalid key file format - unable to parse");
            }
            
            if (!keysData.isValid()) {
                return new ValidationResult(false, "Key file contains incomplete or invalid key data");
            }
            
            // Try to validate cryptographic keys
            try {
                LatticeManager.stringToPublicKey(keysData.getPublicKey());
                LatticeManager.stringToPrivateKey(keysData.getPrivateKey());
            } catch (Exception e) {
                return new ValidationResult(false, "Key file contains corrupted cryptographic keys: " + e.getMessage());
            }
            
            return new ValidationResult(true, "Valid key file with " + keysData.getAlgorithm() + " keys");
            
        } catch (Exception e) {
            return new ValidationResult(false, "Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Result of key file validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return (valid ? "✓ " : "✗ ") + message;
        }
    }
    
    /**
     * Gets the current PQC algorithm from configuration.
     * This mirrors the logic in LatticeManager but provides a public interface.
     * 
     * @return Current PQC algorithm name
     */
    private static String getCurrentPQCAlgorithm() {
        try {
            // Try to read from config.properties
            java.io.InputStream configStream = KeyManager.class.getResourceAsStream("/config.properties");
            if (configStream != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(configStream);
                configStream.close();
                
                String algorithm = props.getProperty("PQC_ALGORITHM", "Kyber768").trim();
                return algorithm;
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to read PQC algorithm from config: " + e.getMessage());
        }
        
        // Default fallback
        return "Kyber768";
    }
    
    /**
     * Creates a backup of an existing key file before overwriting.
     * 
     * @param file Original file
     * @return Backup file, or null if backup failed
     */
    public static File createBackup(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        
        try {
            String backupName = file.getName() + ".backup." + System.currentTimeMillis();
            File backupFile = new File(file.getParent(), backupName);
            Files.copy(file.toPath(), backupFile.toPath());
            return backupFile;
        } catch (Exception e) {
            System.err.println("Warning: Failed to create backup of key file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets file information for display purposes.
     * 
     * @param file File to analyze
     * @return File information string
     */
    public static String getFileInfo(File file) {
        if (file == null || !file.exists()) {
            return "File does not exist";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("File: ").append(file.getName()).append("\n");
        info.append("Path: ").append(file.getAbsolutePath()).append("\n");
        info.append("Size: ").append(file.length()).append(" bytes\n");
        info.append("Last Modified: ").append(new java.util.Date(file.lastModified())).append("\n");
        info.append("Readable: ").append(file.canRead()).append("\n");
        info.append("Writable: ").append(file.canWrite()).append("\n");
        
        // Try to validate as key file
        ValidationResult validation = validateKeyFile(file);
        info.append("Validation: ").append(validation.toString());
        
        return info.toString();
    }
}