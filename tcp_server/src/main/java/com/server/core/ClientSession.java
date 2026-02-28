package com.server.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a connected client. Runs on its own thread and communicates
 * with the owning {@link ChatServer}.
 */
public class ClientSession implements Runnable {
    private final String username;
    private final boolean readOnly;
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final ChatServer server;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public ClientSession(String username, boolean readOnly, Socket socket, DataInputStream dis,
                         DataOutputStream dos, ChatServer server) {
        this.username = username;
        this.readOnly = readOnly;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            while (active.get() && !socket.isClosed()) {
                String msg = dis.readUTF();
                if (msg == null) {
                    continue;
                }

                if (isDisconnectCommand(msg)) {
                    break;
                }

                if ("allUsers".equalsIgnoreCase(msg.trim())) {
                    server.sendUserList(this);
                    continue;
                }

                if (readOnly) {
                    send("[READ-ONLY] Set a username to chat.");
                    continue;
                }

                server.broadcast(username, msg);
            }
        } catch (IOException ignored) {
            // Connection dropped; we'll clean up in finally.
        } finally {
            shutdown();
        }
    }

    void send(String msg) {
        try {
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException ignored) {
            // If sending fails, we'll end up closing the session soon anyway.
            shutdown();
        }
    }

    void shutdown() {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {}
        server.removeClient(username);
    }

    private boolean isDisconnectCommand(String msg) {
        return "end".equalsIgnoreCase(msg.trim()) || "bye".equalsIgnoreCase(msg.trim());
    }
}
