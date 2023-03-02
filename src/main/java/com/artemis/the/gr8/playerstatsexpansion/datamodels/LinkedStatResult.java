package com.artemis.the.gr8.playerstatsexpansion.datamodels;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class LinkedStatResult {

    private LinkedHashMap<String, Integer> statResult;

    public LinkedStatResult(LinkedHashMap<String, Integer> linkedStats) {
        statResult = linkedStats;
    }

    public void insertValueIntoExistingOrder(String playerName, Integer statNumber) {
        statResult.put(playerName, statNumber);
        List<String> playerNames = statResult.keySet().stream().toList();
        int index = playerNames.indexOf(playerName);
        if (index == 0) {
            return;
        }

        int higherValue = statResult.get(playerNames.get(index-1));
        int lowerValue = statResult.get(playerNames.get(index+1));
        if (statNumber <= higherValue && statNumber >= lowerValue) {
            return;
        }

        statResult = statResult.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public Integer get(String key) {
        if (statResult.containsKey(key)) {
            return statResult.get(key);
        }
        return 0;
    }

    public Integer getValueAtIndex(int index) {
        String[] playerNames = statResult.keySet().toArray(new String[0]);
        if (playerNames.length <= index) {
            return 0;
        }
        return statResult.get(playerNames[index]);
    }

    public @Nullable String getKeyAtIndex(int index) {
        String[] playerNames = statResult.keySet().toArray(new String[0]);
        if (playerNames.length <= index) {
            return null;
        }
        return playerNames[index];
    }

    public long getSumOfAllValues() {
        List<Integer> numbers = statResult
                .values()
                .parallelStream()
                .toList();
        return numbers.parallelStream().mapToLong(Integer::longValue).sum();
    }
}