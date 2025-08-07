package com.yourname.tournament;

import org.bukkit.entity.Player;

import java.util.*;

public class KillTracker {
    private final Map<UUID, Integer> killCounts = new HashMap<>();

    public void addKill(Player killer) {
        UUID uuid = killer.getUniqueId();
        killCounts.put(uuid, killCounts.getOrDefault(uuid, 0) + 1);
    }

    public int getKills(Player player) {
        return killCounts.getOrDefault(player.getUniqueId(), 0);
    }

    public Map<UUID, Integer> getAllKills() {
        return killCounts;
    }

    public UUID getWinner() {
        return killCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    public void clear() {
        killCounts.clear();
    }
}