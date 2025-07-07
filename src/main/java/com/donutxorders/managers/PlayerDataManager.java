package com.donutxorders.managers;

import com.donutxorders.models.PlayerData;
import java.util.UUID;

public class PlayerDataManager {
    // Minimal stub for compilation. Expand as needed.

    public PlayerData getOrLoadPlayerData(UUID playerId) {
        // Stub: return a new PlayerData or fetch from cache/database
        return new PlayerData(playerId);
    }

    public void savePlayerData(PlayerData data) {
        // Stub: save logic
    }
}

