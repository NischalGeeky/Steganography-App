package com.stego.gui.model;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data model representing a chat message in the P2P steganographic chat system.
 * Supports text messages, images, and steganographic images with encryption status.
 */
public class ChatMessage {
    
    /**
     * Enumeration of message types supported by the chat system.
     */
    public enum MessageType {
        TEXT,           // Plain text message
        IMAGE,          // Regular image message
        STEGO_IMAGE,    // Image containing hidden steganographic data
        SYSTEM          // System notification message
    }
    
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;
    private String sender;
    private BufferedImage image;
    private boolean encrypted;
    private boolean sent; // true if sent by user, false if received
    
    /**
     * Creates a text message.
     * 
     * @param content Message content
     * @param sender Sender identifier ("self" for own messages, peer ID for received)
     * @param encrypted Whether the message was encrypted
     * @param sent Whether this message was sent (true) or received (false)
     */
    public ChatMessage(String content, String sender, boolean encrypted, boolean sent) {
        this.content = content;
        this.sender = sender;
        this.encrypted = encrypted;
        this.sent = sent;
        this.type = MessageType.TEXT;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Creates an image message.
     * 
     * @param image Image content
     * @param sender Sender identifier
     * @param type Message type (IMAGE or STEGO_IMAGE)
     * @param encrypted Whether the message was encrypted
     * @param sent Whether this message was sent (true) or received (false)
     */
    public ChatMessage(BufferedImage image, String sender, MessageType type, boolean encrypted, boolean sent) {
        this.image = image;
        this.sender = sender;
        this.type = type;
        this.encrypted = encrypted;
        this.sent = sent;
        this.timestamp = LocalDateTime.now();
        this.content = ""; // Empty content for image messages
    }
    
    /**
     * Creates a system message.
     * 
     * @param content System message content
     */
    public static ChatMessage createSystemMessage(String content) {
        ChatMessage message = new ChatMessage(content, "system", false, false);
        message.type = MessageType.SYSTEM;
        return message;
    }
    
    /**
     * Creates a steganographic message with both text and image.
     * 
     * @param content Hidden text content
     * @param image Stego image containing the hidden content
     * @param sender Sender identifier
     * @param sent Whether this message was sent (true) or received (false)
     */
    public static ChatMessage createStegoMessage(String content, BufferedImage image, String sender, boolean sent) {
        ChatMessage message = new ChatMessage(image, sender, MessageType.STEGO_IMAGE, true, sent);
        message.content = content;
        return message;
    }

    // Getters
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public MessageType getType() { return type; }
    public String getSender() { return sender; }
    public BufferedImage getImage() { return image; }
    public boolean isEncrypted() { return encrypted; }
    public boolean isSent() { return sent; }
    
    // Setters
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setType(MessageType type) { this.type = type; }
    public void setSender(String sender) { this.sender = sender; }
    public void setImage(BufferedImage image) { this.image = image; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    public void setSent(boolean sent) { this.sent = sent; }
    
    /**
     * Checks if this message contains an image.
     * 
     * @return true if message has an image, false otherwise
     */
    public boolean hasImage() {
        return image != null && (type == MessageType.IMAGE || type == MessageType.STEGO_IMAGE);
    }
    
    /**
     * Checks if this message contains text content.
     * 
     * @return true if message has text content, false otherwise
     */
    public boolean hasTextContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * Checks if this is a system message.
     * 
     * @return true if system message, false otherwise
     */
    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }
    
    /**
     * Checks if this message was sent by the current user.
     * 
     * @return true if sent by user, false if received
     */
    public boolean isOwnMessage() {
        return "self".equals(sender) || sent;
    }
    
    /**
     * Gets a formatted timestamp string for display.
     * 
     * @return Formatted timestamp
     */
    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "";
        }
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Gets a formatted date and time string for display.
     * 
     * @return Formatted date and time
     */
    public String getFormattedDateTime() {
        if (timestamp == null) {
            return "";
        }
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Gets the display name for the sender.
     * 
     * @return Display name
     */
    public String getSenderDisplayName() {
        if (isSystemMessage()) {
            return "System";
        } else if (isOwnMessage()) {
            return "You";
        } else {
            return sender != null ? sender : "Peer";
        }
    }
    
    /**
     * Gets a summary of the message for display purposes.
     * 
     * @return Message summary
     */
    public String getMessageSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("[").append(getFormattedTimestamp()).append("] ");
        summary.append(getSenderDisplayName()).append(": ");
        
        switch (type) {
            case TEXT:
                summary.append(content);
                if (encrypted) {
                    summary.append(" 🔒");
                }
                break;
            case IMAGE:
                summary.append("📷 Image");
                break;
            case STEGO_IMAGE:
                if (hasTextContent()) {
                    summary.append("🔐 ").append(content);
                } else {
                    summary.append("🔐 Hidden Message");
                }
                break;
            case SYSTEM:
                summary.append("ℹ️ ").append(content);
                break;
        }
        
        return summary.toString();
    }
    
    /**
     * Gets the message content for display, handling different message types.
     * 
     * @return Display content
     */
    public String getDisplayContent() {
        switch (type) {
            case TEXT:
                return content != null ? content : "";
            case IMAGE:
                return "[Image]";
            case STEGO_IMAGE:
                if (hasTextContent()) {
                    return content;
                } else {
                    return "[Steganographic Image]";
                }
            case SYSTEM:
                return content != null ? content : "";
            default:
                return "";
        }
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", timestamp=" + getFormattedDateTime() +
                ", encrypted=" + encrypted +
                ", sent=" + sent +
                ", hasImage=" + hasImage() +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }
}