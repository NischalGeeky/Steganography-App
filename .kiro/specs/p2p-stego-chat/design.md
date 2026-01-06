# Design Document

## Overview

The P2P Steganographic Chat System extends the existing secure image steganography application with two GUI applications that enable secure peer-to-peer communication through steganographic images. The system maintains the existing cryptographic security while adding user-friendly interfaces and networking capabilities.

## Architecture

The system follows a modular architecture with clear separation between the GUI layer, networking layer, and the existing cryptographic/steganographic core:

```
┌─────────────────┐    ┌─────────────────┐
│  Key Generator  │    │   P2P Chat GUI  │
│      GUI        │    │                 │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
         ┌─────────────────┐
         │   GUI Common    │
         │   Components    │
         └─────────────────┘
                     │
         ┌─────────────────┐
         │  Network Layer  │
         │  (P2P Manager)  │
         └─────────────────┘
                     │
         ┌─────────────────┐
         │ Existing Crypto │
         │ & Stego Core    │
         └─────────────────┘
```

## Components and Interfaces

### 1. Key Generator GUI (`KeyGeneratorGUI`)
- **Purpose**: Standalone application for creating and managing `keys.enc` files
- **Key Methods**:
  - `generateKeys()`: Creates new Kyber key pairs and session keys
  - `saveKeysFile(String path)`: Saves keys to specified location
  - `loadKeysFile(String path)`: Loads and validates existing keys file
  - `displayKeyInfo()`: Shows key metadata and status

### 2. P2P Chat GUI (`P2PChatGUI`)
- **Purpose**: Main chat interface with steganographic messaging
- **Key Methods**:
  - `initializeChat(String keysFile)`: Loads keys and initializes crypto components
  - `startHosting(int port)`: Creates server socket for incoming connections
  - `connectToPeer(String ip, int port)`: Establishes connection to remote peer
  - `sendMessage(String message, File coverImage)`: Encrypts and embeds message in image
  - `receiveMessage(BufferedImage stegoImage)`: Extracts and decrypts hidden message

### 3. P2P Network Manager (`P2PNetworkManager`)
- **Purpose**: Handles peer-to-peer networking and message transmission
- **Key Methods**:
  - `startServer(int port)`: Creates server socket and listens for connections
  - `connectToServer(String ip, int port)`: Establishes client connection
  - `sendImage(BufferedImage image)`: Transmits stego image to peer
  - `receiveImage()`: Receives stego image from peer
  - `handleConnection(Socket socket)`: Manages individual peer connections

### 4. Chat Message Handler (`ChatMessageHandler`)
- **Purpose**: Manages message processing and chat history
- **Key Methods**:
  - `processOutgoingMessage(String message, File coverImage)`: Handles message encryption and embedding
  - `processIncomingMessage(BufferedImage stegoImage)`: Handles message extraction and decryption
  - `addMessageToHistory(ChatMessage message)`: Updates chat display
  - `validateMessageCapacity(String message, File image)`: Checks if message fits in image

### 5. GUI Common Components (`GUIUtils`)
- **Purpose**: Shared GUI utilities and styling
- **Key Methods**:
  - `createStyledButton(String text)`: Creates consistent UI buttons
  - `showErrorDialog(String message)`: Displays error messages
  - `showProgressDialog(String operation)`: Shows progress indicators
  - `scaleImageForDisplay(BufferedImage image, int maxWidth, int maxHeight)`: Resizes images for UI

## Data Models

### ChatMessage
```java
public class ChatMessage {
    private String content;
    private LocalDateTime timestamp;
    private MessageType type; // TEXT, IMAGE, STEGO_IMAGE
    private String sender; // "self" or peer identifier
    private BufferedImage image; // for image messages
    private boolean encrypted; // indicates if message was encrypted
}
```

### PeerConnection
```java
public class PeerConnection {
    private String peerAddress;
    private int peerPort;
    private Socket socket;
    private boolean isHost;
    private ConnectionStatus status; // CONNECTING, CONNECTED, DISCONNECTED
    private LocalDateTime connectedAt;
}
```

