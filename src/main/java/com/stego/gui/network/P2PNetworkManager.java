package com.stego.gui.network;

import com.stego.gui.model.PeerConnection;
import javafx.application.Platform;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

/**
 * Manages peer-to-peer network connections for the steganographic chat system.
 * Handles server hosting, client connections, and image transmission.
 */
public class P2PNetworkManager {
    
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int SO_TIMEOUT = 30000; // 30 seconds for socket operations
    private static final String IMAGE_FORMAT = "PNG";
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private PeerConnection currentConnection;
    private Consumer<String> messageHandler;
    private Consumer<BufferedImage> imageHandler;
    private Consumer<String> statusHandler;
    private Consumer<Exception> errorHandler;
    private volatile boolean isRunning = false;
    
    /**
     * Creates a new P2P Network Manager.
     */
    public P2PNetworkManager() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "P2P-Network-Thread");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Sets the message handler for received text messages.
     * 
     * @param handler Message handler function
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    /**
     * Sets the image handler for received images.
     * 
     * @param handler Image handler function
     */
    public void setImageHandler(Consumer<BufferedImage> handler) {
        this.imageHandler = handler;
    }
    
    /**
     * Sets the status handler for connection status updates.
     * 
     * @param handler Status handler function
     */
    public void setStatusHandler(Consumer<String> handler) {
        this.statusHandler = handler;
    }
    
    /**
     * Sets the error handler for network errors.
     * 
     * @param handler Error handler function
     */
    public void setErrorHandler(Consumer<Exception> handler) {
        this.errorHandler = handler;
    }
    
    /**
     * Starts a server socket and listens for incoming connections.
     * 
     * @param port Port to listen on
     * @return CompletableFuture that completes when server is started
     */
    public CompletableFuture<PeerConnection> startServer(int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isRunning) {
                    throw new IllegalStateException("Network manager is already running");
                }
                
                updateStatus("Starting server on port " + port + "...");
                
                // Create server socket
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));
                serverSocket.setSoTimeout(0); // No timeout for accept()
                
                // Create connection object
                currentConnection = new PeerConnection(port);
                currentConnection.setStatus(PeerConnection.ConnectionStatus.CONNECTING);
                
                isRunning = true;
                updateStatus("Server started, waiting for connections on port " + port);
                
                // Start accepting connections in background
                executorService.submit(this::acceptConnections);
                
                return currentConnection;
                
            } catch (Exception e) {
                handleError(new Exception("Failed to start server: " + e.getMessage(), e));
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Connects to a remote peer as a client.
     * 
     * @param address Remote peer IP address
     * @param port Remote peer port
     * @return CompletableFuture that completes when connection is established
     */
    public CompletableFuture<PeerConnection> connectToServer(String address, int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (isRunning) {
                    throw new IllegalStateException("Network manager is already running");
                }
                
                updateStatus("Connecting to " + address + ":" + port + "...");
                
                // Create socket with timeout
                Socket socket = new Socket();
                socket.setSoTimeout(SO_TIMEOUT);
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(address, port), CONNECTION_TIMEOUT);
                
                // Create connection object
                currentConnection = new PeerConnection(address, port);
                currentConnection.setSocket(socket);
                currentConnection.setStatus(PeerConnection.ConnectionStatus.CONNECTED);
                
                isRunning = true;
                updateStatus("Connected to " + address + ":" + port);
                
                // Start handling connection in background
                executorService.submit(() -> handleConnection(socket));
                
                return currentConnection;
                
            } catch (Exception e) {
                handleError(new Exception("Failed to connect to server: " + e.getMessage(), e));
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Sends an image to the connected peer.
     * 
     * @param image Image to send
     * @return CompletableFuture that completes when image is sent
     */
    public CompletableFuture<Void> sendImage(BufferedImage image) {
        return CompletableFuture.runAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Not connected to any peer");
            }
            
            try {
                Socket socket = currentConnection.getSocket();
                OutputStream out = socket.getOutputStream();
                
                // Create a protocol: [MESSAGE_TYPE][DATA_LENGTH][DATA]
                // MESSAGE_TYPE: "IMG" for images, "TXT" for text
                // DATA_LENGTH: 4 bytes (int) indicating data size
                
                // Convert image to byte array
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, IMAGE_FORMAT, baos);
                byte[] imageData = baos.toByteArray();
                
                // Send protocol header
                out.write("IMG".getBytes());
                
                // Send data length (4 bytes, big-endian)
                out.write((imageData.length >>> 24) & 0xFF);
                out.write((imageData.length >>> 16) & 0xFF);
                out.write((imageData.length >>> 8) & 0xFF);
                out.write(imageData.length & 0xFF);
                
                // Send image data
                out.write(imageData);
                out.flush();
                
                currentConnection.updateLastActivity();
                updateStatus("Image sent (" + imageData.length + " bytes)");
                
            } catch (Exception e) {
                handleError(new Exception("Failed to send image: " + e.getMessage(), e));
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Sends a text message to the connected peer.
     * 
     * @param message Text message to send
     * @return CompletableFuture that completes when message is sent
     */
    public CompletableFuture<Void> sendMessage(String message) {
        return CompletableFuture.runAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Not connected to any peer");
            }
            
            try {
                Socket socket = currentConnection.getSocket();
                OutputStream out = socket.getOutputStream();
                
                // Convert message to bytes
                byte[] messageData = message.getBytes("UTF-8");
                
                // Send protocol header
                out.write("TXT".getBytes());
                
                // Send data length (4 bytes, big-endian)
                out.write((messageData.length >>> 24) & 0xFF);
                out.write((messageData.length >>> 16) & 0xFF);
                out.write((messageData.length >>> 8) & 0xFF);
                out.write(messageData.length & 0xFF);
                
                // Send message data
                out.write(messageData);
                out.flush();
                
                currentConnection.updateLastActivity();
                updateStatus("Message sent (" + messageData.length + " bytes)");
                
            } catch (Exception e) {
                handleError(new Exception("Failed to send message: " + e.getMessage(), e));
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Checks if currently connected to a peer.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return currentConnection != null && currentConnection.isConnected();
    }
    
    /**
     * Gets the current peer connection.
     * 
     * @return Current connection or null if not connected
     */
    public PeerConnection getCurrentConnection() {
        return currentConnection;
    }
    
    /**
     * Disconnects from the current peer and stops the network manager.
     */
    public void disconnect() {
        isRunning = false;
        
        try {
            // Close current connection
            if (currentConnection != null) {
                currentConnection.disconnect();
                currentConnection = null;
            }
            
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
            
            updateStatus("Disconnected");
            
        } catch (Exception e) {
            handleError(new Exception("Error during disconnect: " + e.getMessage(), e));
        }
    }
    
    /**
     * Shuts down the network manager and releases resources.
     */
    public void shutdown() {
        disconnect();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * Accepts incoming connections (for server mode).
     */
    private void acceptConnections() {
        while (isRunning && serverSocket != null && !serverSocket.isClosed()) {
            try {
                updateStatus("Waiting for peer connection...");
                Socket clientSocket = serverSocket.accept();
                
                if (!isRunning) {
                    clientSocket.close();
                    break;
                }
                
                // Configure socket
                clientSocket.setSoTimeout(SO_TIMEOUT);
                clientSocket.setTcpNoDelay(true);
                
                // Update connection
                currentConnection.setSocket(clientSocket);
                currentConnection.setStatus(PeerConnection.ConnectionStatus.CONNECTED);
                
                updateStatus("Peer connected from " + clientSocket.getInetAddress().getHostAddress());
                
                // Handle the connection
                handleConnection(clientSocket);
                
            } catch (SocketTimeoutException e) {
                // Normal timeout, continue listening
                continue;
            } catch (Exception e) {
                if (isRunning) {
                    handleError(new Exception("Error accepting connection: " + e.getMessage(), e));
                }
                break;
            }
        }
    }
    
    /**
     * Handles an established connection.
     * 
     * @param socket Connected socket
     */
    private void handleConnection(Socket socket) {
        try (InputStream in = socket.getInputStream()) {
            
            byte[] buffer = new byte[8192];
            
            while (isRunning && socket.isConnected() && !socket.isClosed()) {
                try {
                    // Read message type (3 bytes)
                    byte[] typeBytes = new byte[3];
                    int bytesRead = readFully(in, typeBytes);
                    if (bytesRead != 3) {
                        break; // Connection closed
                    }
                    
                    String messageType = new String(typeBytes);
                    
                    // Read data length (4 bytes)
                    byte[] lengthBytes = new byte[4];
                    bytesRead = readFully(in, lengthBytes);
                    if (bytesRead != 4) {
                        break; // Connection closed
                    }
                    
                    int dataLength = ((lengthBytes[0] & 0xFF) << 24) |
                                   ((lengthBytes[1] & 0xFF) << 16) |
                                   ((lengthBytes[2] & 0xFF) << 8) |
                                   (lengthBytes[3] & 0xFF);
                    
                    if (dataLength < 0 || dataLength > 10 * 1024 * 1024) { // 10MB limit
                        throw new IOException("Invalid data length: " + dataLength);
                    }
                    
                    // Read data
                    byte[] data = new byte[dataLength];
                    bytesRead = readFully(in, data);
                    if (bytesRead != dataLength) {
                        break; // Connection closed
                    }
                    
                    currentConnection.updateLastActivity();
                    
                    // Process based on message type
                    if ("IMG".equals(messageType)) {
                        // Handle image
                        ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        BufferedImage image = ImageIO.read(bais);
                        if (image != null && imageHandler != null) {
                            Platform.runLater(() -> imageHandler.accept(image));
                        }
                        updateStatus("Image received (" + dataLength + " bytes)");
                        
                    } else if ("TXT".equals(messageType)) {
                        // Handle text message
                        String message = new String(data, "UTF-8");
                        if (messageHandler != null) {
                            Platform.runLater(() -> messageHandler.accept(message));
                        }
                        updateStatus("Message received (" + dataLength + " bytes)");
                        
                    } else {
                        System.err.println("Unknown message type: " + messageType);
                    }
                    
                } catch (SocketTimeoutException e) {
                    // Check if connection is still alive
                    if (!socket.isConnected() || socket.isClosed()) {
                        break;
                    }
                    // Continue reading
                }
            }
            
        } catch (Exception e) {
            if (isRunning) {
                handleError(new Exception("Connection error: " + e.getMessage(), e));
            }
        } finally {
            // Connection closed
            if (currentConnection != null) {
                currentConnection.setStatus(PeerConnection.ConnectionStatus.DISCONNECTED);
                updateStatus("Connection closed");
            }
        }
    }
    
    /**
     * Reads exactly the specified number of bytes from the input stream.
     * 
     * @param in Input stream
     * @param buffer Buffer to read into
     * @return Number of bytes read, or -1 if end of stream
     * @throws IOException if read fails
     */
    private int readFully(InputStream in, byte[] buffer) throws IOException {
        int totalRead = 0;
        int bytesToRead = buffer.length;
        
        while (totalRead < bytesToRead) {
            int bytesRead = in.read(buffer, totalRead, bytesToRead - totalRead);
            if (bytesRead == -1) {
                return totalRead > 0 ? totalRead : -1;
            }
            totalRead += bytesRead;
        }
        
        return totalRead;
    }
    
    /**
     * Updates status through the status handler.
     * 
     * @param status Status message
     */
    private void updateStatus(String status) {
        if (statusHandler != null) {
            Platform.runLater(() -> statusHandler.accept(status));
        }
    }
    
    /**
     * Handles errors through the error handler.
     * 
     * @param error Error to handle
     */
    private void handleError(Exception error) {
        if (errorHandler != null) {
            Platform.runLater(() -> errorHandler.accept(error));
        }
    }
}