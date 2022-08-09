package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.statistic.result.TopStatResult;
import org.bukkit.Statistic;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StatCache {

    volatile static StatCache instance;
    private final ConcurrentHashMap<Statistic, CompletableFuture<TopStatResult>> statCache;

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

    public boolean hasRecordOf(Statistic statistic) {
        return statCache.containsKey(statistic);
    }

    public void store(Statistic statistic, CompletableFuture<TopStatResult> allTopStats) {
        statCache.put(statistic, allTopStats);
    }

    public CompletableFuture<TopStatResult> get(Statistic statistic) {
        return statCache.get(statistic);
    }

    public void remove(Statistic statistic) {
        statCache.remove(statistic);
    }
}
