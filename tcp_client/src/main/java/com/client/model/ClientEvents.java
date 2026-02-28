package com.client.model;

public interface ClientEvents {
    void onMessage(String message);
    void onStatusChanged(boolean online);
}
