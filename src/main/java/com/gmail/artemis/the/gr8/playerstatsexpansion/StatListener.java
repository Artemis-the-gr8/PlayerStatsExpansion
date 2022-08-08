package com.gmail.artemis.the.gr8.playerstatsexpansion;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

public final class StatListener implements Listener {

    private Statistic statistic;
    private EntityType entityType;
    private Material material;

    private static StatCache statCache;

    public StatListener(Statistic statistic) {
       this(statistic, null, null);
    }

    public StatListener(Statistic statistic, EntityType entityType) {
        this(statistic, entityType, null);
    }

    public StatListener(Statistic statistic, Material material) {
        this(statistic, null, material);
    }

    private StatListener(Statistic statistic, EntityType entityType, Material material) {
        this.statistic = statistic;
        this.entityType = entityType;
        this.material = material;

        statCache = StatCache.getInstance();
        Bukkit.getPluginManager().registerEvents(this, PlaceholderAPIPlugin.getInstance());
    }

    @EventHandler
    public void onStatisticIncrementEvent(PlayerStatisticIncrementEvent event) {

    }
}
