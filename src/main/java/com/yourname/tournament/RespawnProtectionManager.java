package com.yourname.tournament;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RespawnProtectionManager {
    private final Set<UUID> protectedPlayers = new HashSet<>();

    public void addProtection(Player player) {
        protectedPlayers.add(player.getUniqueId());
    }

    public void removeProtection(Player player) {
        protectedPlayers.remove(player.getUniqueId());
    }

    public boolean isProtected(Player player) {
        return protectedPlayers.contains(player.getUniqueId());
    }

    public void clear() {
        protectedPlayers.clear();
    }
}