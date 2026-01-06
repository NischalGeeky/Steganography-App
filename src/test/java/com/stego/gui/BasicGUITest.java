package com.stego.gui;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.stego.gui.model.KeysData;

/**
 * Basic tests for GUI functionality.
 * Tests key generation, validation, and basic operations.
 */
public class BasicGUITest {
    
    private KeyManager keyManager;
    private Path tempDir;
    
    @Before
    public void setUp() throws IOException {
        keyManager = new KeyManager();
        tempDir = Files.createTempDirectory("stego-test");
    }
    
    /**
     * Property 1: Key generation produces valid keys
     * Validates: Requirements 1.2, 1.5
     */
    @Test
    public void testKeyGenerationProducesValidKeys() {
        try {
            // Generate keys
            KeysData keys = keyManager.generateKeys();
            
            // Verify all key components are present
            assertNotNull("Public key should not be null", keys.getPublicKey());
            assertNotNull("Private key should not be null", keys.getPrivateKey());
            assertNotNull("AES key should not be null", keys.getAesKey());
            assertNotNull("Vigenere key should not be null", keys.getVigenereKey());
            assertNotNull("Algorithm should not be null", keys.getAlgorithm());
            
            // Verify keys are valid
            assertTrue("Keys should be valid", keys.isValid());
            
            // Verify key lengths are reasonable
            assertTrue("Public key should have reasonable length", 
                keys.getPublicKey().length() > 10);
            assertTrue("Private key should have reasonable length", 
                keys.getPrivateKey().length() > 10);
            assertTrue("AES key should have reasonable length", 
                keys.getAesKey().length() > 10);
            assertTrue("Vigenere key should have reasonable length", 
                keys.getVigenereKey().length() > 5);
            
            // Verify algorithm is valid
            assertTrue("Algorithm should be a valid Kyber variant",
                keys.getAlgorithm().startsWith("Kyber"));
                
        } catch (Exception e) {
            fail("Key generation should not throw exceptions: " + e.getMessage());
        }
    }
    
    /**
     * Property 2: Keys file validation is consistent
     * Validates: Requirements 1.4
     */
    @Test
    public void testKeyFileValidationConsistency() {
        try {
            // Generate and save keys
            KeysData originalKeys = keyManager.generateKeys();
            File keyFile = tempDir.resolve("test-keys.enc").toFile();
            keyManager.saveKeys(originalKeys, keyFile);
            
            // Verify file was created
            assertTrue("Key file should exist", keyFile.exists());
            assertTrue("Key file should have content", keyFile.length() > 0);
            
            // Load keys back
            KeysData loadedKeys = keyManager.loadKeys(keyFile);
            
            // Verify loaded keys are valid
            assertNotNull("Loaded keys should not be null", loadedKeys);
            assertTrue("Loaded keys should be valid", loadedKeys.isValid());
            
            // Verify loaded keys match original
            assertEquals("Public keys should match", 
                originalKeys.getPublicKey(), loadedKeys.getPublicKey());
            assertEquals("Private keys should match", 
                originalKeys.getPrivateKey(), loadedKeys.getPrivateKey());
            assertEquals("AES keys should match", 
                originalKeys.getAesKey(), loadedKeys.getAesKey());
            assertEquals("Vigenere keys should match", 
                originalKeys.getVigenereKey(), loadedKeys.getVigenereKey());
            assertEquals("Algorithms should match", 
                originalKeys.getAlgorithm(), loadedKeys.getAlgorithm());
            
        } catch (Exception e) {
            fail("Key file validation test failed: " + e.getMessage());
        }
    }
    
    /**
     * Property 3: Chat message handler initialization
     * Validates: Requirements 2.2
     */
    @Test
    public void testChatMessageHandlerInitialization() {
        try {
            // Generate keys
            KeysData keys = keyManager.generateKeys();
            
            // Create and initialize handler
            ChatMessageHandler handler = new ChatMessageHandler();
            handler.initialize(keys);
            
            // Verify handler accepts the keys
            assertNotNull("Handler should not be null", handler);
            
            // Test that handler rejects null keys
            ChatMessageHandler handler2 = new ChatMessageHandler();
            try {
                handler2.initialize(null);
                fail("Handler should reject null keys");
            } catch (Exception e) {
                // Expected behavior
                assertTrue("Should get meaningful error message", 
                    e.getMessage().contains("Invalid") || e.getMessage().contains("null"));
            }
            
        } catch (Exception e) {
            fail("Chat message handler initialization test failed: " + e.getMessage());
        }
    }
    
    /**
     * Property 4: KeysData serialization round-trip
     * Validates: Requirements 1.4, 1.5
     */
    @Test
    public void testKeysDataSerializationRoundTrip() {
        try {
            // Generate keys
            KeysData originalKeys = keyManager.generateKeys();
            
            // Serialize keys
            String serialized = originalKeys.serialize();
            assertNotNull("Serialized data should not be null", serialized);
            assertFalse("Serialized data should not be empty", serialized.trim().isEmpty());
            
            // Deserialize keys
            KeysData deserializedKeys = KeysData.deserialize(serialized);
            assertNotNull("Deserialized keys should not be null", deserializedKeys);
            assertTrue("Deserialized keys should be valid", deserializedKeys.isValid());
            
            // Verify round-trip integrity
            assertEquals("Public keys should match after round-trip", 
                originalKeys.getPublicKey(), deserializedKeys.getPublicKey());
            assertEquals("Private keys should match after round-trip", 
                originalKeys.getPrivateKey(), deserializedKeys.getPrivateKey());
            assertEquals("AES keys should match after round-trip", 
                originalKeys.getAesKey(), deserializedKeys.getAesKey());
            assertEquals("Vigenere keys should match after round-trip", 
                originalKeys.getVigenereKey(), deserializedKeys.getVigenereKey());
            assertEquals("Algorithms should match after round-trip", 
                originalKeys.getAlgorithm(), deserializedKeys.getAlgorithm());
            
        } catch (Exception e) {
            fail("KeysData serialization round-trip test failed: " + e.getMessage());
        }
    }
}