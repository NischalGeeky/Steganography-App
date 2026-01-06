package com.stego.gui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the P2P Chat GUI application.
 * This application provides peer-to-peer steganographic messaging capabilities.
 */
public class P2PChatMain extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Create and show the P2P Chat GUI
            P2PChatGUI chatGUI = new P2PChatGUI();
            chatGUI.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start P2P Chat GUI: " + e.getMessage());
        }
    }
    
    /**
     * Main method for launching the P2P Chat application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to launch P2P Chat application: " + e.getMessage());
            System.exit(1);
        }
    }
}