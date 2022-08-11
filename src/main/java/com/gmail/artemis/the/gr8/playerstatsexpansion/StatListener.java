package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.statistic.result.TopStatResult;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class StatListener implements Listener {

    private static StatCache statCache;

    public StatListener() {
        statCache = StatCache.getInstance();
    }

    @EventHandler
    public void onStatisticIncrementEvent(PlayerStatisticIncrementEvent event) {
        StatType statType = getStatType(event);
        if (statCache.hasRecordOf(statType)) {
            CompletableFuture<TopStatResult> future = statCache.get(statType);
            if (future.isDone()) {
                TopStatResult topStatResult = StatCache.tryToGetCompletableFutureResult(future);
                int newValue = event.getNewValue();
                assert topStatResult != null;
                LinkedHashMap<String, Integer> updatedValues = getUpdatedHashMap(
                        topStatResult.getNumericalValue(), event.getPlayer().getName(), newValue);

                //add formatted message to create new TopStatResult?
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

    private LinkedHashMap<String, Integer> getUpdatedHashMap(LinkedHashMap<String, Integer> oldValues, String playerName, int newValue) {
        oldValues.put(playerName, newValue);
        List<String> playerNames = oldValues.keySet().stream().toList();
        int index = playerNames.indexOf(playerName);
        if (index == -1 || index == 1) {
            return oldValues;
        }

        int higherValue = oldValues.get(playerNames.get(index-1));
        int lowerValue = oldValues.get(playerNames.get(index+1));
        if (newValue <= higherValue && newValue >= lowerValue) {
            return oldValues;
        }

        return oldValues.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}