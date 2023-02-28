package com.artemis.the.gr8.playerstatsexpansion.datamodels;

import com.artemis.the.gr8.playerstats.api.StatRequest;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public record StatType(Statistic statistic, @Nullable Material material, @Nullable EntityType entityType) {

    public static StatType fromRequest(StatRequest<?> request) {
        StatRequest.Settings settings = request.getSettings();
        Statistic stat = settings.getStatistic();
        return switch (stat.getType()) {
            case UNTYPED -> new StatType(stat, null, null);
            case BLOCK -> new StatType(stat, settings.getBlock(), null);
            case ITEM -> new StatType(stat, settings.getItem(), null);
            case ENTITY -> new StatType(stat, null, settings.getEntity());
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