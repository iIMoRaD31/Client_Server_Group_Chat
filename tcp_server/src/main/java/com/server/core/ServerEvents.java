package com.server.core;

import java.util.Map;

public interface ServerEvents {
    void onLog(String message);
    void onUsersChanged(Map<String, String> userColors);
    void onServerStatus(boolean online);
}
