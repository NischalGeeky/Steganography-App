package com.stego.gui;

import com.stego.gui.model.KeysData;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;

/**
 * GUI application for generating and managing cryptographic keys.
 * This standalone application creates keys.enc files for secure key exchange.
 */
public class KeyGeneratorGUI extends Application {
    
    private Stage primaryStage;
    private TextArea keyInfoArea;
    private Button generateButton;
    private Button loadButton;
    private Button saveButton;
    private Button copyButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private KeysData currentKeys;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        primaryStage.setTitle("Steganographic Key Generator");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        
        // Create the main layout
        VBox root = createMainLayout();
        
        // Create and set the scene
        Scene scene = new Scene(root, 700, 600);
        primaryStage.setScene(scene);
        
        // Show the window
        primaryStage.show();
        
        // Initialize UI state
        updateUIState();
    }
    
    /**
     * Creates the main layout for the key generator interface.
     * 
     * @return Main layout VBox
     */
    private VBox createMainLayout() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        
        // Title
        Label titleLabel = new Label("Steganographic Key Generator");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GUIUtils.PRIMARY_COLOR + ";");
        
        // Description
        Label descLabel = new Label("Generate and manage cryptographic keys for secure steganographic communication");
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
        descLabel.setWrapText(true);
        
        // Key information display area
        keyInfoArea = new TextArea();
        keyInfoArea.setEditable(false);
        keyInfoArea.setPrefRowCount(15);
        keyInfoArea.setPromptText("Key information will be displayed here...");
        keyInfoArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        
        // Button panel
        HBox buttonPanel = createButtonPanel();
        
        // Status and progress panel
        HBox statusPanel = new HBox(10);
        statusPanel.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Ready to generate keys");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        progressIndicator = GUIUtils.createProgressIndicator();
        progressIndicator.setVisible(false);
        
        statusPanel.getChildren().addAll(statusLabel, progressIndicator);
        
        // Add all components to root
        root.getChildren().addAll(
            titleLabel,
            descLabel,
            new Separator(),
            keyInfoArea,
            buttonPanel,
            statusPanel
        );
        
        return root;
    }
    
    /**
     * Creates the button panel with key management actions.
     * 
     * @return Button panel HBox
     */
    private HBox createButtonPanel() {
        HBox buttonPanel = new HBox(15);
        buttonPanel.setAlignment(Pos.CENTER);
        
        // Generate Keys button
        generateButton = GUIUtils.createStyledButton("Generate Keys");
        generateButton.setOnAction(e -> handleGenerateKeys());
        
        // Load Keys button
        loadButton = GUIUtils.createStyledButton("Load Keys");
        loadButton.setOnAction(e -> handleLoadKeys());
        
        // Save Keys button
        saveButton = GUIUtils.createStyledButton("Save Keys");
        saveButton.setOnAction(e -> handleSaveKeys());
        saveButton.setDisable(true);
        
        // Copy Keys button
        copyButton = GUIUtils.createStyledButton("Copy to Clipboard");
        copyButton.setOnAction(e -> handleCopyKeys());
        copyButton.setDisable(true);
        
        buttonPanel.getChildren().addAll(generateButton, loadButton, saveButton, copyButton);
        
        return buttonPanel;
    }
    
    /**
     * Handles the generate keys action.
     */
    private void handleGenerateKeys() {
        // Create background task for key generation
        Task<KeysData> keyGenerationTask = new Task<KeysData>() {
            @Override
            protected KeysData call() throws Exception {
                updateMessage("Generating Kyber key pair...");
                Thread.sleep(100); // Small delay to show progress
                
                updateMessage("Generating session keys...");
                return KeyManager.generateKeys();
            }
            
            @Override
            protected void succeeded() {
                KeysData keys = getValue();
                currentKeys = keys;
                
                // Display key information with better formatting
                StringBuilder displayText = new StringBuilder();
                displayText.append("=== GENERATED KEYS INFORMATION ===\n\n");
                displayText.append(keys.getKeyInfo()).append("\n\n");
                displayText.append("=== SECURITY INFORMATION ===\n");
                displayText.append("Post-Quantum Algorithm: ").append(keys.getAlgorithm()).append("\n");
                displayText.append("Security Level: NIST Level 3 (equivalent to AES-192)\n");
                displayText.append("Quantum Resistant: Yes\n");
                displayText.append("Key Exchange: Kyber KEM (Key Encapsulation Mechanism)\n\n");
                displayText.append("=== USAGE INSTRUCTIONS ===\n");
                displayText.append("1. Save these keys to a secure location\n");
                displayText.append("2. Share the keys.enc file with your communication partner\n");
                displayText.append("3. Keep the private key secure and never share it publicly\n");
                displayText.append("4. Use these keys in the P2P Chat application for secure messaging");
                
                keyInfoArea.setText(displayText.toString());
                
                statusLabel.setText("Keys generated successfully");
                statusLabel.setStyle("-fx-text-fill: " + GUIUtils.SUCCESS_COLOR + ";");
                progressIndicator.setVisible(false);
                
                updateUIState();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                handleError("Key Generation Error", "Failed to generate keys: " + errorMessage);
                progressIndicator.setVisible(false);
            }
        };
        
        // Bind status label to task message
        statusLabel.textProperty().bind(keyGenerationTask.messageProperty());
        statusLabel.setStyle("-fx-text-fill: " + GUIUtils.WARNING_COLOR + ";");
        
        // Show progress indicator
        progressIndicator.setVisible(true);
        
        // Disable generate button during operation
        generateButton.setDisable(true);
        
        // Re-enable button when task completes
        keyGenerationTask.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            generateButton.setDisable(false);
        });
        keyGenerationTask.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            generateButton.setDisable(false);
        });
        
        // Run task in background thread
        Thread keyGenThread = new Thread(keyGenerationTask);
        keyGenThread.setDaemon(true);
        keyGenThread.start();
    }
    
    /**
     * Handles the load keys action.
     */
    private void handleLoadKeys() {
        try {
            statusLabel.setText("Selecting key file...");
            statusLabel.setStyle("-fx-text-fill: " + GUIUtils.WARNING_COLOR + ";");
            
            // Show file chooser for keys.enc files
            javafx.stage.FileChooser fileChooser = GUIUtils.createKeyFileChooser("Load Keys File");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            
            if (selectedFile == null) {
                statusLabel.setText("No file selected");
                statusLabel.setStyle("-fx-text-fill: gray;");
                return;
            }
            
            statusLabel.setText("Validating key file...");
            statusLabel.setStyle("-fx-text-fill: " + GUIUtils.WARNING_COLOR + ";");
            
            // First validate the file
            KeyManager.ValidationResult validation = KeyManager.validateKeyFile(selectedFile);
            if (!validation.isValid()) {
                throw new Exception("Invalid key file: " + validation.getMessage());
            }
            
            statusLabel.setText("Loading keys...");
            
            // Load keys using KeyManager
            KeysData loadedKeys = KeyManager.loadKeys(selectedFile);
            currentKeys = loadedKeys;
            
            // Display key information
            StringBuilder displayText = new StringBuilder();
            displayText.append("=== LOADED KEYS INFORMATION ===\n\n");
            displayText.append(loadedKeys.getKeyInfo()).append("\n\n");
            displayText.append("=== FILE INFORMATION ===\n");
            displayText.append(KeyManager.getFileInfo(selectedFile)).append("\n\n");
            displayText.append("=== VALIDATION RESULT ===\n");
            displayText.append(validation.toString());
            
            keyInfoArea.setText(displayText.toString());
            
            statusLabel.setText("Keys loaded successfully from: " + selectedFile.getName());
            statusLabel.setStyle("-fx-text-fill: " + GUIUtils.SUCCESS_COLOR + ";");
            
            updateUIState();
            
        } catch (Exception e) {
            handleError("Key Loading Error", "Failed to load keys: " + e.getMessage());
        }
    }
    
    /**
     * Handles the save keys action.
     */
    private void handleSaveKeys() {
        try {
            if (currentKeys == null || !currentKeys.isValid()) {
                GUIUtils.showWarningDialog("Save Keys", "No valid keys to save. Generate or load keys first.");
                return;
            }
            
            statusLabel.setText("Saving keys...");
            statusLabel.setStyle("-fx-text-fill: " + GUIUtils.WARNING_COLOR + ";");
            
            // Show file chooser for saving keys.enc files
            javafx.stage.FileChooser fileChooser = GUIUtils.createKeyFileChooser("Save Keys File");
            fileChooser.setInitialFileName("keys.enc");
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            
            if (selectedFile != null) {
                // Create backup if file exists
                if (selectedFile.exists()) {
                    File backup = KeyManager.createBackup(selectedFile);
                    if (backup != null) {
                        System.out.println("Created backup: " + backup.getAbsolutePath());
                    }
                }
                
                // Save keys using KeyManager
                KeyManager.saveKeys(currentKeys, selectedFile);
                
                statusLabel.setText("Keys saved successfully to: " + selectedFile.getName());
                statusLabel.setStyle("-fx-text-fill: " + GUIUtils.SUCCESS_COLOR + ";");
                
                GUIUtils.showInfoDialog("Save Keys", 
                    "Keys saved successfully!\n\n" +
                    "Location: " + selectedFile.getAbsolutePath() + "\n" +
                    "Size: " + selectedFile.length() + " bytes");
            } else {
                statusLabel.setText("Save cancelled");
                statusLabel.setStyle("-fx-text-fill: gray;");
            }
            
        } catch (Exception e) {
            handleError("Key Saving Error", "Failed to save keys: " + e.getMessage());
        }
    }
    
    /**
     * Handles the copy keys action.
     */
    private void handleCopyKeys() {
        try {
            String keyContent = keyInfoArea.getText();
            if (keyContent == null || keyContent.trim().isEmpty()) {
                GUIUtils.showWarningDialog("Copy Keys", "No key information to copy");
                return;
            }
            
            // Copy to system clipboard
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(keyContent);
            clipboard.setContent(content);
            
            statusLabel.setText("Key information copied to clipboard");
            statusLabel.setStyle("-fx-text-fill: " + GUIUtils.SUCCESS_COLOR + ";");
            
        } catch (Exception e) {
            handleError("Copy Error", "Failed to copy keys: " + e.getMessage());
        }
    }
    
    /**
     * Updates the UI state based on current conditions.
     */
    private void updateUIState() {
        // Enable save and copy buttons if there are valid keys
        boolean hasValidKeys = currentKeys != null && currentKeys.isValid();
        
        saveButton.setDisable(!hasValidKeys);
        copyButton.setDisable(!hasValidKeys);
        
        // Update button text and tooltips based on state
        if (hasValidKeys) {
            saveButton.setText("Save Keys");
            copyButton.setText("Copy to Clipboard");
            saveButton.setTooltip(new Tooltip("Save keys to a .enc file"));
            copyButton.setTooltip(new Tooltip("Copy key information to clipboard"));
        } else {
            saveButton.setText("Save Keys");
            copyButton.setText("Copy to Clipboard");
            saveButton.setTooltip(new Tooltip("Generate or load keys first"));
            copyButton.setTooltip(new Tooltip("Generate or load keys first"));
        }
    }
    
    /**
     * Handles errors by showing error dialog and updating status.
     * 
     * @param title Error dialog title
     * @param message Error message
     */
    private void handleError(String title, String message) {
        GUIUtils.showErrorDialog(title, message);
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: " + GUIUtils.ERROR_COLOR + ";");
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