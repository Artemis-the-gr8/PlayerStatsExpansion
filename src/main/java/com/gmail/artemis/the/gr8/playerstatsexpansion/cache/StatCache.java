package com.gmail.artemis.the.gr8.playerstatsexpansion.cache;

import com.gmail.artemis.the.gr8.playerstats.enums.Unit;
import com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels.LinkedStatResult;
import com.gmail.artemis.the.gr8.playerstatsexpansion.MyLogger;
import com.gmail.artemis.the.gr8.playerstatsexpansion.PlayerStatsExpansion;
import com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels.StatType;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;

public final class StatCache {

    private volatile static StatCache instance;

    private final ConcurrentHashMap<StatType, CompletableFuture<LinkedStatResult>> storedStatResults;
    private final ConcurrentHashMap<StatType, Instant> lastUpdatedTimestamps;
    private final ConcurrentLinkedQueue<OfflinePlayer> onlinePlayers;

    private StatCache() {
        storedStatResults = new ConcurrentHashMap<>();
        lastUpdatedTimestamps = new ConcurrentHashMap<>();
        onlinePlayers = new ConcurrentLinkedQueue<>();
    }

    public static StatCache getInstance() {
        StatCache localVariable = instance;
        if (localVariable != null) {
            return localVariable;
        }
        synchronized (StatCache.class) {
            if (instance == null) {
                instance = new StatCache();
            }
            return instance;
        }
    }

    public void clear() {
        storedStatResults.clear();
        onlinePlayers.clear();
    }

    public boolean hasRecordOf(StatType statType) {
        return storedStatResults.containsKey(statType);
    }

    public boolean needsUpdatingYet(StatType statType) {
        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
        if (needsManualUpdating(statType)) {
            int updateInterval = (unitType == Unit.Type.DISTANCE) ?
                    PlayerStatsExpansion.getDistanceUpdateSetting() :
                    PlayerStatsExpansion.getTimeUpdateSetting();

            long secondsBetween = lastUpdatedTimestamps.get(statType).until(Instant.now(), ChronoUnit.SECONDS);
            return secondsBetween > updateInterval;

        }
        return false;
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
        onlinePlayers.add(player);
    }

    public void removeOnlinePlayer(OfflinePlayer player) {
        onlinePlayers.remove(player);
        updateAllForPlayer(player);
    }

    private void updateAllForPlayer(OfflinePlayer player) {
        MyLogger.logInfo("Updating values for player " + player.getName());
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
        if (storedStatResults.containsKey(statType)) {
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
            onlinePlayers.stream().parallel().forEach(onlinePlayer -> {
                int newStat = onlinePlayer.getStatistic(entry.getKey().statistic());
                entry.getValue().thenApplyAsync(linkedResult -> {
                    linkedResult.insertValueIntoExistingOrder(onlinePlayer.getName(), newStat);
                    return linkedResult;
                });
            });
        }
    }
}