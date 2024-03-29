package com.artemis.the.gr8.playerstatsexpansion.cache;

import com.artemis.the.gr8.playerstatsexpansion.datamodels.StatType;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;


public final class StatListener implements Listener {

    private static StatCache cache;

    public StatListener(StatCache statCache) {
        cache = statCache;
    }

    @EventHandler
    public void onStatisticIncrementEvent(PlayerStatisticIncrementEvent event) {
        StatType statType = getStatType(event);
        String playerName = event.getPlayer().getName();
        int newValue = event.getNewValue();

        if (cache.hasRecordOf(statType)) {
            cache.updateValue(statType, playerName, newValue);
        }
    }

    private StatType getStatType(PlayerStatisticIncrementEvent event) {
        Statistic stat = event.getStatistic();
        return switch (stat.getType()) {
            case UNTYPED -> new StatType(stat, null, null);
            case BLOCK, ITEM -> new StatType(stat, event.getMaterial(), null);
            case ENTITY -> new StatType(stat, null, event.getEntityType());
        };
    }
}