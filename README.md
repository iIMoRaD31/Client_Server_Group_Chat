#  Group Chat Application — TCP/JavaFX

A real-time multi-client group chat application built with **Java TCP Sockets** and **JavaFX**, following a clean Server-Client architecture with strict separation of concerns.

---

##  Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [How to Build](#how-to-build)
- [How to Run](#how-to-run)
- [UML Diagrams](#uml-diagrams)
- [Technical Design Decisions](#technical-design-decisions)
- [Authors](#authors)

---

## Overview

This application enables real-time group communication between multiple clients through a central TCP server. Clients connect, authenticate with a username, and exchange messages in a shared chat room. The server distributes all messages to every connected participant.

---

## Features

### Client
| Feature | Description |
|---|---|
| **Authentication** | Users enter a username before accessing the chat interface |
| **Read-Only Mode** | Connecting without a username restricts the user to receiving messages only |
| **Real-Time Messaging** | Send messages via the SEND button or by pressing Enter |
| **Active Users** | Type `allUsers` to receive a list of all currently connected users |
| **Disconnect** | Type `end` or `bye` to cleanly disconnect from the server |
| **Status Indicator** | Online/Offline label with a colored circle indicator in the UI |

### Server
| Feature | Description |
|---|---|
| **Concurrent Connections** | Accepts multiple simultaneous client connections (thread-per-client) |
| **Message Broadcasting** | Formats messages with sender username and timestamp, then relays to all clients |
| **Live Client List** | A JavaFX `ListView` showing all connected usernames in real time |
| **Color Coding** | Each user entry in the list is assigned a random background color |
| **Activity Logging** | Displays events such as `Server Started`, `Waiting for Client`, `Welcome [User]` |

---

## Architecture

The application follows a **Server-Client** architecture with the **Observer pattern** used to decouple the business logic (Model) from the user interface (View).

```
┌─────────────────────────────────────────────────┐
│                   SERVER                        │
│  ┌────────────┐    ┌───────────────────────┐    │
│  │ TCPServer  │───▶│     ServerModel        │    │
│  └─────┬──────┘    │  (broadcasts msgs,    │    │
│        │           │   manages clients)    │    │
│  spawns│           └──────────┬────────────┘    │
│        ▼                      │ notifies        │
│  ┌─────────────┐   ┌──────────▼────────────┐    │
│  │ClientHandler│   │  ServerViewController │    │
│  │ (per thread)│   │     (JavaFX UI)        │    │
│  └─────────────┘   └───────────────────────┘    │
└─────────────────────────────────────────────────┘
              │  TCP/IP Socket (port 3000)
┌─────────────▼───────────────────────────────────┐
│                   CLIENT                        │
│  ┌─────────────┐   ┌───────────────────────┐    │
│  │ ClientModel │   │ ClientViewController  │    │
│  │ (socket +   │   │     (JavaFX UI)        │    │
│  │  listener)  │◀──│                       │    │
│  └─────────────┘   └───────────────────────┘    │
└─────────────────────────────────────────────────┘
```

### Key Design Patterns

- **Observer Pattern**: `ServerModel` notifies `ServerViewController` via the `ServerObserver` interface. `ClientModel` notifies `ClientViewController` via the `ClientObserver` interface. This ensures the Model has zero dependency on JavaFX.
- **Thread-per-Connection**: Each connected client gets a dedicated `ClientHandler` thread on the server, enabling truly concurrent message handling.
- **Separation of Concerns**: The Model layer (socket I/O, business logic) is completely independent of the View layer (JavaFX). Replacing the GUI does not require any changes to the networking code.

---

## Project Structure

```
group-chat-app/
├── TCPServer/                     # Maven project — Server application
│   ├── src/main/java/
│   │   ├── server/
│   │   │   ├── TCPServer.java         # Entry point, manages ServerSocket
│   │   │   ├── ServerModel.java       # Core logic: client list, broadcast
│   │   │   ├── ClientHandler.java     # Runnable: one thread per client
│   │   │   ├── ServerObserver.java    # Interface for UI callbacks
│   │   │   ├── ServerViewController.java # JavaFX controller
│   │   │   └── ServerApp.java         # JavaFX Application launcher
│   │   └── common/
│   │       ├── AppConfig.java         # Loads config.properties
│   │       └── MessageProtocol.java   # Shared constants & utilities
│   ├── src/main/resources/
│   │   ├── server-view.fxml
│   │   ├── server-style.css
│   │   └── config.properties          # server.ip, server.port
│   └── pom.xml
│
├── TCPClient/                     # Maven project — Client application
│   ├── src/main/java/
│   │   ├── client/
│   │   │   ├── TCPClient.java         # Entry point
│   │   │   ├── ClientModel.java       # Socket, listener thread, send logic
│   │   │   ├── ClientObserver.java    # Interface for UI callbacks
│   │   │   ├── ClientViewController.java # JavaFX controller
│   │   │   └── ClientApp.java         # JavaFX Application launcher
│   │   └── common/
│   │       ├── AppConfig.java
│   │       └── MessageProtocol.java
│   ├── src/main/resources/
│   │   ├── client-view.fxml
│   │   ├── client-style.css
│   │   └── config.properties          # server.ip, server.port
│   └── pom.xml
│
├── uml/
│   ├── class_diagram.puml
│   ├── deployment_diagram.puml
│   ├── usecase_diagram.puml
│   └── sequence_diagram.puml
│
└── README.md
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17 or higher |
| JavaFX SDK | 17 or higher |
| Maven | 3.8+ |
| IntelliJ IDEA | Any recent version (recommended) |

---

## Configuration

Both applications load network settings from `src/main/resources/config.properties` at startup:

```properties
# config.properties
server.ip=localhost
server.port=3000
```

Modify this file to change the host or port without recompiling.

---

## Dependencies

To run the applications you need to install the following dependencies (remove from the command the dependencies that are already installed on your machine):

### Linux(Ubuntu/Debian) 

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk maven ripgrep && \
mkdir -p javafx && \
for m in base graphics controls; do \
  mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy -Dartifact=org.openjfx:javafx-$m:21.0.2:jar -DoutputDirectory=javafx && \
  mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy -Dartifact=org.openjfx:javafx-$m:21.0.2:jar:linux -DoutputDirectory=javafx; \
done

```

### Windows (PowerShell, Admin)

```bash
winget install -e --id EclipseAdoptium.Temurin.17.JDK
winget install -e --id Apache.Maven
winget install -e --id BurntSushi.ripgrep.MSVC
New-Item -ItemType Directory -Force javafx | Out-Null
"base","graphics","controls" | ForEach-Object {
  mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy "-Dartifact=org.openjfx:javafx-$_:21.0.2:jar" -DoutputDirectory=javafx
  mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy "-Dartifact=org.openjfx:javafx-$_:21.0.2:jar:win" -DoutputDirectory=javafx
}


```

---

## How to Run

### Start the Server (from source code)

```bash
mvn -f project_1/tcp_server javafx:run
```

### Start the Server (from JAR file)

```bash
java --module-path ./javafx --add-modules javafx.controls,javafx.graphics,javafx.base -jar project_1/tcp_server/target/tcp_server-1.0-SNAPSHOT.jar

```

The server window will open and begin listening for connections.

### Start the Client (from source code)

```bash
mvn -f project_1/tcp_client javafx:run
```

### Start the Client (from JAR file)

```bash
java --module-path ./javafx --add-modules javafx.controls,javafx.graphics,javafx.base -jar project_1/tcp_client/target/tcp_client-1.0-SNAPSHOT.jar
```

Launch multiple client instances to simulate a multi-user chat session.

### Special Commands (in the client chat input)

| Command | Action |
|---|---|
| `allUsers` | Displays a list of all currently connected users |
| `end` or `bye` | Disconnects from the server and closes the client |
| *(empty username)* | Connects in Read-Only Mode — cannot send messages |

---

## UML Diagrams

All diagrams are located in the `uml/` directory as PlantUML source files (`.puml`). Render them using the [PlantUML online server](https://www.plantuml.com/plantuml) or the PlantUML IntelliJ plugin.

| Diagram | File | Description |
|---|---|---|
| Class Diagram | `class_diagram.puml` | Full software structure: classes, interfaces, relationships, and packages |
| Deployment Diagram | `deployment_diagram.puml` | Physical nodes (server machine, client machines) and TCP/IP links |
| Use Case Diagram | `usecase_diagram.puml` | User and server interactions with the system |
| Sequence Diagram | `sequence_diagram.puml` | End-to-end message flow: connect → chat → allUsers → disconnect |

---

## Technical Design Decisions

### Thread-per-Connection vs. I/O Multiplexing

This implementation uses the **thread-per-connection** model. Each client connection is handled by a dedicated `ClientHandler` thread. This model is straightforward to implement and reason about, and is perfectly appropriate for the scale of this project (a small group chat). For production systems serving thousands of concurrent clients, **NIO with a Selector** (I/O multiplexing) would be preferred as it avoids the overhead of maintaining thousands of OS threads.

### Why the Observer Pattern?

The Observer pattern allows `ServerModel` and `ClientModel` to report events (new message, user joined, user left) to any registered listener **without importing or depending on JavaFX**. This keeps the model fully testable and portable — you could attach a console-based view, a web view, or a testing harness without changing a single line of networking code.

### Configuration File

Using `config.properties` instead of hardcoded values means the application can be deployed across different environments (development, staging, production) simply by editing a text file, with no recompilation required.

---

## Authors

- **Mouad Hakimi** — UML Architecture & Documentation  
- **Morad Litime** — Server implementation  
- **Abdelhamid Knar** — Client implementation  


