package com.client.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket communication layer for the JavaFX client.
 * Keeps networking off the UI thread.
 */
public class ChatClient {
    private final String host;
    private final int port;
    private final String username;
    private final boolean readOnly;
    private final ClientEvents events;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChatClient(String host, int port, String username, ClientEvents events) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.username = username == null ? "" : username.trim();
        this.readOnly = this.username.isEmpty();
        this.events = Objects.requireNonNull(events);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void connect() {
        executor.submit(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 4000);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                connected.set(true);
                events.onStatusChanged(true);

                // Server sends greeting first.
                String greeting = dis.readUTF();
                events.onMessage(greeting);

                // Send username (may be blank -> read-only).
                dos.writeUTF(username);
                dos.flush();

                listen();
            } catch (IOException e) {
                events.onMessage("Connection failed: " + e.getMessage());
                connected.set(false);
                events.onStatusChanged(false);
            } finally {
                cleanup();
            }
        });
    }

    private void listen() {
        try {
            while (connected.get() && !socket.isClosed()) {
                String msg = dis.readUTF();
                events.onMessage(msg);
            }
        } catch (IOException ignored) {
            // falls through to cleanup
        }
        connected.set(false);
        events.onStatusChanged(false);
    }

    public void sendMessage(String message) {
        if (!connected.get() || dos == null) {
            events.onMessage("Not connected to server.");
            return;
        }
        if (readOnly) {
            events.onMessage("[READ-ONLY] You need a username to chat.");
            return;
        }
        try {
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            events.onMessage("Send failed: " + e.getMessage());
            disconnect();
        }
    }

    public void requestActiveUsers() {
        if (connected.get() && dos != null) {
            try {
                dos.writeUTF("allUsers");
                dos.flush();
            } catch (IOException e) {
                events.onMessage("Request failed: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (!connected.compareAndSet(true, false)) {
            return;
        }
        try {
            if (dos != null) {
                dos.writeUTF("bye");
                dos.flush();
            }
        } catch (IOException ignored) {
        } finally {
            cleanup();
            events.onStatusChanged(false);
        }
    }

    private void cleanup() {
        executor.shutdownNow();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
