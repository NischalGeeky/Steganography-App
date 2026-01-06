package com.stego.gui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the Key Generator GUI application.
 * This standalone application creates and manages keys.enc files for secure key exchange.
 */
public class KeyGeneratorMain extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Create and show the Key Generator GUI
            KeyGeneratorGUI keyGenerator = new KeyGeneratorGUI();
            keyGenerator.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start Key Generator GUI: " + e.getMessage());
        }
    }
    
    /**
     * Main method for launching the Key Generator application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to launch Key Generator application: " + e.getMessage());
            System.exit(1);
        }
    }
}