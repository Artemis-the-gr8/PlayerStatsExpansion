package com.artemis.the.gr8.playerstatsexpansion;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatManager;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstatsexpansion.datamodels.ProcessedArgs;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

public final class RequestHandler {

    private static StatManager statManager;

    public RequestHandler(StatManager statManager) {
        RequestHandler.statManager = statManager;
    }

    public @Nullable StatRequest<Integer> getPlayerRequest(@NotNull ProcessedArgs processedArgs) {
        String playerName = processedArgs.playerName();
        if (playerName == null) {
            MyLogger.logWarning("missing or invalid player-name");
            return null;
        }

        RequestGenerator<Integer> requestGenerator = statManager.createPlayerStatRequest(playerName);
        return createRequest(requestGenerator, processedArgs);
    }

    public @Nullable StatRequest<Long> getServerRequest(ProcessedArgs processedArgs) {
        RequestGenerator<Long> requestGenerator = statManager.createServerStatRequest();
        return createRequest(requestGenerator, processedArgs);
    }

    public @Nullable StatRequest<LinkedHashMap<String, Integer>> getTopRequest(@NotNull ProcessedArgs processedArgs) {
        int topListSize = processedArgs.topListSize();

        RequestGenerator<LinkedHashMap<String, Integer>> requestGenerator = statManager.createTopStatRequest(topListSize);
        return createRequest(requestGenerator, processedArgs);
    }

    public StatRequest<LinkedHashMap<String, Integer>> transformIntoTotalTopRequest(@NotNull StatRequest<?> statRequest) {
        RequestGenerator<LinkedHashMap<String, Integer>> generator = statManager.createTotalTopStatRequest();
        StatRequest.Settings settings = statRequest.getSettings();

        Statistic stat = settings.getStatistic();
        return switch (stat.getType()) {
            case UNTYPED -> generator.untyped(stat);
            case ENTITY -> {
                if (settings.getEntity() != null) {
                    yield generator.entityType(stat, settings.getEntity());
                } else {
                    yield null;
                }
            }
            case BLOCK, ITEM -> {
                Material material = null;
                if (settings.getBlock() != null) {
                    material = settings.getBlock();
                } else if (settings.getItem() != null) {
                    material = settings.getItem();
                }
                if (material != null) {
                    yield generator.blockOrItemType(stat, material);
                } else {
                    yield null;
                }
            }
        };
    }

    private @Nullable <T> StatRequest<T> createRequest(RequestGenerator<T> requestGenerator, @NotNull ProcessedArgs processedArgs) {
        Statistic stat = processedArgs.getStatistic();
        if (stat == null) {
            MyLogger.logWarning("missing or invalid statistic");
            return null;
        }

        switch (stat.getType()) {
            case UNTYPED -> {
                return requestGenerator.untyped(stat);
            }
            case BLOCK, ITEM -> {
                Material material = processedArgs.getMaterialSubStat();
                if (material == null) {
                    MyLogger.logWarning("missing or invalid Material");
                    return null;
                }
                return requestGenerator.blockOrItemType(stat, material);

            }
            case ENTITY -> {
                EntityType entityType = processedArgs.getEntitySubStat();
                if (entityType == null) {
                    MyLogger.logWarning("missing or invalid EntityType");
                    return null;
                }
                return requestGenerator.entityType(stat, entityType);
            }
            default -> {
                return null;
            }
        }
    }
}
