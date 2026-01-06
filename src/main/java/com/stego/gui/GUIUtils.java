package com.stego.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Common GUI utilities and styling components for the steganographic chat system.
 * Provides consistent UI elements and helper methods across both applications.
 */
public class GUIUtils {
    
    // Application styling constants
    public static final String PRIMARY_COLOR = "#2196F3";
    public static final String SECONDARY_COLOR = "#FFC107";
    public static final String SUCCESS_COLOR = "#4CAF50";
    public static final String ERROR_COLOR = "#F44336";
    public static final String WARNING_COLOR = "#FF9800";
    
    // Common CSS styles
    public static final String BUTTON_STYLE = 
        "-fx-background-color: " + PRIMARY_COLOR + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10px 20px; " +
        "-fx-background-radius: 5px;";
    
    public static final String SUCCESS_BUTTON_STYLE = 
        "-fx-background-color: " + SUCCESS_COLOR + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10px 20px; " +
        "-fx-background-radius: 5px;";
    
    public static final String ERROR_BUTTON_STYLE = 
        "-fx-background-color: " + ERROR_COLOR + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10px 20px; " +
        "-fx-background-radius: 5px;";

    /**
     * Creates a styled button with consistent appearance.
     * 
     * @param text Button text
     * @return Styled button
     */
    public static Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle(BUTTON_STYLE);
        
        // Add hover effects
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_STYLE + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_STYLE));
        
        return button;
    }
    
    /**
     * Creates a success-styled button.
     * 
     * @param text Button text
     * @return Success-styled button
     */
    public static Button createSuccessButton(String text) {
        Button button = new Button(text);
        button.setStyle(SUCCESS_BUTTON_STYLE);
        
        button.setOnMouseEntered(e -> button.setStyle(SUCCESS_BUTTON_STYLE + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(SUCCESS_BUTTON_STYLE));
        
        return button;
    }
    
    /**
     * Creates an error-styled button.
     * 
     * @param text Button text
     * @return Error-styled button
     */
    public static Button createErrorButton(String text) {
        Button button = new Button(text);
        button.setStyle(ERROR_BUTTON_STYLE);
        
        button.setOnMouseEntered(e -> button.setStyle(ERROR_BUTTON_STYLE + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(ERROR_BUTTON_STYLE));
        
        return button;
    }

    /**
     * Shows an error dialog with the specified message.
     * 
     * @param title Dialog title
     * @param message Error message
     */
    public static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shows an information dialog with the specified message.
     * 
     * @param title Dialog title
     * @param message Information message
     */
    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shows a warning dialog with the specified message.
     * 
     * @param title Dialog title
     * @param message Warning message
     */
    public static void showWarningDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Creates a progress indicator for long-running operations.
     * 
     * @return Progress indicator
     */
    public static ProgressIndicator createProgressIndicator() {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(50, 50);
        return progress;
    }

    /**
     * Scales a BufferedImage for display in the GUI while maintaining aspect ratio.
     * 
     * @param image Original image
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Scaled JavaFX Image
     */
    public static Image scaleImageForDisplay(BufferedImage image, int maxWidth, int maxHeight) {
        try {
            // Calculate scaling factor to maintain aspect ratio
            double scaleX = (double) maxWidth / image.getWidth();
            double scaleY = (double) maxHeight / image.getHeight();
            double scale = Math.min(scaleX, scaleY);
            
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            
            // Convert BufferedImage to JavaFX Image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            
            return new Image(bais, newWidth, newHeight, true, true);
        } catch (Exception e) {
            System.err.println("Error scaling image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates an ImageView with the scaled image.
     * 
     * @param image BufferedImage to display
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return ImageView with scaled image
     */
    public static ImageView createScaledImageView(BufferedImage image, int maxWidth, int maxHeight) {
        Image scaledImage = scaleImageForDisplay(image, maxWidth, maxHeight);
        if (scaledImage != null) {
            return new ImageView(scaledImage);
        }
        return new ImageView();
    }

    /**
     * Creates a file chooser for selecting image files.
     * 
     * @param title Dialog title
     * @return Configured FileChooser
     */
    public static FileChooser createImageFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
            new FileChooser.ExtensionFilter("PNG Files", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Files", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        return fileChooser;
    }
    
    /**
     * Creates a file chooser for selecting key files.
     * 
     * @param title Dialog title
     * @return Configured FileChooser
     */
    public static FileChooser createKeyFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Key Files", "*.enc"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        return fileChooser;
    }
    
    /**
     * Validates if a file exists and is readable.
     * 
     * @param file File to validate
     * @return true if file is valid, false otherwise
     */
    public static boolean isValidFile(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }
    
    /**
     * Gets the file extension from a filename.
     * 
     * @param filename Filename
     * @return File extension (without dot) or empty string if no extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Checks if a file is a supported image format.
     * 
     * @param file File to check
     * @return true if supported image format, false otherwise
     */
    public static boolean isSupportedImageFormat(File file) {
        if (!isValidFile(file)) {
            return false;
        }
        
        String extension = getFileExtension(file.getName());
        return extension.equals("png") || extension.equals("jpg") || 
               extension.equals("jpeg") || extension.equals("bmp") || 
               extension.equals("gif");
    }
}