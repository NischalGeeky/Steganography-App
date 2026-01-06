package com.stego.gui;

import com.stego.gui.model.ChatMessage;
import com.stego.gui.model.KeysData;
import com.stego.gui.model.PeerConnection;
import com.stego.gui.network.P2PNetworkManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * GUI application for peer-to-peer steganographic chat.
 * Provides secure messaging through steganographic images.
 */
public class P2PChatGUI extends Application {
    
    private Stage primaryStage;
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Button connectButton;
    private Button hostButton;
    private Button loadKeysButton;
    private Label statusLabel;
    private Label connectionLabel;
    
    // Core components
    private ChatMessageHandler messageHandler;
    private P2PNetworkManager networkManager;
    private KeysData currentKeys;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize core components
        initializeComponents();
        
        primaryStage.setTitle("P2P Steganographic Chat");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Create the main layout
        BorderPane root = createMainLayout();
        
        // Create and set the scene
        Scene scene = new Scene(root, 900, 700);
        primaryStage.setScene(scene);
        
        // Handle window closing
        primaryStage.setOnCloseRequest(e -> {
            if (networkManager != null) {
                networkManager.shutdown();
            }
        });
        
        // Show the window
        primaryStage.show();
        
        // Initialize UI state
        updateUIState();
    }
    
    /**
     * Initializes the core components.
     */
    private void initializeComponents() {
        // Initialize message handler
        messageHandler = new ChatMessageHandler();
        messageHandler.setMessageAddedHandler(this::onMessageAdded);
        messageHandler.setStatusHandler(this::updateStatus);
        messageHandler.setErrorHandler(this::handleError);
        
        // Initialize network manager
        networkManager = new P2PNetworkManager();
        networkManager.setMessageHandler(this::onTextMessageReceived);
        networkManager.setImageHandler(this::onImageReceived);
        networkManager.setStatusHandler(this::updateNetworkStatus);
        networkManager.setErrorHandler(this::handleNetworkError);
    }
    
    /**
     * Creates the main layout for the chat interface.
     * 
     * @return Main layout BorderPane
     */
    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        
        // Top panel with connection controls
        VBox topPanel = createTopPanel();
        root.setTop(topPanel);
        
        // Center panel with chat area
        VBox centerPanel = createCenterPanel();
        root.setCenter(centerPanel);
        
        // Bottom panel with message input
        HBox bottomPanel = createBottomPanel();
        root.setBottom(bottomPanel);
        
        return root;
    }
    
    /**
     * Creates the top panel with connection controls.
     * 
     * @return Top panel VBox
     */
    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(15));
        topPanel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        
        // Title
        Label titleLabel = new Label("P2P Steganographic Chat");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + GUIUtils.PRIMARY_COLOR + ";");
        
        // Connection controls
        HBox connectionPanel = createConnectionPanel();
        
        // Status labels
        connectionLabel = new Label("Not connected");
        connectionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        statusLabel = new Label("Load keys to begin");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        topPanel.getChildren().addAll(titleLabel, connectionPanel, connectionLabel, statusLabel);
        
        return topPanel;
    }
    
    /**
     * Creates the connection control panel.
     * 
     * @return Connection panel HBox
     */
    private HBox createConnectionPanel() {
        HBox connectionPanel = new HBox(15);
        connectionPanel.setAlignment(Pos.CENTER_LEFT);
        
        // Load Keys button
        loadKeysButton = GUIUtils.createStyledButton("Load Keys");
        loadKeysButton.setOnAction(e -> handleLoadKeys());
        
        // Host button
        hostButton = GUIUtils.createStyledButton("Host Chat");
        hostButton.setOnAction(e -> handleHostChat());
        hostButton.setDisable(true);
        
        // Connect button
        connectButton = GUIUtils.createStyledButton("Connect to Peer");
        connectButton.setOnAction(e -> handleConnectToPeer());
        connectButton.setDisable(true);
        
        connectionPanel.getChildren().addAll(loadKeysButton, new Separator(), hostButton, connectButton);
        
        return connectionPanel;
    }
    
    /**
     * Creates the center panel with chat display.
     * 
     * @return Center panel VBox
     */
    private VBox createCenterPanel() {
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(15));
        
        // Chat area
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefRowCount(20);
        chatArea.setPromptText("Chat messages will appear here...");
        chatArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        chatArea.setWrapText(true);
        
        VBox.setVgrow(chatArea, Priority.ALWAYS);
        
        centerPanel.getChildren().add(chatArea);
        
        return centerPanel;
    }
    
    /**
     * Creates the bottom panel with message input.
     * 
     * @return Bottom panel HBox
     */
    private HBox createBottomPanel() {
        HBox bottomPanel = new HBox(10);
        bottomPanel.setPadding(new Insets(15));
        bottomPanel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        
        // Message input field
        messageField = new TextField();
        messageField.setPromptText("Type your message here...");
        messageField.setPrefWidth(400);
        messageField.setDisable(true);
        messageField.setOnAction(e -> handleSendMessage());
        
        // Send button
        sendButton = GUIUtils.createSuccessButton("Send Message");
        sendButton.setOnAction(e -> handleSendMessage());
        sendButton.setDisable(true);
        
        // Image button
        Button imageButton = GUIUtils.createStyledButton("Send Image");
        imageButton.setOnAction(e -> handleSendImage());
        imageButton.setDisable(true);
        
        HBox.setHgrow(messageField, Priority.ALWAYS);
        
        bottomPanel.getChildren().addAll(messageField, sendButton, imageButton);
        
        return bottomPanel;
    }
    
    /**
     * Handles the load keys action.
     */
    private void handleLoadKeys() {
        try {
            updateStatus("Selecting key file...");
            
            // Show file chooser for keys.enc files
            javafx.stage.FileChooser fileChooser = GUIUtils.createKeyFileChooser("Load Keys File");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            
            if (selectedFile == null) {
                updateStatus("No file selected");
                return;
            }
            
            updateStatus("Loading keys...");
            
            // Load keys using KeyManager
            KeysData loadedKeys = KeyManager.loadKeys(selectedFile);
            currentKeys = loadedKeys;
            
            // Initialize message handler with keys
            messageHandler.initialize(loadedKeys);
            
            updateStatus("Keys loaded successfully: " + loadedKeys.getAlgorithm());
            messageHandler.addSystemMessage("Cryptographic keys loaded successfully. Algorithm: " + loadedKeys.getAlgorithm());
            
            updateUIState();
            
        } catch (Exception e) {
            handleError(new Exception("Failed to load keys: " + e.getMessage(), e));
        }
    }
    
    /**
     * Handles the host chat action.
     */
    private void handleHostChat() {
        try {
            if (!messageHandler.isInitialized()) {
                GUIUtils.showWarningDialog("Host Chat", "Please load cryptographic keys first.");
                return;
            }
            
            // Show port input dialog
            TextInputDialog portDialog = new TextInputDialog("8080");
            portDialog.setTitle("Host Chat");
            portDialog.setHeaderText("Start hosting a chat session");
            portDialog.setContentText("Enter port number:");
            
            portDialog.showAndWait().ifPresent(portStr -> {
                try {
                    int port = Integer.parseInt(portStr);
                    if (port < 1024 || port > 65535) {
                        throw new NumberFormatException("Port must be between 1024 and 65535");
                    }
                    
                    updateStatus("Starting host on port " + port + "...");
                    
                    CompletableFuture<PeerConnection> hostFuture = networkManager.startServer(port);
                    hostFuture.thenAccept(connection -> {
                        Platform.runLater(() -> {
                            connectionLabel.setText("Hosting on port " + port + " - Waiting for peer...");
                            connectionLabel.setStyle("-fx-text-fill: " + GUIUtils.WARNING_COLOR + ";");
                            messageHandler.addSystemMessage("Started hosting on port " + port + ". Waiting for peer connection...");
                            updateUIState();
                        });
                    }).exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            handleError(new Exception("Failed to start hosting: " + throwable.getMessage(), throwable));
                        });
                        return null;
                    });
                    
                } catch (NumberFormatException e) {
                    GUIUtils.showErrorDialog("Invalid Port", "Please enter a valid port number (1024-65535)");
                }
            });
            
        } catch (Exception e) {
            handleError(new Exception("Failed to start hosting: " + e.getMessage(), e));
        }
    }
    
    /**
     * Handles the connect to peer action.
     */
    private void handleConnectToPeer() {
        try {
            if (!messageHandler.isInitialized()) {
                GUIUtils.showWarningDialog("Connect to Peer", "Please load cryptographic keys first.");
                return;
            }
            
            // Show connection dialog
            Dialog<String[]> connectionDialog = new Dialog<>();
            connectionDialog.setTitle("Connect to Peer");
            connectionDialog.setHeaderText("Enter peer connection details");
            
            // Create dialog content
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            
            TextField addressField = new TextField("localhost");
            TextField portField = new TextField("8080");
            
            grid.add(new Label("IP Address:"), 0, 0);
            grid.add(addressField, 1, 0);
            grid.add(new Label("Port:"), 0, 1);
            grid.add(portField, 1, 1);
            
            connectionDialog.getDialogPane().setContent(grid);
            
            ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
            connectionDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);
            
            connectionDialog.setResultConverter(dialogButton -> {
                if (dialogButton == connectButtonType) {
                    return new String[]{addressField.getText(), portField.getText()};
                }
                return null;
            });
            
            connectionDialog.showAndWait().ifPresent(result -> {
                try {
                    String address = result[0].trim();
                    int port = Integer.parseInt(result[1].trim());
                    
                    if (address.isEmpty()) {
                        throw new IllegalArgumentException("IP address cannot be empty");
                    }
                    if (port < 1024 || port > 65535) {
                        throw new NumberFormatException("Port must be between 1024 and 65535");
                    }
                    
                    updateStatus("Connecting to " + address + ":" + port + "...");
                    
                    CompletableFuture<PeerConnection> connectFuture = networkManager.connectToServer(address, port);
                    connectFuture.thenAccept(connection -> {
                        Platform.runLater(() -> {
                            connectionLabel.setText("Connected to " + address + ":" + port);
                            connectionLabel.setStyle("-fx-text-fill: " + GUIUtils.SUCCESS_COLOR + ";");
                            messageHandler.addSystemMessage("Connected to peer at " + address + ":" + port);
                            updateUIState();
                        });
                    }).exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            handleError(new Exception("Failed to connect: " + throwable.getMessage(), throwable));
                        });
                        return null;
                    });
                    
                } catch (Exception e) {
                    GUIUtils.showErrorDialog("Connection Error", "Invalid connection details: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            handleError(new Exception("Failed to connect to peer: " + e.getMessage(), e));
        }
    }
    
    /**
     * Handles sending a text message.
     */
    private void handleSendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (!networkManager.isConnected()) {
            GUIUtils.showWarningDialog("Send Message", "Not connected to any peer. Please host or connect first.");
            return;
        }
        
        try {
            updateStatus("Sending message...");
            
            // Show cover image selection dialog
            javafx.stage.FileChooser fileChooser = GUIUtils.createImageFileChooser("Select Cover Image");
            File coverImage = fileChooser.showOpenDialog(primaryStage);
            
            if (coverImage == null) {
                updateStatus("Message sending cancelled - no cover image selected");
                return;
            }
            
            // Process message in background
            CompletableFuture.runAsync(() -> {
                try {
                    // Process outgoing message (encrypt and embed)
                    BufferedImage stegoImage = messageHandler.processOutgoingMessage(message, coverImage);
                    
                    // Send the steganographic image
                    networkManager.sendImage(stegoImage).thenRun(() -> {
                        Platform.runLater(() -> {
                            messageField.clear();
                            updateStatus("Message sent successfully");
                        });
                    }).exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            handleError(new Exception("Failed to send image: " + throwable.getMessage(), throwable));
                        });
                        return null;
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleError(new Exception("Failed to process message: " + e.getMessage(), e));
                    });
                }
            });
            
        } catch (Exception e) {
            handleError(new Exception("Failed to send message: " + e.getMessage(), e));
        }
    }
    
    /**
     * Handles sending an image message.
     */
    private void handleSendImage() {
        if (!networkManager.isConnected()) {
            GUIUtils.showWarningDialog("Send Image", "Not connected to any peer. Please host or connect first.");
            return;
        }
        
        try {
            // Show image selection dialog
            javafx.stage.FileChooser fileChooser = GUIUtils.createImageFileChooser("Select Image to Send");
            File imageFile = fileChooser.showOpenDialog(primaryStage);
            
            if (imageFile == null) {
                return;
            }
            
            updateStatus("Sending image...");
            
            // Load and send image
            CompletableFuture.runAsync(() -> {
                try {
                    BufferedImage image = javax.imageio.ImageIO.read(imageFile);
                    if (image == null) {
                        throw new Exception("Unable to read image file");
                    }
                    
                    // Add to message history
                    Platform.runLater(() -> {
                        ChatMessage imageMessage = new ChatMessage(image, "self", 
                                                                 ChatMessage.MessageType.IMAGE, false, true);
                        messageHandler.addMessageToHistory(imageMessage);
                    });
                    
                    // Send the image
                    networkManager.sendImage(image).thenRun(() -> {
                        Platform.runLater(() -> {
                            updateStatus("Image sent successfully");
                        });
                    }).exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            handleError(new Exception("Failed to send image: " + throwable.getMessage(), throwable));
                        });
                        return null;
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        handleError(new Exception("Failed to process image: " + e.getMessage(), e));
                    });
                }
            });
            
        } catch (Exception e) {
            handleError(new Exception("Failed to send image: " + e.getMessage(), e));
        }
    }
    
    /**
     * Called when a new message is added to the chat history.
     * 
     * @param message Added message
     */
    private void onMessageAdded(ChatMessage message) {
        Platform.runLater(() -> {
            String displayText = message.getMessageSummary() + "\n";
            chatArea.appendText(displayText);
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * Called when a text message is received from the network.
     * 
     * @param message Received text message
     */
    private void onTextMessageReceived(String message) {
        // Add as regular text message
        ChatMessage textMessage = new ChatMessage(message, "peer", false, false);
        messageHandler.addMessageToHistory(textMessage);
    }
    
    /**
     * Called when an image is received from the network.
     * 
     * @param image Received image
     */
    private void onImageReceived(BufferedImage image) {
        try {
            // Try to process as steganographic image
            String extractedMessage = messageHandler.processIncomingMessage(image);
            
            if (extractedMessage != null) {
                updateStatus("Steganographic message received and decrypted");
            } else {
                updateStatus("Regular image received");
            }
            
        } catch (Exception e) {
            handleError(new Exception("Failed to process received image: " + e.getMessage(), e));
        }
    }
    
    /**
     * Updates the network status.
     * 
     * @param status Status message
     */
    private void updateNetworkStatus(String status) {
        Platform.runLater(() -> {
            if (status.contains("connected") || status.contains("Connected")) {
                connectionLabel.setText(status);
                connectionLabel.setStyle("-fx-text-fill: " + GUIUtils.SUCCESS_COLOR + ";");
                updateUIState();
            } else if (status.contains("closed") || status.contains("disconnected")) {
                connectionLabel.setText("Disconnected");
                connectionLabel.setStyle("-fx-text-fill: " + GUIUtils.ERROR_COLOR + ";");
                updateUIState();
            }
        });
    }
    
    /**
     * Handles network errors.
     * 
     * @param error Network error
     */
    private void handleNetworkError(Exception error) {
        Platform.runLater(() -> {
            handleError(error);
        });
    }
    
    /**
     * Updates the general status.
     * 
     * @param status Status message
     */
    private void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            statusLabel.setStyle("-fx-text-fill: gray;");
        });
    }
    
    /**
     * Updates the UI state based on current conditions.
     */
    private void updateUIState() {
        boolean hasKeys = messageHandler != null && messageHandler.isInitialized();
        boolean isConnected = networkManager != null && networkManager.isConnected();
        
        // Enable/disable connection buttons based on key availability
        hostButton.setDisable(!hasKeys);
        connectButton.setDisable(!hasKeys);
        
        // Enable/disable messaging controls based on connection status
        messageField.setDisable(!isConnected);
        sendButton.setDisable(!isConnected);
        
        // Update button tooltips
        if (!hasKeys) {
            hostButton.setTooltip(new Tooltip("Load cryptographic keys first"));
            connectButton.setTooltip(new Tooltip("Load cryptographic keys first"));
        } else {
            hostButton.setTooltip(new Tooltip("Start hosting a chat session"));
            connectButton.setTooltip(new Tooltip("Connect to a peer"));
        }
        
        if (!isConnected) {
            sendButton.setTooltip(new Tooltip("Connect to a peer first"));
            messageField.setTooltip(new Tooltip("Connect to a peer to start messaging"));
        } else {
            sendButton.setTooltip(new Tooltip("Send encrypted steganographic message"));
            messageField.setTooltip(new Tooltip("Type your message here"));
        }
    }
    
    /**
     * Handles errors by showing error dialog and updating status.
     * 
     * @param error Error to handle
     */
    private void handleError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unknown error occurred";
        }
        
        GUIUtils.showErrorDialog("Error", message);
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: " + GUIUtils.ERROR_COLOR + ";");
        
        // Log error for debugging
        System.err.println("P2P Chat Error: " + message);
        if (error.getCause() != null) {
            error.getCause().printStackTrace();
        }
    }
    
    /**
     * Main method for standalone execution.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}