package com.gmail.artemis.the.gr8.playerstatsexpansion.cache;

import com.gmail.artemis.the.gr8.playerstats.enums.Unit;
import com.gmail.artemis.the.gr8.playerstatsexpansion.LinkedStatResult;
import com.gmail.artemis.the.gr8.playerstatsexpansion.MyLogger;
import com.gmail.artemis.the.gr8.playerstatsexpansion.StatType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;

public final class StatCache {

    private volatile static StatCache instance;

    private final ConcurrentHashMap<StatType, CompletableFuture<LinkedStatResult>> statCache;
    private final ConcurrentHashMap<StatType, Instant> lastUpdated;
    private final ConcurrentLinkedQueue<Player> onlinePlayers;

    private StatCache() {
        statCache = new ConcurrentHashMap<>();
        lastUpdated = new ConcurrentHashMap<>();
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
        statCache.clear();
        lastUpdated.clear();
    }

    public boolean hasRecordOf(StatType statType) {
        String record = statCache.containsKey(statType) ? "[yes]" : "[no]";
        MyLogger.logWarning("(cache) record of " + statType.statistic() + ": " + record);
        return statCache.containsKey(statType);
    }

    public boolean isTimeToUpdate(StatType statType, int secondsToPass) {
        long secondsBetween = lastUpdated.get(statType).until(Instant.now(), ChronoUnit.SECONDS);
        boolean update = secondsBetween > secondsToPass;
        if (update) {
            MyLogger.logWarning("Time to update " + statType.statistic());
        }
        return update;
    }

    /** Adds the given StatType to the cache.*/
    public void add(StatType statType, CompletableFuture<LinkedStatResult> allTopStats) {
        statCache.put(statType, allTopStats);

        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
        if (unitType == Unit.Type.DISTANCE || unitType == Unit.Type.TIME) {
            lastUpdated.put(statType, Instant.now());
        }
    }

    public void update() {
        MyLogger.logPersistentWarning("Updating cache!");
        CompletableFuture.runAsync(() -> statCache.entrySet().stream().parallel().forEach(entry -> {
            if (needsManualUpdating(entry.getKey())) {
                entry.getValue().thenRunAsync(new Updater(entry));
            }
        }));
    }

    public void addOnlinePlayer(Player player) {
        onlinePlayers.add(player);
    }

    public void removeOnlinePlayer(Player player) {
        onlinePlayers.remove(player);
        statCache.entrySet().stream().parallel().forEach(entry -> {
            if (needsManualUpdating(entry.getKey())) {
                entry.getValue().thenRunAsync(new Updater(entry));
            }
        });
    }

    /** Update the CompletableFuture for this StatType in the cache with the provided values,
     either when this future has completed or immediately if it is already done.*/
    public void completeWithNewValue(StatType statType, String playerName, int newStatValue) {
        if (statCache.containsKey(statType)) {
            MyLogger.logPersistentWarning("Update scheduled for [" + playerName + "] with new value [" + newStatValue + "]");
            CompletableFuture<LinkedStatResult> future = statCache.get(statType);
            future.thenApplyAsync(map -> {
                MyLogger.logPersistentWarning("Updating [" + playerName + "] with new value [" + newStatValue + "]");
                map.insertValueIntoExistingOrder(playerName, newStatValue);
                return map;
            });
        }
    }

    /** Attempts to get the value from this CompletableFuture. It checks future.isDone()
     and immediately returns null if that returns false. Otherwise, it tries to get the
     value and return it, with a time-out of 10 seconds to be extra safe.*/
    public @Nullable LinkedStatResult tryToGetCompletableFutureResult(StatType statType) {
        CompletableFuture<LinkedStatResult> cachedResult = statCache.get(statType);
        LinkedStatResult result = null;
        if (!cachedResult.isDone()) {
            MyLogger.logWarning("(cache) waiting for task...");
            return null;
        }
        try {
            result = cachedResult.get(10, TimeUnit.SECONDS);
        } catch (CancellationException canceled) {
            MyLogger.logWarning("Attempting to get a Future value from a CompletableFuture that is canceled!");
            statCache.remove(statType);
        } catch (InterruptedException interrupted) {
            MyLogger.logWarning("This thread was interrupted while waiting for StatResults");
            statCache.remove(statType);
        } catch (ExecutionException exception) {
            exception.printStackTrace();
            MyLogger.logWarning("An ExecutionException occurred while trying to get all statistic values");
            statCache.remove(statType);
        } catch (TimeoutException timeoutException) {
            MyLogger.logWarning("a PlaceHolder request has timed out");
            statCache.remove(statType);
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
                entry.getValue().thenApplyAsync(map -> {
                    MyLogger.logPersistentWarning("Updating [" + onlinePlayer.getName() + "] with new value for [" + entry.getKey().statistic() + "]");
                    map.insertValueIntoExistingOrder(onlinePlayer.getName(), newStat);
                    lastUpdated.put(entry.getKey(), Instant.now());
                    return map;
                });
            });
        }
    }
}