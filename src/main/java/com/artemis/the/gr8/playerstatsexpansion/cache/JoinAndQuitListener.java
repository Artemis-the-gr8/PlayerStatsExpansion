package com.artemis.the.gr8.playerstatsexpansion.cache;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinAndQuitListener implements Listener {

    private static StatCache cache;

    public JoinAndQuitListener(StatCache statCache) {
        cache = statCache;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        cache.addOnlinePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        cache.removeOnlinePlayer(event.getPlayer());
    }
}