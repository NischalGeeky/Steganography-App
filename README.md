# Secure Image Steganography with Hybrid Cryptography

A Java-based steganography application that combines adaptive encryption, image steganography, and post-quantum cryptography for secure data hiding.

## Features

- **Entropy-Based Cipher Selection**: Automatically selects encryption algorithm (AES-256 or Vigenère) based on Shannon entropy analysis
- **Texture-Adaptive Masking**: Skips smooth image regions to avoid visible artifacts during embedding
- **Sparse Randomized Sampling**: Uses deterministic random block ordering for enhanced security
- **Split-Payload Orchestration**: Distributes payload across RGB channels (Red, Green, Blue) for fault tolerance
- **Post-Quantum Cryptography**: Configurable Kyber-based key exchange (Kyber512, Kyber768, Kyber1024)
- **DCT-Based Steganography**: Uses Discrete Cosine Transform for robust data embedding in images

## Prerequisites

- **Java 21 (LTS)** or higher
- **Maven 3.6+**

## Configuration

The application uses a configuration file for post-quantum cryptography algorithm selection:

**Location**: `src/main/resources/config.properties`

Edit this file to change the PQC algorithm:
```properties
# Supported values: Kyber512, Kyber768, Kyber1024
PQC_ALGORITHM=Kyber768
```

## Building the Project

```bash
mvn clean compile
```

## Usage

### Encryption (Embedding Data)

1. Place your message in `message.txt`
2. Place your cover image as `image.png`
3. Run the main class:

```bash
mvn exec:java -Dexec.mainClass="com.stego.Main"
```

The application will:
- Encrypt the message using entropy-based cipher selection
- Generate Kyber key pairs
- Split the encrypted payload into 3 chunks
- Embed chunks across RGB channels in the image
- Output `output.png` (stego image) and `keys.enc` (encrypted keys)

### Decryption (Extracting Data)

```bash
mvn exec:java -Dexec.mainClass="com.stego.ReceiverMain"
```

The application will:
- Extract chunks from all RGB channels
- Decrypt the keys using Kyber private key
- Decrypt and reconstruct the original message

## Project Structure

```
src/main/java/com/stego/
├── Main.java              # Encryption/embedding workflow
├── ReceiverMain.java      # Decryption/extraction workflow
├── FileEncryptor.java     # Entropy-based adaptive encryption
├── FileDecryptor.java     # Adaptive decryption
├── ImageStego.java        # DCT-based steganography with channel support
├── LatticeManager.java    # Post-quantum key management
├── HybridEncryptor.java   # Kyber-based key wrapping
└── HybridDecryptor.java   # Kyber-based key unwrapping

src/main/resources/
└── config.properties      # PQC algorithm configuration
```

## Technical Details

### Encryption Strategy
- Words with entropy > 3.8: AES-256 encryption
- Words with entropy ≤ 3.8: Vigenère cipher

### Steganography Method
- 8x8 DCT blocks on selected color channels
- Texture-adaptive masking (skips smooth regions)
- Deterministic random block ordering
- Split payload across RGB channels

### Key Exchange
- Post-quantum cryptography using Kyber (configurable)
- Hybrid encryption: Kyber wraps AES session keys
- Keys stored in `keys.enc` file

## Dependencies

- BouncyCastle PQC Provider (bcprov-jdk18on, bcpkix-jdk18on, bcutil-jdk18on)
- JUnit (for testing)

## License

[Add your license here]
