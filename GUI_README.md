# P2P Steganographic Chat System - GUI Applications

This document describes the new GUI applications for the secure image steganography system.

## Applications

### 1. Key Generator GUI (`stego-key-generator.jar`)

A standalone application for generating and managing cryptographic keys.

**Features:**
- Generate Kyber post-quantum cryptographic key pairs
- Create AES and Vigenère session keys
- Load and validate existing key files
- Save keys to `.enc` files for sharing
- Copy key information to clipboard

## Quick Start Guide

### 1. Generate Keys
```bash
# Start the Key Generator GUI
mvn exec:java -Dexec.mainClass="com.stego.gui.KeyGeneratorMain"
```
- Click "Generate Keys" to create new cryptographic keys
- Click "Save Keys" to save as `keys.enc` file
- Share this file with your communication partner

### 2. Start P2P Chat
```bash
# Start the P2P Chat GUI
mvn exec:java -Dexec.mainClass="com.stego.gui.P2PChatMain"
```
- Click "Load Keys" and select your `keys.enc` file
- Either "Host Chat" (enter port) or "Connect to Peer" (enter IP:port)
- Type messages, select cover images, and send encrypted steganographic messages

### Alternative: Using JAR files with JavaFX module path
If you have JavaFX SDK installed separately:
```bash
# Download JavaFX SDK from https://openjfx.io/
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/stego-key-generator.jar
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/stego-p2p-chat.jar
```

## Security Features

- **Post-Quantum Cryptography**: Uses Kyber768 algorithm (NIST Level 3 security)
- **Entropy-Based Encryption**: Automatically selects AES-256 or Vigenère based on message entropy
- **DCT Steganography**: Robust image-based message hiding using Discrete Cosine Transform
- **Split-Payload**: Distributes encrypted data across RGB channels for fault tolerance
- **Texture-Adaptive**: Skips smooth image regions to avoid visible artifacts

## Network Protocol

The P2P chat uses a simple TCP-based protocol:
- **Message Format**: `[TYPE][LENGTH][DATA]`
- **Types**: `IMG` for images, `TXT` for text
- **Port Range**: 1024-65535 (user configurable)
- **Connection**: Direct peer-to-peer, no central server

## File Formats

### Keys File (`.enc`)
Contains serialized cryptographic keys:
- Kyber public/private key pair
- AES session key
- Vigenère cipher key
- Algorithm identifier and timestamp

### Steganographic Images
Standard PNG images containing hidden encrypted messages:
- Uses existing DCT-based steganography algorithm
- Maintains image quality and visual appearance
- Supports capacity validation before embedding

## System Requirements

- **Java**: Version 21 (LTS) or higher
- **Memory**: Minimum 512MB RAM
- **Network**: TCP connectivity for P2P communication
- **Display**: GUI requires graphical environment

## Troubleshooting

### Key Generation Issues
- Ensure sufficient entropy for key generation
- Check file permissions for saving keys
- Verify PQC algorithm configuration in `config.properties`

### Connection Problems
- Check firewall settings for the specified port
- Ensure both peers are using the same key file
- Verify network connectivity between peers

### Message Sending Failures
- Confirm image capacity is sufficient for message length
- Check that cover image is a supported format (PNG, JPG)
- Verify cryptographic keys are properly loaded

## Development

The GUI applications are built using JavaFX and integrate with the existing steganography core:

- **GUI Framework**: JavaFX 21
- **Networking**: Java NIO with CompletableFuture
- **Cryptography**: BouncyCastle PQC provider
- **Image Processing**: Java BufferedImage and ImageIO

## Building and Running

```bash
# Compile all applications
mvn clean compile

# Build executable JARs
mvn clean package -DskipTests

# Run Key Generator (recommended method)
mvn exec:java -Dexec.mainClass="com.stego.gui.KeyGeneratorMain"

# Run P2P Chat (recommended method)
mvn exec:java -Dexec.mainClass="com.stego.gui.P2PChatMain"
```

### JavaFX Runtime Issue Solution

The JavaFX runtime components issue when running JAR files directly is solved by using Maven exec plugin:

**✅ Working Method:**
```bash
mvn exec:java -Dexec.mainClass="com.stego.gui.KeyGeneratorMain"
mvn exec:java -Dexec.mainClass="com.stego.gui.P2PChatMain"
```

**Alternative (if you have JavaFX SDK):**
```bash
# Download JavaFX SDK from https://openjfx.io/
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/stego-key-generator.jar
```

## Legacy Command Line Tools

The original command-line tools are still available:

- `stego-encoder.jar`: Command-line encryption and embedding
- `stego-decoder.jar`: Command-line extraction and decryption

These can be used for automated or scripted operations.