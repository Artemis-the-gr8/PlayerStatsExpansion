package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public record StatType(Statistic statistic, @Nullable Material material, @Nullable EntityType entityType) {

    public static StatType fromRequest(StatRequest<?> request) {
        Statistic stat = request.getStatisticSetting();
        return switch (stat.getType()) {
            case UNTYPED -> new StatType(stat, null, null);
            case BLOCK -> new StatType(stat, request.getBlockSetting(), null);
            case ITEM -> new StatType(stat, request.getItemSetting(), null);
            case ENTITY -> new StatType(stat, null, request.getEntitySetting());
        };
    }

    public String getSubStatName() {
        return switch (statistic().getType()) {
            case BLOCK, ITEM -> {
                Material material = material();
                if (material != null) {
                    yield material.toString();
                } else {
                    yield null;
                }
            }
            case ENTITY -> {
                EntityType entityType = entityType();
                if (entityType != null) {
                    yield entityType.toString();
                } else {
                    yield null;
                }
            }
            default -> null;
        };
    }
}