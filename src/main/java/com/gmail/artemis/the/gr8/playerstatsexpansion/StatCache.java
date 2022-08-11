package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.statistic.result.TopStatResult;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

public class StatCache {

    volatile static StatCache instance;
    private final ConcurrentHashMap<StatType, CompletableFuture<TopStatResult>> statCache;

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
        return statCache.containsKey(statType);
    }

    /** Add a statistic to the cache and register a listener for it */
    public void add(StatType statType, CompletableFuture<TopStatResult> allTopStats) {
        statCache.put(statType, allTopStats);
    }

    public CompletableFuture<TopStatResult> get(StatType statType) {
        return statCache.get(statType);
    }

    public void remove(StatType statType) {
        statCache.remove(statType);
    }

    public static @Nullable TopStatResult tryToGetCompletableFutureResult(CompletableFuture<TopStatResult> future) {
        TopStatResult result = null;
        try {
            result = future.get(60, TimeUnit.SECONDS);
        } catch (CancellationException canceled) {
            PlayerStatsExpansion.logWarning("Attempting to get a Future value from a CompletableFuture that is canceled!");
        } catch (InterruptedException interrupted) {
            PlayerStatsExpansion.logWarning("This thread was interrupted while waiting for StatResults");
        } catch (ExecutionException exception) {
            PlayerStatsExpansion.logWarning("An ExecutionException occurred while trying to get all statistic values");
        } catch (TimeoutException timeoutException) {
            PlayerStatsExpansion.logWarning("a PlaceHolder request has timed out");
        }
        return result;
    }
}