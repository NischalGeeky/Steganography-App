package com.stego.gui;

import com.stego.FileEncryptor;
import com.stego.FileDecryptor;
import com.stego.ImageStego;
import com.stego.gui.model.ChatMessage;
import com.stego.gui.model.KeysData;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

/**
 * Handles message processing for the P2P steganographic chat system.
 * Manages encryption, steganographic embedding, extraction, and decryption.
 */
public class ChatMessageHandler {
    
    private KeysData keys;
    private List<ChatMessage> messageHistory;
    private Consumer<ChatMessage> messageAddedHandler;
    private Consumer<String> statusHandler;
    private Consumer<Exception> errorHandler;
    
    /**
     * Creates a new chat message handler.
     */
    public ChatMessageHandler() {
        this.messageHistory = new ArrayList<>();
    }
    
    /**
     * Initializes the handler with cryptographic keys.
     * 
     * @param keys Cryptographic keys for encryption/decryption
     * @throws Exception if keys are invalid
     */
    public void initialize(KeysData keys) throws Exception {
        if (keys == null || !keys.isValid()) {
            throw new Exception("Invalid or missing cryptographic keys");
        }
        this.keys = keys;
        updateStatus("Message handler initialized with " + keys.getAlgorithm() + " keys");
    }
    
    /**
     * Sets the handler for when new messages are added to history.
     * 
     * @param handler Message added handler
     */
    public void setMessageAddedHandler(Consumer<ChatMessage> handler) {
        this.messageAddedHandler = handler;
    }
    
    /**
     * Sets the status update handler.
     * 
     * @param handler Status handler
     */
    public void setStatusHandler(Consumer<String> handler) {
        this.statusHandler = handler;
    }
    
    /**
     * Sets the error handler.
     * 
     * @param handler Error handler
     */
    public void setErrorHandler(Consumer<Exception> handler) {
        this.errorHandler = handler;
    }
    
    /**
     * Processes an outgoing message by encrypting and embedding it in an image.
     * 
     * @param message Message text to send
     * @param coverImage Cover image file for steganography
     * @return Steganographic image containing the encrypted message
     * @throws Exception if processing fails
     */
    public BufferedImage processOutgoingMessage(String message, File coverImage) throws Exception {
        if (keys == null) {
            throw new Exception("Message handler not initialized with keys");
        }
        
        if (message == null || message.trim().isEmpty()) {
            throw new Exception("Message cannot be empty");
        }
        
        if (coverImage == null || !coverImage.exists()) {
            throw new Exception("Cover image file is required");
        }
        
        try {
            updateStatus("Processing outgoing message...");
            
            // Step 1: Validate message capacity
            validateMessageCapacity(message, coverImage);
            
            // Step 2: Encrypt the message using shared keys
            updateStatus("Encrypting message...");
            String encryptedMessage = encryptMessageWithSharedKeys(message);
            
            // Step 3: Create temporary files for steganographic processing
            File tempInput = File.createTempFile("stego_input_", ".png");
            File tempOutput = File.createTempFile("stego_output_", ".png");
            
            try {
                // Copy cover image to temp file
                BufferedImage coverImg = ImageIO.read(coverImage);
                ImageIO.write(coverImg, "PNG", tempInput);
                
                // Step 4: Embed encrypted message using existing ImageStego
                updateStatus("Embedding message in image...");
                ImageStego.encode(tempInput.getAbsolutePath(), tempOutput.getAbsolutePath(), 
                                encryptedMessage, keys.getVigenereKey());
                
                // Step 5: Read the result
                BufferedImage stegoImage = ImageIO.read(tempOutput);
                
                // Step 6: Add to message history
                ChatMessage chatMessage = ChatMessage.createStegoMessage(message, stegoImage, "self", true);
                addMessageToHistory(chatMessage);
                
                updateStatus("Message processed and embedded successfully");
                return stegoImage;
                
            } finally {
                // Clean up temporary files
                if (tempInput.exists()) tempInput.delete();
                if (tempOutput.exists()) tempOutput.delete();
            }
            
        } catch (Exception e) {
            handleError(new Exception("Failed to process outgoing message: " + e.getMessage(), e));
            throw e;
        }
    }
    
