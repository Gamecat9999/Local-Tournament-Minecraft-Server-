package com.yourname.tournament;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TournamentPlugin extends JavaPlugin {
    private GameManager gameManager;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);
        Bukkit.getPluginManager().registerEvents(gameManager, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        switch (label.toLowerCase()) {
            case "starttournament" -> {
                if (player.hasPermission("tournament.start")) {
                    gameManager.startGame();
                    player.sendMessage(ChatColor.GREEN + "Tournament started!");
                }
                return true;
            }
            case "stoptournament" -> {
                if (player.hasPermission("tournament.start")) {
                    gameManager.forceStopGame();
                    player.sendMessage(ChatColor.RED + "Tournament stopped.");
                }
                return true;
            }
            case "spectate" -> {
                if (!player.hasPermission("tournament.admin")) {
                    player.sendMessage(ChatColor.RED + "⛔ You must be an admin to use spectate commands.");
                    return true;
                }

                if (args.length == 1 && args[0].equalsIgnoreCase("rotate")) {
                    gameManager.startRotation(player);
                } else if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
                    gameManager.stopRotation(player);
                } else if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        gameManager.startFollowView(player, target);
                    } else {
                        player.sendMessage(ChatColor.RED + "Player not found.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /spectate <player>|rotate|stop");
                }
                return true;
            }
            case "spectateleader" -> {
                if (!player.hasPermission("tournament.admin")) {
                    player.sendMessage(ChatColor.RED + "⛔ You must be an admin to spectate the leader.");
                    return true;
                }

                Player leader = gameManager.getCurrentLeader();
                if (leader != null) {
                    gameManager.startFollowView(player, leader);
                } else {
                    player.sendMessage(ChatColor.RED + "No active leader to spectate.");
                }
                return true;
            }
            case "unspectate" -> {
                gameManager.unspectate(player);
                return true;
            }
            case "tpall" -> {
                if (player.hasPermission("tournament.admin")) {
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        if (!target.equals(player)) {
                            target.teleport(player.getLocation());
                        }
                    }
                    player.sendMessage(ChatColor.GREEN + "✅ All players teleported to you.");
                }
                return true;
            }
            case "respawn" -> {
                if (player.hasPermission("tournament.admin") && args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null && !target.isDead()) {
                        target.setHealth(0.0);
                        player.sendMessage(ChatColor.GREEN + "✅ Forced respawn for " + target.getName());
                    } else {
                        player.sendMessage(ChatColor.RED + "Player not found or already dead.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /respawn <player>");
                }
                return true;
            }
            case "setwinnerspot" -> {
                if (player.hasPermission("tournament.admin")) {
                    gameManager.setWinnerSpot(player.getLocation());
                    player.sendMessage(ChatColor.GOLD + "Winner spot set to your location.");
                }
                return true;
            }
        }

        return false;
    }
}