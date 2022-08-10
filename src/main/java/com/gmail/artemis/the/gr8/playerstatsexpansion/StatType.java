package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public record StatType(Statistic statistic, @Nullable EntityType entityType, @Nullable Material material) {

    public static StatType fromRequest(StatRequest<?> request) {
        Statistic stat = request.getStatisticSetting();
        return switch (stat.getType()) {
            case UNTYPED -> new StatType(stat, null, null);
            case BLOCK -> new StatType(stat, null, request.getBlockSetting());
            case ITEM -> new StatType(stat, null, request.getItemSetting());
            case ENTITY -> new StatType(stat, request.getEntitySetting(), null);
        };
    }
}