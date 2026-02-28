package com.server.core;

import com.server.util.ColorUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Core server model. Listens for TCP connections and delegates each client to a {@link ClientSession}.
 * All UI updates are surfaced via {@link ServerEvents}.
 */
public class ChatServer {

    private final String host;
    private final int port;
    private final ServerEvents events;

    private final Map<String, ClientSession> clients = new ConcurrentHashMap<>();
    private final Map<String, String> userColors = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatServer(String host, int port, ServerEvents events) {
        this.host = host;
        this.port = port;
        this.events = Objects.requireNonNull(events, "events");
    }

    public void start() {
        if (running) return;
        running = true;
        pool.submit(this::acceptLoop);
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        clients.values().forEach(ClientSession::shutdown);
        pool.shutdownNow();
        events.onServerStatus(false);
    }

    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket()) {
            serverSocket = ss;
            ss.bind(new InetSocketAddress(host, port));
            events.onLog("Server started on " + host + ":" + port);
            events.onServerStatus(true);
            events.onLog("Waiting for clients...");
            while (running) {
                Socket socket = ss.accept();
                pool.submit(() -> performHandshake(socket));
            }
        } catch (IOException e) {
            events.onLog("Server stopped: " + e.getMessage());
        } finally {
            running = false;
            events.onServerStatus(false);
        }
    }

    private void performHandshake(Socket socket) {
        boolean registered = false;
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("Welcome! Send your username (blank = read-only).");
            dos.flush();
            String raw = dis.readUTF();
            String username = raw == null ? "" : raw.trim();
            boolean readOnly = username.isEmpty();
            if (readOnly) {
                username = "reader-" + Integer.toHexString(Math.abs(socket.hashCode())).substring(0, 4);
            }
            if (clients.containsKey(username)) {
                dos.writeUTF("Username already taken. Disconnecting.");
                dos.flush();
                socket.close();
                return;
            }

            String color = ColorUtil.randomPastelHex();
            userColors.put(username, color);
            ClientSession session = new ClientSession(username, readOnly, socket, dis, dos, this);
            clients.put(username, session);
            registered = true;

            events.onUsersChanged(Collections.unmodifiableMap(userColors));
            events.onLog("Welcome " + username + " (" + socket.getRemoteSocketAddress() + ")");
            broadcastSystem(username + " joined the room.");

            pool.submit(session);
        } catch (IOException e) {
            events.onLog("Handshake failed: " + e.getMessage());
        } finally {
            if (!registered) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    void broadcastSystem(String msg) {
        broadcast("[SYSTEM]", msg);
    }

    void broadcast(String fromUser, String msg) {
        String formatted = String.format("[%s] %s: %s", LocalTime.now().format(TIME_FMT), fromUser, msg);
        events.onLog(formatted);
        for (ClientSession session : clients.values()) {
            session.send(formatted);
        }
    }

    void sendUserList(ClientSession target) {
        String list = String.join(", ", clients.keySet());
        target.send("[USERS] " + (list.isEmpty() ? "(none)" : list));
    }

    void removeClient(String username) {
        if (clients.remove(username) != null) {
            userColors.remove(username);
            events.onUsersChanged(Collections.unmodifiableMap(userColors));
            broadcastSystem(username + " left the room.");
        }
    }
}
