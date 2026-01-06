# Requirements Document

## Introduction

This document specifies the requirements for a peer-to-peer steganographic chat system that extends the existing secure image steganography application. The system will provide two GUI applications: a Key Generator for secure key exchange and a P2P Chat application for encrypted image-based communication.

## Glossary

- **Key Generator GUI**: A standalone application that creates and manages `keys.enc` files for secure key exchange
- **P2P Chat GUI**: A peer-to-peer chat application that uses steganography to hide encrypted messages in images
- **keys.enc**: Encrypted key file containing Kyber public/private keys and session keys for secure communication
- **Stego Image**: An image containing hidden encrypted message data using DCT-based steganography
- **Peer**: Another user in the peer-to-peer network running the chat application
- **Cover Image**: Original image used as a container for hiding encrypted messages
- **Session Keys**: AES and Vigenère keys used for message encryption before steganographic embedding

## Requirements

### Requirement 1

**User Story:** As a user, I want to generate and share cryptographic keys securely, so that I can establish secure communication channels with other users.

#### Acceptance Criteria

1. WHEN a user launches the Key Generator GUI THEN the system SHALL display a simple interface for key generation and management
2. WHEN a user clicks "Generate Keys" THEN the system SHALL create a new `keys.enc` file containing Kyber key pairs and session keys
3. WHEN keys are generated THEN the system SHALL display the file location and provide options to save or copy the file
4. WHEN a user loads an existing `keys.enc` file THEN the system SHALL validate the file format and display key information
5. WHERE key generation is requested THEN the system SHALL use the configured PQC algorithm from config.properties

### Requirement 2

**User Story:** As a user, I want to start a peer-to-peer chat session, so that I can communicate securely with other users using steganographic images.

#### Acceptance Criteria

1. WHEN a user launches the P2P Chat GUI THEN the system SHALL display a chat interface with connection options
2. WHEN a user loads a `keys.enc` file THEN the system SHALL initialize the cryptographic components for secure communication
3. WHEN a user starts hosting THEN the system SHALL create a server socket and display connection information
4. WHEN a user connects to a peer THEN the system SHALL establish a TCP connection using provided IP address and port
5. WHEN connection is established THEN the system SHALL display connection status and enable message sending

### Requirement 3

**User Story:** As a user, I want to send encrypted messages hidden in images, so that I can communicate covertly with my peers.

#### Acceptance Criteria

1. WHEN a user types a message and selects a cover image THEN the system SHALL encrypt the message using entropy-based cipher selection
2. WHEN message encryption is complete THEN the system SHALL embed the encrypted message into the image using DCT steganography
3. WHEN steganographic embedding is complete THEN the system SHALL send the stego image to the connected peer
4. WHEN a stego image is sent THEN the system SHALL display the message in the chat history with a timestamp
5. WHERE message length exceeds image capacity THEN the system SHALL display an error and prevent sending

### Requirement 4

**User Story:** As a user, I want to receive and decrypt hidden messages from images, so that I can read secure communications from my peers.

#### Acceptance Criteria

1. WHEN a stego image is received from a peer THEN the system SHALL automatically attempt to extract hidden data
2. WHEN data extraction is successful THEN the system SHALL decrypt the message using the loaded session keys
3. WHEN message decryption is complete THEN the system SHALL display the original message in the chat history
4. WHEN decryption fails THEN the system SHALL display an error message indicating corruption or invalid keys
5. WHEN a received image contains no hidden data THEN the system SHALL display the image as a regular image message

### Requirement 5

**User Story:** As a user, I want to manage my chat sessions and view message history, so that I can track my secure communications.

#### Acceptance Criteria

1. WHEN messages are sent or received THEN the system SHALL display them in chronological order with timestamps
2. WHEN a user scrolls through chat history THEN the system SHALL maintain message ordering and display formatting
3. WHEN a user saves a received stego image THEN the system SHALL provide options to save the cover image or extract the message
4. WHEN connection is lost THEN the system SHALL display disconnection status and allow reconnection attempts
5. WHERE chat history becomes long THEN the system SHALL provide scrolling functionality to view older messages

### Requirement 6

**User Story:** As a user, I want to configure network and security settings, so that I can customize the application for my environment.

#### Acceptance Criteria

1. WHEN a user accesses settings THEN the system SHALL provide options to configure network port and PQC algorithm
2. WHEN network settings are changed THEN the system SHALL validate port availability and update connection parameters
3. WHEN PQC algorithm is changed THEN the system SHALL update the configuration file and require key regeneration
4. WHEN invalid settings are entered THEN the system SHALL display validation errors and prevent saving
5. WHERE settings are modified THEN the system SHALL persist changes to configuration files

### Requirement 7

**User Story:** As a user, I want the system to handle errors gracefully, so that I can continue using the application even when issues occur.

#### Acceptance Criteria

1. WHEN network connection fails THEN the system SHALL display appropriate error messages and retry options
2. WHEN image processing fails THEN the system SHALL log the error and notify the user without crashing
3. WHEN cryptographic operations fail THEN the system SHALL display security warnings and prevent unsafe operations
4. WHEN file operations fail THEN the system SHALL show file system errors and suggest alternative actions
5. WHERE unexpected errors occur THEN the system SHALL log detailed error information for debugging

### Requirement 8

**User Story:** As a user, I want the GUI to be intuitive and responsive, so that I can efficiently use the steganographic chat system.

#### Acceptance Criteria

1. WHEN GUI components are displayed THEN the system SHALL use consistent styling and layout principles
2. WHEN user interactions occur THEN the system SHALL provide immediate visual feedback and progress indicators
3. WHEN long operations are running THEN the system SHALL display progress bars or loading indicators
4. WHEN images are displayed THEN the system SHALL scale them appropriately for the chat interface
5. WHERE accessibility is required THEN the system SHALL support keyboard navigation and screen reader compatibility