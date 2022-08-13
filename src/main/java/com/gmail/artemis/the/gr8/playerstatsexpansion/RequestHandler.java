package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.gmail.artemis.the.gr8.playerstats.api.StatManager;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels.ProcessedArgs;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

public class RequestHandler {

    private final StatManager statManager;

    public RequestHandler(StatManager statManager) {
        this.statManager = statManager;
    }

    public @Nullable StatRequest<Integer> getPlayerRequest(@NotNull ProcessedArgs processedArgs) {
        MyLogger.logWarning("(main) getting playerRequest for [" + processedArgs.getStatistic() + "] [" + processedArgs.playerName() + "]");
        String playerName = processedArgs.playerName();
        if (playerName == null) {
            MyLogger.logWarning("missing or invalid player-name");
            return null;
        }

        RequestGenerator<Integer> requestGenerator = statManager.playerStatRequest(playerName);
        return createRequest(requestGenerator, processedArgs);
    }

    public @Nullable StatRequest<Long> getServerRequest(ProcessedArgs processedArgs) {
        MyLogger.logWarning("(main) getting serverRequest for [" + processedArgs.getStatistic() + "]");
        RequestGenerator<Long> requestGenerator = statManager.serverStatRequest();
        return createRequest(requestGenerator, processedArgs);
    }

    public @Nullable StatRequest<LinkedHashMap<String, Integer>> getTopRequest(ProcessedArgs processedArgs) {
        MyLogger.logWarning("(main) getting topRequest for [" + processedArgs.getStatistic() + "] [top: " + processedArgs.topListSize() + "]");
        int topListSize = processedArgs.topListSize();

        RequestGenerator<LinkedHashMap<String, Integer>> requestGenerator = statManager.topStatRequest(topListSize);
        return createRequest(requestGenerator, processedArgs);
    }

    public StatRequest<LinkedHashMap<String, Integer>> transformIntoTotalTopRequest(@NotNull StatRequest<?> statRequest) {
        MyLogger.logWarning("(main) transforming request into total request for [" + statRequest.getTargetSetting() + "] [" + statRequest.getStatisticSetting() + "]");
        RequestGenerator<LinkedHashMap<String, Integer>> generator = statManager.totalTopStatRequest();
        Statistic stat = statRequest.getStatisticSetting();
        return switch (stat.getType()) {
            case UNTYPED -> generator.untyped(stat);
            case ENTITY -> {
                if (statRequest.getEntitySetting() != null) {
                    yield generator.entityType(stat, statRequest.getEntitySetting());
                } else {
                    yield null;
                }
            }
            case BLOCK, ITEM -> {
                Material material = null;
                if (statRequest.getBlockSetting() != null) {
                    material = statRequest.getBlockSetting();
                } else if (statRequest.getItemSetting() != null) {
                    material = statRequest.getItemSetting();
                }
                if (material != null) {
                    yield generator.blockOrItemType(stat, material);
                } else {
                    yield null;
                }
            }
        };
    }

    private @Nullable <T> StatRequest<T> createRequest(RequestGenerator<T> requestGenerator, ProcessedArgs processedArgs) {
        Statistic stat = processedArgs.getStatistic();
        if (stat == null) {
            MyLogger.logWarning("missing or invalid Statistic");
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
