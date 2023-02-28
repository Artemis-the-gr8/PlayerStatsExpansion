package com.artemis.the.gr8.playerstatsexpansion.cache;

import com.artemis.the.gr8.playerstats.api.StatManager;
import com.artemis.the.gr8.playerstats.api.enums.Unit;
import com.artemis.the.gr8.playerstatsexpansion.Config;
import com.artemis.the.gr8.playerstatsexpansion.PlayerStatsExpansion;
import com.artemis.the.gr8.playerstatsexpansion.datamodels.LinkedStatResult;
import com.artemis.the.gr8.playerstatsexpansion.datamodels.StatType;
import com.artemis.the.gr8.playerstatsexpansion.MyLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;

public final class StatCache {

    private static Config config;
    private final StatManager statManager;

    private final ConcurrentHashMap<StatType, CompletableFuture<LinkedStatResult>> storedStatResults;
    private final ConcurrentHashMap<StatType, Instant> lastUpdatedTimestamps;
    private final ConcurrentLinkedQueue<OfflinePlayer> relevantOnlinePlayers;

    public StatCache(StatManager statManager) {
        config = PlayerStatsExpansion.getConfig();
        this.statManager = statManager;

        storedStatResults = new ConcurrentHashMap<>();
        lastUpdatedTimestamps = new ConcurrentHashMap<>();
        relevantOnlinePlayers = new ConcurrentLinkedQueue<>();

        relevantOnlinePlayers.addAll(Bukkit.getOnlinePlayers()
                .stream().filter(player -> !statManager.isExcludedPlayer(player.getName()))
                .toList());
    }

    public void clear() {
        storedStatResults.clear();
        lastUpdatedTimestamps.clear();
        relevantOnlinePlayers.clear();
    }

    public boolean hasRecordOf(StatType statType) {
        return storedStatResults.containsKey(statType);
    }

    public boolean needsUpdatingYet(StatType statType) {
        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
        boolean update = false;
        if (needsManualUpdating(statType)) {
            double updateInterval = (unitType == Unit.Type.DISTANCE) ?
                    config.getDistanceUpdateSetting() :
                    config.getTimeUpdateSetting();

            long secondsBetween = lastUpdatedTimestamps.get(statType).until(Instant.now(), ChronoUnit.SECONDS);
            update = secondsBetween > updateInterval;

        }
        return update;
    }

    /** Adds the given StatType to the cache.*/
    public void add(StatType statType, CompletableFuture<LinkedStatResult> allTopStats) {
        storedStatResults.put(statType, allTopStats);
        lastUpdatedTimestamps.put(statType, Instant.now());
    }

    public void update(StatType statType) {
        lastUpdatedTimestamps.put(statType, Instant.now());

        Map.Entry<StatType, CompletableFuture<LinkedStatResult>> entry = Map.entry(statType, storedStatResults.get(statType));
        CompletableFuture.runAsync(() -> {
            if (needsManualUpdating(entry.getKey())) {
                entry.getValue().thenRunAsync(new Updater(entry));
            }
        });
    }

    public void addOnlinePlayer(OfflinePlayer player) {
        if (!statManager.isExcludedPlayer(player.getName())) {
            relevantOnlinePlayers.add(player);
        }
    }

    public void removeOnlinePlayer(OfflinePlayer player) {
        if (!statManager.isExcludedPlayer(player.getName())) {
            relevantOnlinePlayers.remove(player);
            updateAllForPlayer(player);
        }
    }

    private void updateAllForPlayer(OfflinePlayer player) {
        CompletableFuture.runAsync(() -> storedStatResults.entrySet().stream().parallel().forEach(entry -> {
            if (needsManualUpdating(entry.getKey())) {
                int stat = player.getStatistic(entry.getKey().statistic());
                entry.getValue().thenApplyAsync(linkedResult -> {
                    linkedResult.insertValueIntoExistingOrder(player.getName(), stat);
                    return linkedResult;
                });
            }
        }));
    }

    /** Update the CompletableFuture for this StatType in the cache with the provided values,
     either when this future has completed or immediately if it is already done.*/
    public void updateValue(StatType statType, String playerName, int newStatValue) {
        if (!statManager.isExcludedPlayer(playerName) && storedStatResults.containsKey(statType)) {
            CompletableFuture<LinkedStatResult> future = storedStatResults.get(statType);
            future.thenApplyAsync(map -> {
                map.insertValueIntoExistingOrder(playerName, newStatValue);
                return map;
            });
        }
    }

    /** Attempts to get the value from this CompletableFuture. It checks future.isDone()
     and immediately returns null if that returns false. Otherwise, it tries to get the
     value and return it, with a time-out of 10 seconds to be extra safe.*/
    public @Nullable LinkedStatResult tryToGetCompletableFutureResult(StatType statType) {
        CompletableFuture<LinkedStatResult> cachedResult = storedStatResults.get(statType);
        LinkedStatResult result = null;
        if (!cachedResult.isDone()) {
            return null;
        }
        try {
            result = cachedResult.get(10, TimeUnit.SECONDS);
        } catch (CancellationException canceled) {
            MyLogger.logWarning("Attempting to get a Future value from a CompletableFuture that is canceled!");
            storedStatResults.remove(statType);
        } catch (InterruptedException interrupted) {
            MyLogger.logWarning("This thread was interrupted while waiting for StatResults");
            storedStatResults.remove(statType);
        } catch (ExecutionException exception) {
            MyLogger.logWarning("An ExecutionException occurred while trying to get all statistic values");
            storedStatResults.remove(statType);
        } catch (TimeoutException timeoutException) {
            MyLogger.logWarning("a PlaceHolder request has timed out");
            storedStatResults.remove(statType);
        }
        return result;
    }

    private boolean needsManualUpdating(StatType statType) {
        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
        return unitType == Unit.Type.DISTANCE || unitType == Unit.Type.TIME;
    }

    public class Updater implements Runnable {

        private final Map.Entry<StatType, CompletableFuture<LinkedStatResult>> entry;

        public Updater(Map.Entry<StatType, CompletableFuture<LinkedStatResult>> entryToUpdate) {
            entry = entryToUpdate;
        }

        @Override
        public void run() {
            relevantOnlinePlayers.stream().parallel().forEach(onlinePlayer -> {
                int newStat = onlinePlayer.getStatistic(entry.getKey().statistic());
                entry.getValue().thenApplyAsync(linkedResult -> {
                    linkedResult.insertValueIntoExistingOrder(onlinePlayer.getName(), newStat);
                    return linkedResult;
                });
            });
        }
    }
}