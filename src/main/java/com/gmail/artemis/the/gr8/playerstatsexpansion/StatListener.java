package com.gmail.artemis.the.gr8.playerstatsexpansion;

import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import java.util.concurrent.CompletableFuture;

public final class StatListener implements Listener {

    private static StatCache statCache;

    public StatListener() {
        statCache = StatCache.getInstance();
    }

    @EventHandler
    public void onStatisticIncrementEvent(PlayerStatisticIncrementEvent event) {
        StatType statType = getStatType(event);
        String playerName = event.getPlayer().getName();
        int newValue = event.getNewValue();

        if (statCache.hasRecordOf(statType)) {
            LinkedStatResult linkedStatResult = statCache.tryToGetCompletableFutureResult(statType);

            if (linkedStatResult != null) {
                final CompletableFuture<LinkedStatResult> future =
                        CompletableFuture.supplyAsync(() ->
                        {
                            linkedStatResult.insertValueIntoExistingOrder(playerName, newValue);
                            return linkedStatResult;
                        });

                statCache.update(statType, future);
            }
            else {
                statCache.scheduleUpdate(statType, playerName, newValue);
            }
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