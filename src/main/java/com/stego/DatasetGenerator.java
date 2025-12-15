package com.stego;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DatasetGenerator {

    // --- CONFIGURATION ---
    // Update path if needed, but it looked correct in your logs
    static final String RAW_DIR = "C:\\Users\\Nischal\\Desktop\\Steganography-App-main\\images";
    static final String OUTPUT_BASE = "C:\\Users\\Nischal\\Desktop\\Steganography-App-main\\dataset";
    static final String COVER_DIR = OUTPUT_BASE + "\\cover";
    static final String STEGO_DIR = OUTPUT_BASE + "\\stego";

    public static void main(String[] args) {
        File folder = new File(RAW_DIR);
        
        if (!folder.exists()) {
            System.err.println("❌ ERROR: Source folder not found: " + RAW_DIR);
            return;
        }

        File[] listOfFiles = folder.listFiles();
        new File(COVER_DIR).mkdirs();
        new File(STEGO_DIR).mkdirs();

        int count = 0;
        System.out.println("--- Starting DCT Dataset Generation (Fixed Message Length) ---");

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                String name = file.getName().toLowerCase();
                if (file.isFile() && (name.endsWith(".png") || name.endsWith(".jpg"))) {
                    try {
                        String cleanName = "img_" + count + ".png";
                        String stegoName = "stego_" + count + ".png";

                        File coverOut = new File(COVER_DIR, cleanName);
                        File stegoOut = new File(STEGO_DIR, stegoName);

                        Files.copy(file.toPath(), coverOut.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        // --- THE FIX ---
                        // Previous message was ~28 bytes. Capacity is 32 bytes.
                        // New message is ~5 bytes. Fits easily.
                        String secretMsg = "AI_" + count; 
                        
                        ImageStego.encode(coverOut.getPath(), stegoOut.getPath(), secretMsg);

                        if (count % 50 == 0) System.out.println("Processed " + count + " pairs...");
                        count++;

                    } catch (Exception e) {
                        System.err.println("⚠️ Skipped " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("\n✅ SUCCESS! Dataset Created.");
        System.out.println("Total Pairs: " + count);
    }
}