package com.yourname.tournament;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager implements Listener {
    private final JavaPlugin plugin;
    private boolean gameActive = false;
    private int timeRemaining = 600;

    private final KillTracker killTracker = new KillTracker();
    private final RespawnProtectionManager protectionManager = new RespawnProtectionManager();
    private final Map<UUID, GameMode> originalModes = new HashMap<>();
    private final Map<UUID, BukkitRunnable> rotationTasks = new HashMap<>();

    private Location winnerSpot = null;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public KillTracker getKillTracker() {
        return killTracker;
    }

    public Player getCurrentLeader() {
        UUID leaderId = killTracker.getWinner();
        return (leaderId != null) ? Bukkit.getPlayer(leaderId) : null;
    }

    public void setWinnerSpot(Location loc) {
        this.winnerSpot = loc;
    }

    private boolean isAdmin(Player player) {
        return player.hasPermission("tournament.admin");
    }

    public void startGame() {
        gameActive = true;
        timeRemaining = 600;
        killTracker.clear();
        protectionManager.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isAdmin(p)) {
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);
            }
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "🏁 Tournament begins! You have 10 minutes!");

        new BukkitRunnable() {
            public void run() {
                if (!gameActive) {
                    cancel();
                    return;
                }

                if (timeRemaining <= 0) {
                    endGame();
                    cancel();
                    return;
                }

                timeRemaining--;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(p);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void endGame() {
        gameActive = false;

        UUID winnerId = killTracker.getWinner();
        String winnerName = (winnerId != null) ? Bukkit.getOfflinePlayer(winnerId).getName() : null;
        int winnerKills = (winnerId != null) ? killTracker.getAllKills().getOrDefault(winnerId, 0) : 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(p.getWorld().getSpawnLocation());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            if (!isAdmin(p)) {
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);
            }
        }

        if (winnerId != null && winnerName != null) {
            Bukkit.broadcastMessage(ChatColor.AQUA + "🏆 Winner: " + winnerName + " with " + winnerKills + " kills!");

            Player winnerOnline = Bukkit.getPlayer(winnerId);
            if (winnerOnline != null && winnerSpot != null) {
                winnerOnline.teleport(winnerSpot);
                winnerOnline.sendMessage(ChatColor.GOLD + "🌟 You’ve been teleported to the winner’s spotlight!");
                winnerOnline.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 10, 1));

                Firework fw = winnerOnline.getWorld().spawn(winnerSpot, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.AQUA)
                        .withFade(Color.YELLOW)
                        .with(FireworkEffect.Type.STAR)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(2);
                fw.setFireworkMeta(meta);

                ArmorStand stand = winnerOnline.getWorld().spawn(winnerSpot.clone().add(0, 1.8, 0), ArmorStand.class);
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setCustomName(ChatColor.GOLD + "🏆 Winner: " + winnerOnline.getName());
                stand.setCustomNameVisible(true);
            }
        } else {
            Bukkit.broadcastMessage(ChatColor.GRAY + "No winner this time. Better luck next round!");
        }
    }

    public void forceStopGame() {
        if (!gameActive) return;
        gameActive = false;
        protectionManager.clear();
        killTracker.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            if (!isAdmin(p)) {
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);
            }
        }

        Bukkit.broadcastMessage(ChatColor.RED + "⛔ Tournament force-stopped by an admin.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameActive) return;

        Player killer = event.getEntity().getKiller();
        Player victim = event.getEntity();

        if (killer == null || victim == null) return;

        killTracker.addKill(killer);
        int killerKills = killTracker.getKills(killer);

        Bukkit.broadcastMessage(ChatColor.GRAY + "☠ " + killer.getName() + " → " + victim.getName() + " [" + killerKills + "]");

        switch (killerKills) {
            case 3 -> killer.getWorld().strikeLightningEffect(killer.getLocation());
            case 5 -> killer.getWorld().spawnParticle(Particle.FLAME, killer.getLocation(), 20);
            case 10 -> killer.playSound(killer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            updateScoreboard(p);
        }
    }
        @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!gameActive) return;

        Player player = event.getPlayer();
        protectionManager.addProtection(player);

        new BukkitRunnable() {
            int seconds = 30;

            public void run() {
                if (!gameActive || !protectionManager.isProtected(player)) {
                    cancel();
                    return;
                }

                if (seconds <= 0) {
                    protectionManager.removeProtection(player);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.GRAY + "🛡️ Protection ended."));
                    cancel();
                    return;
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.YELLOW + "🛡️ " + seconds + "s of protection"));
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!gameActive || !(event.getEntity() instanceof Player player)) return;
        if (protectionManager.isProtected(player)) event.setCancelled(true);
    }

    public void unspectate(Player viewer) {
        GameMode previous = originalModes.getOrDefault(viewer.getUniqueId(), GameMode.SURVIVAL);
        viewer.setGameMode(previous);
        viewer.sendMessage(ChatColor.GREEN + "👋 You’ve returned from spectating.");
        originalModes.remove(viewer.getUniqueId());

        BukkitRunnable task = rotationTasks.remove(viewer.getUniqueId());
        if (task != null) task.cancel();
    }

    public void stopRotation(Player viewer) {
        viewer.sendMessage(ChatColor.GREEN + "🛑 Rotation stopped.");
        unspectate(viewer);
    }

    public void startFollowView(Player viewer, Player target) {
        originalModes.put(viewer.getUniqueId(), viewer.getGameMode());
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.setSpectatorTarget(target);

        viewer.sendTitle(ChatColor.AQUA + "👁️ Spectating: " + target.getName(),
            ChatColor.YELLOW + String.valueOf(killTracker.getKills(target)) + " kills", 0, 40, 10);
    }

    public void startRotation(Player viewer) {
        if (!gameActive || viewer == null) return;

        List<Player> targets = Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.equals(viewer))
            .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
            .filter(p -> !protectionManager.isProtected(p))
            .sorted((a, b) -> killTracker.getKills(b) - killTracker.getKills(a))
            .collect(Collectors.toList());

        if (targets.isEmpty()) {
            viewer.sendMessage(ChatColor.GRAY + "No active players to rotate through.");
            return;
        }

        originalModes.put(viewer.getUniqueId(), viewer.getGameMode());
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.sendMessage(ChatColor.AQUA + "🔁 Starting live rotation...");

        BukkitRunnable rotation = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!viewer.isOnline() || !gameActive) {
                    cancel();
                    return;
                }

                if (index >= targets.size()) index = 0;
                Player target = targets.get(index++);
                startFollowView(viewer, target);
            }
        };

        rotationTasks.put(viewer.getUniqueId(), rotation);
        rotation.runTaskTimer(plugin, 0L, 200L); // 10 seconds
    }

    private void updateScoreboard(Player p) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("tournament", "dummy", ChatColor.GOLD + "Tournament Stats");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(ChatColor.AQUA + "Kills: " + killTracker.getKills(p)).setScore(2);
        objective.getScore(ChatColor.YELLOW + "Time Left: " + formatTime(timeRemaining)).setScore(1);

        p.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}