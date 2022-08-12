package com.gmail.artemis.the.gr8.playerstatsexpansion.cache;

import com.gmail.artemis.the.gr8.playerstatsexpansion.MyLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinAndQuitListener implements Listener {

    private static StatCache statCache;

    public JoinAndQuitListener() {
        statCache = StatCache.getInstance();
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        MyLogger.logPersistentWarning("Added " + event.getPlayer().getName() + " to the cache!");
        statCache.addOnlinePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        MyLogger.logPersistentWarning("Removed " + event.getPlayer().getName() + " from the cache!");
        statCache.removeOnlinePlayer(event.getPlayer());
    }
}