    /**
     * Processes an incoming steganographic image by extracting and decrypting the message.
     * 
     * @param stegoImage Steganographic image received from peer
     * @return Extracted and decrypted message, or null if no message found
     * @throws Exception if processing fails
     */
    public String processIncomingMessage(BufferedImage stegoImage) throws Exception {
        if (keys == null) {
            throw new Exception("Message handler not initialized with keys");
        }
        
        if (stegoImage == null) {
            throw new Exception("Steganographic image is required");
        }
        
        try {
            updateStatus("Processing incoming message...");
            
            // Step 1: Create temporary file for steganographic processing
            File tempFile = File.createTempFile("stego_received_", ".png");
            
            try {
                // Write image to temp file
                ImageIO.write(stegoImage, "PNG", tempFile);
                
                // Step 2: Extract encrypted message using existing ImageStego
                updateStatus("Extracting message from image...");
                String extractedCiphertext = ImageStego.decode(tempFile.getAbsolutePath(), keys.getVigenereKey());
                
                if (extractedCiphertext == null || extractedCiphertext.trim().isEmpty()) {
                    // No steganographic content found - treat as regular image
                    ChatMessage imageMessage = new ChatMessage(stegoImage, "peer", 
                                                             ChatMessage.MessageType.IMAGE, false, false);
                    addMessageToHistory(imageMessage);
                    updateStatus("Received regular image (no hidden message)");
                    return null;
                }
                
                // Step 3: Decrypt the extracted message
                updateStatus("Decrypting message...");
                String decryptedMessage = FileDecryptor.decryptText(extractedCiphertext, 
                                                                  keys.getAesKey(), keys.getVigenereKey());
                
                // Step 4: Add to message history
                ChatMessage chatMessage = ChatMessage.createStegoMessage(decryptedMessage, stegoImage, "peer", false);
                addMessageToHistory(chatMessage);
                
                updateStatus("Message extracted and decrypted successfully");
                return decryptedMessage;
                
            } finally {
                // Clean up temporary file
                if (tempFile.exists()) tempFile.delete();
            }
            
        } catch (Exception e) {
            // If decryption fails, treat as regular image
            ChatMessage imageMessage = new ChatMessage(stegoImage, "peer", 
                                                     ChatMessage.MessageType.IMAGE, false, false);
            addMessageToHistory(imageMessage);
            
            handleError(new Exception("Failed to process incoming message (treating as regular image): " + e.getMessage(), e));
            return null;
        }
    }
    
