package com.gmail.artemis.the.gr8.playerstatsexpansion;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

public class StatCache {

    volatile static StatCache instance;
    private final ConcurrentHashMap<StatType, CompletableFuture<LinkedStatResult>> statCache;

    private StatCache() {
        statCache = new ConcurrentHashMap<>();
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

    public boolean hasRecordOf(StatType statType) {
        String record = statCache.containsKey(statType) ? "[yes]" : "[no]";
        MyLogger.logWarning("(cache) record of " + statType.statistic() + ": " + record);
        return statCache.containsKey(statType);
    }

    /** Updates the cache for the given StatType with the provided value, or adds a new entry
     if there are no records of this StatType yet.*/
    public void update(StatType statType, CompletableFuture<LinkedStatResult> allTopStats) {
        statCache.put(statType, allTopStats);
    }

    /** Update the CompletableFuture for this StatType in the cache with the provided values,
     either when this future has completed or immediately if it is already done.*/
    public void scheduleUpdate(StatType statType, String playerName, int newStatValue) {
        if (statCache.containsKey(statType)) {
            MyLogger.logWarning("Update scheduled for [" + playerName + "] with new value [" + newStatValue + "]");
            CompletableFuture<LinkedStatResult> future = statCache.get(statType);
            future.thenApplyAsync(map -> {
                MyLogger.logWarning("Updating [" + playerName + "] with new value [" + newStatValue + "]");
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
}