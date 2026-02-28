package com.server;

/**
 * Entry point for the TCP server JavaFX application.
 * Launches {@link ServerApp} which spins up the socket listener and renders the UI.
 */
public class TCPServer {

    public static void main(String[] args) {
        ServerApp.launchApp(args);
    }
}
