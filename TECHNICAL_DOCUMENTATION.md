# Steganography App - Complete Technical Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [File Structure & Components](#file-structure--components)
3. [Encryption Process](#encryption-process)
4. [Steganography Algorithm](#steganography-algorithm)
5. [Peer-to-Peer Communication](#peer-to-peer-communication)
6. [GUI Applications](#gui-applications)
7. [Security Features](#security-features)
8. [Build & Deployment](#build--deployment)

---

## System Overview

This is a **Java-based secure steganographic communication system** that combines post-quantum cryptography with image-based message hiding. The system provides both command-line and graphical interfaces for covert communication through digital images.

### Key Technologies
- **Java 21** - Core programming language
- **JavaFX 21** - GUI framework
- **BouncyCastle PQC** - Post-quantum cryptography provider
- **Maven** - Build and dependency management
- **DCT Steganography** - Discrete Cosine Transform message embedding

---

## File Structure & Components

### Core Application Files

#### `/src/main/java/com/stego/Main.java`
**Purpose:** Command-line encoder entry point
**Functionality:**
- Reads message from `message.txt`
- Performs hybrid encryption (AES + Vigenère)
- Generates Kyber lattice key pairs
- Wraps session keys with post-quantum encryption
- Implements split-payload orchestration across RGB channels
- Creates final steganographic image `output.png`

**Key Process Flow:**
```java
1. Read message.txt
2. HybridEncryptor.encryptText() → AES + Vigenère encryption
3. LatticeManager.generateLatticeKeyPair() → Kyber key generation
4. HybridEncryptor.encryptAESKey() → Key wrapping with Kyber
5. Split payload into 3 chunks
6. ImageStego.encode() → Multi-channel DCT embedding
```

#### `/src/main/java/com/stego/ReceiverMain.java`
**Purpose:** Command-line decoder entry point
**Functionality:**
- Extracts hidden messages from steganographic images
- Decrypts using Kyber private keys
- Recovers original plaintext message

#### `/src/main/java/com/stego/ImageStego.java`
**Purpose:** Core steganography engine
**Key Features:**
- **DCT-based embedding** in 8x8 image blocks
- **Multi-channel support** (RED/GREEN/BLUE)
- **Texture-adaptive masking** to avoid visible artifacts
- **Sparse randomized sampling** for security
- **Robust quantization** with persistence factor

**Critical Constants:**
```java
private static final int N = 8; // 8x8 DCT blocks
private static final int PERSISTENCE = 20; // Quantization robustness
private static final double VARIANCE_THRESHOLD = 200.0; // Texture masking
```

**Channel Encoding Strategy:**
- **RED Channel:** Low-frequency components (header data)
- **GREEN Channel:** Mid-frequency components (body data)  
- **BLUE Channel:** High-frequency components (metadata)

### Cryptographic Components

#### `/src/main/java/com/stego/LatticeManager.java`
**Purpose:** Post-quantum key management
**Functionality:**
- Generates Kyber768 key pairs (NIST Level 3 security)
- Serializes/deserializes keys for file storage
- Key format conversion between string and binary representations

#### `/src/main/java/com/stego/HybridEncryptor.java`
**Purpose:** Hybrid encryption orchestration
**Functionality:**
- **Entropy-based cipher selection:** Chooses AES-256 or Vigenère based on message characteristics
- **Key wrapping:** Encrypts session keys with Kyber public keys
- **Combined output:** Creates unified encrypted payload

#### `/src/main/java/com/stego/FileEncryptor.java`
**Purpose:** File-based encryption operations
**Output Class:**
```java
public static class Output {
    private String finalOutput;      // Combined encrypted text
    private String aesKey;           // AES session key
    private String vigenereKey;      // Vigenère cipher key
}
```

#### `/src/main/java/com/stego/FileDecryptor.java`
**Purpose:** File-based decryption operations
**Functionality:**
- Recovers session keys using Kyber private keys
- Decrypts hybrid encrypted payload
- Returns original plaintext message

### GUI Components

#### `/src/main/java/com/stego/gui/P2PChatMain.java`
**Purpose:** P2P Chat application entry point
**Functionality:**
- JavaFX Application launcher
- Initializes P2PChatGUI
- Handles application lifecycle and error management

#### `/src/main/java/com/stego/gui/P2PChatGUI.java`
**Purpose:** Main peer-to-peer chat interface
**Key Features:**
- **Real-time messaging** with steganographic image exchange
- **Connection management:** Host or connect to peers
- **Message composition:** Text input with cover image selection
- **Chat history:** Chronological message display
- **Status indicators:** Connection state and encryption status

**GUI Layout:**
```
┌─────────────────────────────────────────┐
│ Key Management: [Load Keys] [Generate]  │
├─────────────────────────────────────────┤
│ Connection: [Host Chat] [Connect]       │
├─────────────────────────────────────────┤
│ Chat History Display Area              │
├─────────────────────────────────────────┤
│ Message Input: [________________] [Send]│
│ Cover Image: [Browse...] [Preview]     │
└─────────────────────────────────────────┘
```

#### `/src/main/java/com/stego/gui/KeyGeneratorMain.java`
**Purpose:** Key generator application entry point
**Functionality:**
- Launches KeyGeneratorGUI
- Manages cryptographic key creation workflow

#### `/src/main/java/com/stego/gui/KeyGeneratorGUI.java`
**Purpose:** Cryptographic key generation interface
**Features:**
- **Kyber key pair generation** with configurable security levels
- **Session key creation** (AES + Vigenère)
- **Key file management:** Save/load `.enc` files
- **Clipboard integration:** Copy key data
- **Validation:** Key format and integrity checking

#### `/src/main/java/com/stego/gui/KeyManager.java`
**Purpose:** Key validation and management utilities
**ValidationResult Enum:**
```java
public enum ValidationResult {
    VALID,           // Keys are properly formatted
    INVALID_FORMAT,  // File structure is corrupted
    MISSING_KEYS,    // Required key components absent
    DECRYPTION_ERROR // Key decryption failed
}
```

### Network Components

#### `/src/main/java/com/stego/gui/network/P2PNetworkManager.java`
**Purpose:** Peer-to-peer network communication
**Key Features:**
- **TCP socket management** for direct peer connections
- **Asynchronous messaging** with CompletableFuture
- **Protocol handling:** Message type routing and validation
- **Connection state management:** Status tracking and error recovery

**Network Protocol:**
```
Message Format: [TYPE][LENGTH][DATA]
Types: IMG (image), TXT (text)
Port Range: 1024-65535 (user configurable)
```

#### `/src/main/java/com/stego/gui/ChatMessageHandler.java`
**Purpose:** Message processing and routing
**Functionality:**
- **Incoming message processing:** Extract and decrypt hidden data
- **Outgoing message preparation:** Encrypt and embed messages
- **Error handling:** Corrupted data detection and recovery
- **Message validation:** Format and integrity checking

### Data Models

#### `/src/main/java/com/stego/gui/model/ChatMessage.java`
**Purpose:** Chat message data structure
**MessageType Enum:**
```java
public enum MessageType {
    SENT,           // Message sent by current user
    RECEIVED,       // Message from peer
    SYSTEM,         // System notifications
    ERROR           // Error messages
}
```

#### `/src/main/java/com/stego/gui/model/PeerConnection.java`
**Purpose:** Peer connection state management
**ConnectionStatus Enum:**
```java
public enum ConnectionStatus {
    DISCONNECTED,   // No active connection
    CONNECTING,     // Connection in progress
    CONNECTED,      // Active connection
    ERROR           // Connection failed
}
```

#### `/src/main/java/com/stego/gui/model/KeysData.java`
**Purpose:** Cryptographic key container
**Contents:**
- Kyber public/private key pair
- AES session key
- Vigenère cipher key
- Algorithm metadata and timestamps

### Utility Components

#### `/src/main/java/com/stego/gui/GUIUtils.java`
**Purpose:** GUI helper utilities
**Functions:**
- **File dialog management:** Open/save dialogs with filtering
- **Image processing:** Scaling and format conversion
- **Error display:** User-friendly error messages
- **Theme management:** Consistent styling across components

---

## Encryption Process

### Step-by-Step Encryption Workflow

#### 1. Message Preparation
```java
// Read plaintext message
String content = new String(Files.readAllBytes(Paths.get("message.txt")));
```

#### 2. Hybrid Encryption
**Entropy Analysis:**
- System analyzes message characteristics (entropy, patterns, length)
- **High entropy messages** → AES-256 encryption (stronger security)
- **Low entropy messages** → Vigenère cipher (faster processing)

**Encryption Process:**
```java
Output output = FileEncryptor.encryptText(content);
// Returns:
// - finalOutput: Combined encrypted payload
// - aesKey: AES session key (if used)
// - vigenereKey: Vigenère cipher key
```

#### 3. Post-Quantum Key Generation
**Kyber768 Algorithm:**
- NIST Level 3 security standard
- Lattice-based cryptography resistant to quantum attacks
- Key pair generation: `LatticeManager.generateLatticeKeyPair()`

**Key Components:**
```java
KeyPair latticePair = LatticeManager.generateLatticeKeyPair();
PublicKey receiverPub = latticePair.getPublic();   // For encryption
PrivateKey receiverPriv = latticePair.getPrivate(); // For decryption
```

#### 4. Session Key Wrapping
**Key Protection Process:**
```java
// Encrypt session keys with Kyber public key
String secureAES = HybridEncryptor.encryptAESKey(output.getAesKey(), receiverPub);
String secureVigenere = HybridEncryptor.encryptAESKey(output.getVigenereKey(), receiverPub);
```

**Security Benefit:** Even if session keys are compromised, they remain protected by post-quantum encryption.

#### 5. Split-Payload Orchestration
**Channel Distribution Strategy:**
```java
int totalLen = encryptedText.length();
int chunkSize = (totalLen + 2) / 3; // Divide into 3 parts

String chunk1 = encryptedText.substring(0, Math.min(chunkSize, totalLen));     // RED channel
String chunk2 = encryptedText.substring(chunkSize, Math.min(2 * chunkSize, totalLen)); // GREEN channel  
String chunk3 = encryptedText.substring(2 * chunkSize);                        // BLUE channel
```

**Rationale:**
- **Fault tolerance:** Damage to one channel doesn't destroy entire message
- **Capacity optimization:** Different channels have different embedding capacities
- **Security enhancement:** Multi-channel analysis required for detection

---

## Steganography Algorithm

### DCT-Based Message Embedding

#### 1. Image Block Processing
**8x8 Block Division:**
```java
// Divide image into 8x8 pixel blocks
for (int y = 0; y <= height - N; y += N) {
    for (int x = 0; x <= width - N; x += N) {
        // Process each block
    }
}
```

#### 2. Texture-Adaptive Masking
**Variance Calculation:**
```java
private static double getBlockVariance(double[][] dctBlock) {
    // Calculate variance of DCT coefficients (excluding DC component)
    // High variance = textured areas = good for embedding
    // Low variance = smooth areas = skip to avoid artifacts
}
```

**Threshold Application:**
- Blocks with variance ≥ 200.0 are selected for embedding
- Smooth blocks are skipped to prevent visible changes
- Blue channel used for variance calculation (consistency)

#### 3. Sparse Randomized Sampling
**Deterministic Shuffling:**
```java
// Generate seed from Vigenère key
long seed = seedFromKey(vigenereKey);
Random rand = new Random(seed);

// Shuffle block order for security
for (int i = blockCoords.size() - 1; i > 0; i--) {
    int j = rand.nextInt(i + 1);
    // Swap blocks
}
```

**Security Benefits:**
- **Deterministic:** Same key produces same block order (required for decoding)
- **Randomized appearance:** Prevents pattern analysis attacks
- **Key-dependent:** Block order changes with different keys

#### 4. DCT Coefficient Modification
**Robust Quantization:**
```java
private static void embedBitRobust(double[][] dct, int u, int v, int bit) {
    double val = dct[u][v];
    double quantized = Math.round(val / PERSISTENCE);
    int parity = (int) Math.abs(quantized) % 2;
    
    if (parity != bit) {
        // Force correct parity (0 = even, 1 = odd)
        if (val > 0) quantized += 1;
        else quantized -= 1;
    }
    
    dct[u][v] = quantized * PERSISTENCE; // Persistence = 20
}
```

**Coefficient Selection:**
```java
private static final int[] COEFF_X = {3, 4, 3, 4, 2, 5, 2, 5};
private static final int[] COEFF_Y = {3, 3, 4, 4, 2, 2, 5, 5};
// Mid-frequency coefficients for optimal robustness
```

#### 5. Multi-Channel Processing
**Channel-Specific Embedding:**
```java
// Extract specific color channel
private static double[][] getChannelLayer(BufferedImage img, int startX, int startY, String channel) {
    int shift;
    switch (channel.toUpperCase()) {
        case "RED":   shift = 16; break;
        case "GREEN": shift = 8;  break;
        case "BLUE":  
        default:      shift = 0;  break;
    }
    // Extract channel values
}
```

**Channel Reconstruction:**
```java
// Modify specific channel while preserving others
private static void setChannelLayer(BufferedImage img, int startX, int startY, 
                                   double[][] block, String channel) {
    // Update only the target channel
    // Preserve RGB values from other channels
}
```

### Message Extraction Process

#### 1. Block Order Regeneration
- Use same Vigenère key to generate deterministic seed
- Reproduce identical block shuffling sequence
- Ensure synchronization with embedding process

#### 2. Coefficient Reading
```java
private static int extractBitRobust(double[][] dct, int u, int v) {
    double val = dct[u][v];
    double quantized = Math.round(val / PERSISTENCE);
    return (int) Math.abs(quantized) % 2; // Extract parity
}
```

#### 3. Message Reconstruction
- Read 4-byte length header first
- Validate length (prevent crashes on non-steganographic images)
- Extract message bytes according to length
- Return empty string if no valid message found

---

## Peer-to-Peer Communication

### Network Architecture

#### 1. Connection Management
**TCP Socket Implementation:**
```java
// Server mode (hosting chat)
ServerSocket serverSocket = new ServerSocket(port);
Socket clientSocket = serverSocket.accept();

// Client mode (connecting to peer)
Socket socket = new Socket(peerAddress, port);
```

**Connection States:**
```java
public enum ConnectionStatus {
    DISCONNECTED,   // Initial state
    CONNECTING,     // Connection attempt in progress
    CONNECTED,      // Active data transfer
    ERROR           // Connection failed
}
```

#### 2. Message Protocol
**Protocol Format:**
```
[TYPE:3 bytes][LENGTH:4 bytes][DATA:variable bytes]
```

**Message Types:**
- `IMG`: Steganographic image data
- `TXT`: Plain text messages (system notifications)

**Protocol Implementation:**
```java
// Message sending
public void sendMessage(String type, byte[] data) {
    byte[] typeBytes = type.getBytes();
    byte[] lengthBytes = intToBytes(data.length);
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.write(typeBytes);
    outputStream.write(lengthBytes);
    outputStream.write(data);
    
    socket.getOutputStream().write(outputStream.toByteArray());
}
```

#### 3. Asynchronous Message Handling
**CompletableFuture Integration:**
```java
public CompletableFuture<Void> sendMessageAsync(String message) {
    return CompletableFuture.runAsync(() -> {
        try {
            // Process and send message
            processOutgoingMessage(message);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    });
}
```

**Event-Driven Architecture:**
- **Message listeners** for incoming data
- **Status callbacks** for connection state changes
- **Error handlers** for network failures

### Security in Communication

#### 1. End-to-End Encryption
- **Message encryption** occurs before network transmission
- **Steganographic embedding** hides encrypted data in images
- **Key synchronization** ensures both parties can decrypt

#### 2. Authentication
- **Shared key files** (`keys.enc`) provide mutual authentication
- **Key validation** prevents unauthorized connections
- **Algorithm consistency** ensures compatibility

#### 3. Integrity Protection
- **Message length validation** prevents truncation attacks
- **Format checking** ensures data integrity
- **Error detection** identifies corrupted transmissions

---

## GUI Applications

### Key Generator GUI

#### Interface Layout
```
┌─────────────────────────────────────────┐
│ Key Generator - Steganography App      │
├─────────────────────────────────────────┤
│ Algorithm: [Kyber768 ▼]                 │
│ Security Level: [NIST Level 3]         │
├─────────────────────────────────────────┤
│ [Generate Keys]                         │
├─────────────────────────────────────────┤
│ Public Key: [________________] [Copy]  │
│ Private Key: [________________] [Copy]  │
│ AES Key: [________________] [Copy]     │
│ Vigenère Key: [________________] [Copy] │
├─────────────────────────────────────────┤
│ [Save Keys] [Load Keys] [Validate]     │
├─────────────────────────────────────────┤
│ Status: Keys generated successfully ✓   │
└─────────────────────────────────────────┘
```

#### Key Generation Workflow
1. **Algorithm Selection:** Choose PQC algorithm (Kyber768 default)
2. **Key Generation:** Create key pairs and session keys
3. **Display Results:** Show all generated keys
4. **File Operations:** Save to `.enc` format or load existing keys
5. **Validation:** Verify key integrity and format

#### Error Handling
- **Insufficient entropy:** Detect and warn about low entropy conditions
- **File permissions:** Handle save/load permission errors
- **Format validation:** Check key file integrity
- **Algorithm compatibility:** Ensure proper algorithm configuration

### P2P Chat GUI

#### Interface Components
```
┌─────────────────────────────────────────┐
│ P2P Steganographic Chat                 │
├─────────────────────────────────────────┤
│ File: [Load Keys...] [Generate New]     │
│ Status: Keys loaded ✓                   │
├─────────────────────────────────────────┤
│ Connection:                             │
│ ○ Host Chat on Port: [____] [Start]    │
│ ○ Connect to: [IP:Port] [Connect]       │
│ Status: Connected to 192.168.1.100 ✓    │
├─────────────────────────────────────────┤
│ Chat History:                           │
│ ┌─────────────────────────────────────┐ │
│ │ [12:30] You: Hello there!           │ │
│ │ [12:31] Peer: Hi! Image received... │ │
│ │ [12:32] System: Message decrypted ✓ │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ Message: [_________________________]    │
│ Cover Image: [Browse...] [Preview]      │
│ [Send Message] [Clear] [Save Chat]      │
└─────────────────────────────────────────┘
```

#### Chat Operation Flow

**1. Key Loading:**
- User loads `.enc` key file
- System validates key format and integrity
- Cryptographic components initialized

**2. Connection Establishment:**
- **Host Mode:** Create server socket on specified port
- **Client Mode:** Connect to peer's IP address and port
- **Status Updates:** Real-time connection state display

**3. Message Composition:**
- Type plaintext message
- Select cover image file
- System validates image capacity

**4. Message Processing:**
```java
// Automatic background processing
CompletableFuture.runAsync(() -> {
    // 1. Encrypt message with hybrid encryption
    // 2. Split payload across RGB channels
    // 3. Embed in cover image using DCT
    // 4. Send steganographic image to peer
});
```

**5. Message Reception:**
- Receive steganographic image from peer
- Extract hidden data using DCT analysis
- Decrypt with session keys
- Display in chat history

#### GUI Features

**Real-time Status Indicators:**
- **Key Status:** Loaded/Not loaded/Invalid
- **Connection Status:** Disconnected/Connecting/Connected/Error
- **Encryption Status:** Encrypted/Decrypted/Failed
- **Message Status:** Sent/Delivered/Failed

**User Experience Enhancements:**
- **Progress bars** for long operations
- **Image preview** for cover images
- **Chat history** with timestamps
- **Copy/paste support** for messages
- **File dialogs** with appropriate filtering

**Accessibility Features:**
- **Keyboard navigation** for all controls
- **Screen reader compatibility** with proper ARIA labels
- **High contrast mode** support
- **Resizable interface** for different screen sizes

---

## Security Features

### Cryptographic Security

#### 1. Post-Quantum Protection
**Kyber768 Algorithm:**
- **NIST Level 3** security certification
- **Lattice-based** cryptography resistant to quantum attacks
- **Key sizes:** Public key ~1.5KB, Private key ~2.4KB
- **Security margin:** ~256-bit classical security

#### 2. Hybrid Encryption
**Dual-Layer Protection:**
```java
// Layer 1: Symmetric encryption (AES-256 or Vigenère)
String encryptedContent = encryptMessage(plaintext, sessionKey);

// Layer 2: Asymmetric key protection (Kyber)
String protectedKey = encryptKey(sessionKey, publicKey);
```

**Entropy-Based Selection:**
- **High entropy messages:** AES-256 (stronger security)
- **Low entropy messages:** Vigenère (faster processing)
- **Automatic selection:** Based on statistical analysis

#### 3. Key Management
**Secure Key Storage:**
- **Encrypted key files:** `.enc` format with protection
- **Key separation:** Different keys for different purposes
- **Key rotation:** Support for periodic key updates

### Steganographic Security

#### 1. Statistical Undetectability
**Texture-Adaptive Embedding:**
- **Variance analysis:** Skip smooth image regions
- **Threshold optimization:** 200.0 variance threshold
- **Channel selection:** Use appropriate frequency components

#### 2. Robustness to Attacks
**Multi-Channel Distribution:**
- **Fault tolerance:** Single channel damage doesn't destroy message
- **Capacity optimization:** Different channels for different data types
- **Detection resistance:** Requires multi-channel analysis

#### 3. Randomized Embedding
**Deterministic Randomness:**
- **Key-dependent shuffling:** Block order varies with keys
- **Sparse sampling:** Not all blocks used for embedding
- **Coefficient selection:** Mid-frequency DCT coefficients

### Network Security

#### 1. Secure Protocol
**Message Authentication:**
- **Type validation:** Verify message type headers
- **Length checking:** Prevent buffer overflow attacks
- **Format validation:** Ensure proper message structure

#### 2. Connection Security
**Peer Authentication:**
- **Shared key validation:** Both parties must have matching keys
- **Algorithm consistency:** Verify compatible encryption settings
- **State synchronization:** Maintain consistent connection state

---

## Build & Deployment

### Maven Configuration

#### Project Structure
```xml
<groupId>com.stego</groupId>
<artifactId>stego-app</artifactId>
<version>1.0-SNAPSHOT</version>
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

#### Dependencies
**Core Dependencies:**
```xml
<!-- Post-Quantum Cryptography -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>

<!-- GUI Framework -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.1</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

### Build Process

#### Compilation
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Build executable JARs
mvn clean package -DskipTests
```

#### Executable JARs
**Maven Shade Plugin Configuration:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <!-- Multiple JARs for different entry points -->
        <execution><id>encoder</id>...</execution>
        <execution><id>decoder</id>...</execution>
        <execution><id>key-generator</id>...</execution>
        <execution><id>p2p-chat</id>...</execution>
    </executions>
</plugin>
```

**Generated JARs:**
- `stego-encoder.jar` - Command-line encryption tool
- `stego-decoder.jar` - Command-line decryption tool
- `stego-key-generator.jar` - GUI key generator
- `stego-p2p-chat.jar` - GUI chat application

### Deployment Options

#### 1. Maven Exec Plugin (Recommended)
```bash
# Run Key Generator GUI
mvn exec:java -Dexec.mainClass="com.stego.gui.KeyGeneratorMain"

# Run P2P Chat GUI
mvn exec:java -Dexec.mainClass="com.stego.gui.P2PChatMain"
```

#### 2. JavaFX Module Path
```bash
# Requires JavaFX SDK installation
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/stego-p2p-chat.jar
```

#### 3. Native Image (Advanced)
```bash
# Using GraalVM for native compilation
native-image --module-path /path/to/javafx-sdk/lib \
             --add-modules javafx.controls,javafx.fxml \
             -jar target/stego-p2p-chat.jar
```

### System Requirements

#### Minimum Requirements
- **Java:** Version 21 (LTS) or higher
- **Memory:** 512MB RAM minimum
- **Storage:** 50MB disk space
- **Network:** TCP connectivity for P2P features
- **Display:** Graphical environment for GUI applications

#### Recommended Requirements
- **Java:** Version 21 with latest updates
- **Memory:** 2GB RAM or higher
- **Storage:** 100MB disk space
- **Network:** Broadband connection for image transfer
- **Display:** 1024x768 resolution or higher

### Troubleshooting

#### Common Issues

**1. JavaFX Runtime Issues:**
```bash
# Solution: Use Maven exec plugin instead of direct JAR execution
mvn exec:java -Dexec.mainClass="com.stego.gui.P2PChatMain"
```

**2. Key Generation Failures:**
- Check system entropy availability
- Verify file permissions for key storage
- Ensure BouncyCastle PQC provider is properly configured

**3. Network Connection Problems:**
- Verify firewall settings for specified ports
- Check network connectivity between peers
- Ensure both parties use matching key files

**4. Image Processing Errors:**
- Verify image format support (PNG, JPG)
- Check image capacity for message length
- Ensure sufficient texture for embedding

---

## Conclusion

This steganography application represents a comprehensive secure communication system that combines:

- **Post-quantum cryptography** for future-proof security
- **Advanced steganography** for covert message hiding
- **Peer-to-peer networking** for direct communication
- **User-friendly GUIs** for accessible operation

The system provides multiple layers of security while maintaining usability through intuitive graphical interfaces. The modular architecture allows for easy extension and modification of individual components.

**Key Strengths:**
- Quantum-resistant cryptographic foundation
- Robust multi-channel steganographic embedding
- Direct peer-to-peer communication without central servers
- Comprehensive GUI applications for all user levels
- Extensive documentation and error handling

**Potential Applications:**
- Secure corporate communications
- Journalist source protection
- Activist organization coordination
- Personal privacy enhancement
- Academic research in steganography

The application demonstrates advanced concepts in cryptography, steganography, and secure network communication while remaining accessible through well-designed graphical interfaces.