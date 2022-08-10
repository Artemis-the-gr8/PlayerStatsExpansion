package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.statistic.result.TopStatResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
}