    /**
     * Validates that a message can fit in the given cover image.
     * 
     * @param message Message to validate
     * @param coverImage Cover image file
     * @throws Exception if message is too long for the image
     */
    public void validateMessageCapacity(String message, File coverImage) throws Exception {
        if (message == null || coverImage == null) {
            return;
        }
        
        try {
            // Read image to get dimensions
            BufferedImage img = ImageIO.read(coverImage);
            if (img == null) {
                throw new Exception("Unable to read cover image");
            }
            
            int width = img.getWidth();
            int height = img.getHeight();
            
            // Calculate approximate capacity based on ImageStego implementation
            // Each 8x8 block can store 8 bits, but we need to account for:
            // - Message length header (4 bytes)
            // - Texture-adaptive masking (reduces available blocks)
            // - Split-payload overhead
            
            int totalBlocks = (width / 8) * (height / 8);
            int estimatedUsableBlocks = (int) (totalBlocks * 0.7); // 70% due to texture masking
            int maxMessageBytes = estimatedUsableBlocks - 4; // Subtract header
            
            // Encrypt message to get actual size
            FileEncryptor.Output encryptionOutput = FileEncryptor.encryptText(message);
            int actualMessageBytes = encryptionOutput.getFinalOutput().getBytes().length;
            
            if (actualMessageBytes > maxMessageBytes) {
                throw new Exception(String.format(
                    "Message too long for image. Message: %d bytes, Image capacity: ~%d bytes. " +
                    "Try a shorter message or larger image.",
                    actualMessageBytes, maxMessageBytes));
            }
            
            updateStatus(String.format("Message capacity check passed (%d/%d bytes)", 
                                     actualMessageBytes, maxMessageBytes));
            
        } catch (Exception e) {
            if (e.getMessage().contains("Message too long")) {
                throw e;
            } else {
                throw new Exception("Failed to validate message capacity: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Adds a message to the chat history.
     * 
     * @param message Message to add
     */
    public void addMessageToHistory(ChatMessage message) {
        if (message != null) {
            messageHistory.add(message);
            if (messageAddedHandler != null) {
                messageAddedHandler.accept(message);
            }
        }
    }
    
    /**
     * Adds a system message to the chat history.
     * 
     * @param content System message content
     */
    public void addSystemMessage(String content) {
        ChatMessage systemMessage = ChatMessage.createSystemMessage(content);
        addMessageToHistory(systemMessage);
    }
    
    /**
     * Gets the complete message history.
     * 
     * @return List of all chat messages
     */
    public List<ChatMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    /**
     * Clears the message history.
     */
    public void clearHistory() {
        messageHistory.clear();
        updateStatus("Message history cleared");
    }
    
    /**
     * Gets the current cryptographic keys.
     * 
     * @return Current keys or null if not initialized
     */
    public KeysData getKeys() {
        return keys;
    }
    
    /**
     * Checks if the handler is initialized with keys.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return keys != null && keys.isValid();
    }
    
    /**
     * Encrypts a message using the shared keys from the keys.enc file.
     * Uses the same entropy-based cipher selection as FileEncryptor but with shared keys.
     * 
     * @param message Message to encrypt
     * @return Encrypted message string
     * @throws Exception if encryption fails
     */
    private String encryptMessageWithSharedKeys(String message) throws Exception {
        if (keys == null || !keys.isValid()) {
            throw new Exception("No valid keys available for encryption");
        }
        
        StringBuilder finalOutput = new StringBuilder();
        String[] words = message.split("\\s+");
        
        // Use shared keys from keys.enc file
        String vigenereKey = keys.getVigenereKey();
        String aesKey = keys.getAesKey();
        
        for (String word : words) {
            String encryptedWord;
            double entropy = FileEncryptor.calculateEntropy(word);
            
            // Same entropy-based cipher selection as FileEncryptor
            if (entropy > 2.5) { // ENTROPY_THRESHOLD from FileEncryptor
                encryptedWord = FileEncryptor.aesEncrypt(word, aesKey);
            } else {
                encryptedWord = FileEncryptor.vigenereCipher(word, vigenereKey);
            }
            
            finalOutput.append(encryptedWord).append(" ");
        }
        
        String result = finalOutput.toString().trim();
        return result;
    }
    
    /**
     * Gets statistics about the message handler.
     * 
     * @return Statistics string
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Message Handler Statistics:\n");
        stats.append("Initialized: ").append(isInitialized()).append("\n");
        stats.append("Total Messages: ").append(messageHistory.size()).append("\n");
        
        if (isInitialized()) {
            stats.append("Algorithm: ").append(keys.getAlgorithm()).append("\n");
        }
        
        long textMessages = messageHistory.stream()
            .filter(m -> m.getType() == ChatMessage.MessageType.TEXT)
            .count();
        long imageMessages = messageHistory.stream()
            .filter(m -> m.getType() == ChatMessage.MessageType.IMAGE)
            .count();
        long stegoMessages = messageHistory.stream()
            .filter(m -> m.getType() == ChatMessage.MessageType.STEGO_IMAGE)
            .count();
        long systemMessages = messageHistory.stream()
            .filter(m -> m.getType() == ChatMessage.MessageType.SYSTEM)
            .count();
        
        stats.append("Text Messages: ").append(textMessages).append("\n");
        stats.append("Image Messages: ").append(imageMessages).append("\n");
        stats.append("Steganographic Messages: ").append(stegoMessages).append("\n");
        stats.append("System Messages: ").append(systemMessages);
        
        return stats.toString();
    }
    
    /**
     * Updates status through the status handler.
     * 
     * @param status Status message
     */
    private void updateStatus(String status) {
        if (statusHandler != null) {
            statusHandler.accept(status);
        }
    }
    
    /**
     * Handles errors through the error handler.
     * 
     * @param error Error to handle
     */
    private void handleError(Exception error) {
        if (errorHandler != null) {
            errorHandler.accept(error);
        }
    }
}