package com.gmail.artemis.the.gr8.playerstatsexpansion.cache;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinAndQuitListener implements Listener {

    private static StatCache statCache;

    public JoinAndQuitListener() {
        statCache = StatCache.getInstance();
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        statCache.addOnlinePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        statCache.removeOnlinePlayer(event.getPlayer());
    }
}