package com.stego.gui.model;

import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data model representing a peer-to-peer connection in the steganographic chat system.
 * Manages connection state, peer information, and network socket details.
 */
public class PeerConnection {
    
    /**
     * Enumeration of possible connection states.
     */
    public enum ConnectionStatus {
        DISCONNECTED,   // Not connected
        CONNECTING,     // Attempting to connect
        CONNECTED,      // Successfully connected
        RECONNECTING,   // Attempting to reconnect after failure
        ERROR           // Connection error occurred
    }
    
    private String peerAddress;
    private int peerPort;
    private Socket socket;
    private boolean isHost;
    private ConnectionStatus status;
    private LocalDateTime connectedAt;
    private LocalDateTime lastActivity;
    private String errorMessage;
    private int reconnectAttempts;
    
    /**
     * Creates a new peer connection for hosting.
     * 
     * @param port Port to listen on
     */
    public PeerConnection(int port) {
        this.peerPort = port;
        this.isHost = true;
        this.status = ConnectionStatus.DISCONNECTED;
        this.reconnectAttempts = 0;
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Creates a new peer connection for connecting to a remote peer.
     * 
     * @param peerAddress Remote peer IP address
     * @param peerPort Remote peer port
     */
    public PeerConnection(String peerAddress, int peerPort) {
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.isHost = false;
        this.status = ConnectionStatus.DISCONNECTED;
        this.reconnectAttempts = 0;
        this.lastActivity = LocalDateTime.now();
    }

    // Getters
    public String getPeerAddress() { return peerAddress; }
    public int getPeerPort() { return peerPort; }
    public Socket getSocket() { return socket; }
    public boolean isHost() { return isHost; }
    public ConnectionStatus getStatus() { return status; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public String getErrorMessage() { return errorMessage; }
    public int getReconnectAttempts() { return reconnectAttempts; }
    
    // Setters
    public void setPeerAddress(String peerAddress) { 
        this.peerAddress = peerAddress;
        updateLastActivity();
    }
    
    public void setPeerPort(int peerPort) { 
        this.peerPort = peerPort;
        updateLastActivity();
    }
    
    public void setSocket(Socket socket) { 
        this.socket = socket;
        if (socket != null && socket.isConnected()) {
            this.peerAddress = socket.getInetAddress().getHostAddress();
            this.status = ConnectionStatus.CONNECTED;
            this.connectedAt = LocalDateTime.now();
        }
        updateLastActivity();
    }
    
    public void setHost(boolean host) { this.isHost = host; }
    
    public void setStatus(ConnectionStatus status) { 
        this.status = status;
        if (status == ConnectionStatus.CONNECTED && connectedAt == null) {
            this.connectedAt = LocalDateTime.now();
        } else if (status == ConnectionStatus.DISCONNECTED) {
            this.connectedAt = null;
        }
        updateLastActivity();
    }
    
    public void setErrorMessage(String errorMessage) { 
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            this.status = ConnectionStatus.ERROR;
        }
        updateLastActivity();
    }
    
    public void incrementReconnectAttempts() { 
        this.reconnectAttempts++;
        updateLastActivity();
    }
    
    public void resetReconnectAttempts() { 
        this.reconnectAttempts = 0;
        updateLastActivity();
    }
    
    /**
     * Updates the last activity timestamp.
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Checks if the connection is currently active.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return status == ConnectionStatus.CONNECTED && 
               socket != null && 
               socket.isConnected() && 
               !socket.isClosed();
    }
    
    /**
     * Checks if the connection is in an error state.
     * 
     * @return true if error state, false otherwise
     */
    public boolean hasError() {
        return status == ConnectionStatus.ERROR || errorMessage != null;
    }
    
    /**
     * Checks if the connection is attempting to connect or reconnect.
     * 
     * @return true if connecting, false otherwise
     */
    public boolean isConnecting() {
        return status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING;
    }
    
    /**
     * Gets the connection identifier for display purposes.
     * 
     * @return Connection identifier
     */
    public String getConnectionId() {
        if (isHost) {
            return "Host:" + peerPort;
        } else {
            return peerAddress + ":" + peerPort;
        }
    }
    
    /**
     * Gets a formatted string of the connection time.
     * 
     * @return Formatted connection time or "Not connected"
     */
    public String getFormattedConnectionTime() {
        if (connectedAt == null) {
            return "Not connected";
        }
        return connectedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Gets a formatted string of the last activity time.
     * 
     * @return Formatted last activity time
     */
    public String getFormattedLastActivity() {
        if (lastActivity == null) {
            return "Unknown";
        }
        return lastActivity.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Gets the connection duration in a human-readable format.
     * 
     * @return Connection duration or "Not connected"
     */
    public String getConnectionDuration() {
        if (connectedAt == null || !isConnected()) {
            return "Not connected";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(connectedAt, now).getSeconds();
        
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else {
            return (seconds / 3600) + " hours";
        }
    }
    
    /**
     * Gets connection status information for display.
     * 
     * @return Status information string
     */
    public String getStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Status: ").append(status.toString().toLowerCase().replace('_', ' '));
        
        if (isConnected()) {
            info.append("\nConnected: ").append(getFormattedConnectionTime());
            info.append("\nDuration: ").append(getConnectionDuration());
        }
        
        if (hasError() && errorMessage != null) {
            info.append("\nError: ").append(errorMessage);
        }
        
        if (reconnectAttempts > 0) {
            info.append("\nReconnect attempts: ").append(reconnectAttempts);
        }
        
        info.append("\nLast activity: ").append(getFormattedLastActivity());
        
        return info.toString();
    }
    
    /**
     * Closes the connection and cleans up resources.
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing socket: " + e.getMessage());
        } finally {
            socket = null;
            status = ConnectionStatus.DISCONNECTED;
            connectedAt = null;
            updateLastActivity();
        }
    }
    
    /**
     * Validates the connection parameters.
     * 
     * @return true if parameters are valid, false otherwise
     */
    public boolean isValidConfiguration() {
        if (isHost) {
            return peerPort > 0 && peerPort <= 65535;
        } else {
            return peerAddress != null && !peerAddress.trim().isEmpty() &&
                   peerPort > 0 && peerPort <= 65535;
        }
    }
    
    @Override
    public String toString() {
        return "PeerConnection{" +
                "id='" + getConnectionId() + '\'' +
                ", status=" + status +
                ", isHost=" + isHost +
                ", connected=" + isConnected() +
                ", reconnectAttempts=" + reconnectAttempts +
                ", lastActivity=" + getFormattedLastActivity() +
                '}';
    }
}