### KeysData
```java
public class KeysData {
    private String publicKey;
    private String privateKey;
    private String aesKey;
    private String vigenereKey;
    private String algorithm; // Kyber variant
    private LocalDateTime createdAt;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*
Property 1: Key generation produces valid keys
*For any* key generation request, the system should create a keys.enc file containing valid Kyber key pairs and session keys that match the configured algorithm
**Validates: Requirements 1.2, 1.5**

Property 2: Keys file validation is consistent
*For any* keys.enc file, the system should correctly validate valid files and reject invalid files based on format and content verification
**Validates: Requirements 1.4**

Property 3: Crypto initialization from valid keys
*For any* valid keys.enc file, loading it should properly initialize all cryptographic components required for secure communication
**Validates: Requirements 2.2**

Property 4: Server socket creation
*For any* valid port number, starting hosting should create a server socket bound to that port and ready to accept connections
**Validates: Requirements 2.3**

Property 5: TCP connection establishment
*For any* valid IP address and port combination where a server is listening, connection attempts should establish successful TCP connections
**Validates: Requirements 2.4**

Property 6: Entropy-based encryption consistency
*For any* message text, the system should apply AES-256 or Vigenère encryption based on Shannon entropy analysis using the same threshold consistently
**Validates: Requirements 3.1**

Property 7: Steganographic round-trip integrity
*For any* encrypted message and cover image, embedding the message and then extracting it should recover the original encrypted content
**Validates: Requirements 3.2, 4.1, 4.2**

Property 8: Image transmission completeness
*For any* stego image sent to a connected peer, the complete image data should be successfully transmitted and received
**Validates: Requirements 3.3**

Property 9: Message capacity validation
*For any* message and cover image, if the message length exceeds the image's steganographic capacity, the system should reject the operation
**Validates: Requirements 3.5**

Property 10: Non-stego image detection
*For any* regular image without hidden data, the system should correctly identify it as containing no steganographic content
**Validates: Requirements 4.5**

Property 11: Port availability validation
*For any* port number, the system should correctly determine if the port is available for binding or already in use
**Validates: Requirements 6.2**

Property 12: Configuration persistence
*For any* valid configuration change, the system should persist the changes to the configuration file and reload them correctly
**Validates: Requirements 6.3, 6.5**

Property 13: Input validation consistency
*For any* invalid settings input, the system should reject the input and prevent saving invalid configuration
**Validates: Requirements 6.4**

Property 14: Error resilience
*For any* image processing failure, the system should handle the error gracefully without crashing the application
**Validates: Requirements 7.2**

Property 15: Cryptographic error handling
*For any* cryptographic operation failure, the system should detect the failure and prevent unsafe operations from proceeding
**Validates: Requirements 7.3**

Property 16: Error logging completeness
*For any* unexpected error, the system should log sufficient detail to enable debugging and troubleshooting
**Validates: Requirements 7.5**

Property 17: Image scaling consistency
*For any* image displayed in the chat interface, the system should scale it to fit within the interface constraints while maintaining aspect ratio
**Validates: Requirements 8.4**

## Error Handling

The system implements comprehensive error handling across multiple layers:

### Network Layer Errors
- **Connection Failures**: Graceful handling of network timeouts, refused connections, and peer disconnections
- **Transmission Errors**: Detection and recovery from incomplete image transfers
- **Port Conflicts**: Validation of port availability before binding server sockets

### Cryptographic Errors
- **Key Validation**: Verification of key file integrity and format
- **Encryption Failures**: Handling of cipher initialization and operation failures
- **Decryption Failures**: Detection of corrupted or tampered steganographic data

### Image Processing Errors
- **Format Validation**: Support for common image formats with graceful handling of unsupported types
- **Capacity Validation**: Prevention of message overflow beyond image steganographic capacity
- **Corruption Detection**: Identification of damaged or invalid image data

### File System Errors
- **Permission Issues**: Handling of read/write permission failures
- **Disk Space**: Detection of insufficient storage for key files and images
- **Path Validation**: Verification of file paths and directory existence

## Testing Strategy

The testing approach combines unit testing for individual components with property-based testing for system-wide correctness guarantees.

### Unit Testing Approach
Unit tests will focus on:
- GUI component initialization and event handling
- Network connection establishment and data transmission
- File I/O operations for keys and configuration
- Image processing and validation logic
- Error handling and recovery mechanisms

### Property-Based Testing Approach
Property-based tests will use **QuickCheck for Java (junit-quickcheck)** as the testing framework. Each property-based test will run a minimum of 100 iterations to ensure comprehensive coverage of the input space.

Property-based tests will verify:
- **Cryptographic Properties**: Key generation, encryption/decryption round-trips, and algorithm consistency
- **Steganographic Properties**: Message embedding/extraction integrity and capacity validation
- **Network Properties**: Connection establishment, data transmission completeness, and error handling
- **Configuration Properties**: Settings validation, persistence, and consistency
- **Error Handling Properties**: Graceful failure handling and system stability

Each property-based test will be tagged with comments explicitly referencing the correctness property from this design document using the format: **Feature: p2p-stego-chat, Property {number}: {property_text}**

The dual testing approach ensures both concrete functionality verification through unit tests and universal correctness validation through property-based testing, providing comprehensive coverage of the system's behavior across all valid inputs and edge cases.