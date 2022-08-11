package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.TextComponent;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.gmail.artemis.the.gr8.playerstats.api.*;
import com.gmail.artemis.the.gr8.playerstats.enums.Unit;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstats.statistic.result.StatResult;
import com.gmail.artemis.the.gr8.playerstats.statistic.result.TopStatResult;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class PlayerStatsExpansion extends PlaceholderExpansion {

    private static StatManager statManager;
    private static ApiFormatter statFormatter;

    private static StatCache statCache;
    private StatListener statListener;

    @Override
    public String getIdentifier() {
        return "playerstats";
    }

    @Override
    public String getAuthor() {
        return "Artemis";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getRequiredPlugin() {
        return "PlayerStats";
    }

    @Override
    public boolean canRegister() {
        PlayerStats playerStats;
        try {
            playerStats = PlayerStats.getAPI();
        } catch (IllegalStateException e) {
            logWarning("Unable to connect to PlayerStats' API!");
            return false;
        }
        statManager = playerStats.getStatManager();
        statFormatter = playerStats.getFormatter();
        statCache = StatCache.getInstance();

        registerListener();
        return true;
    }

    private void registerListener() {
        if (statListener == null) {
            statListener = new StatListener();
            Bukkit.getPluginManager().registerEvents(
                    statListener, PlaceholderAPIPlugin.getInstance());
        }
    }

    /**format: %playerstats_target:arg,stat_name:sub_stat_name% */
    @Override
    public String onRequest(OfflinePlayer player, String args) {
        TextComponent prefix = switch (args) {
            case "prefix" -> statFormatter.getPluginPrefix();
            case "rainbowprefix" -> statFormatter.getRainbowPluginPrefix();
            case "prefixtitle" -> statFormatter.getPluginPrefixAsTitle();
            case "rainbowprefixtitle" -> statFormatter.getRainbowPluginPrefixAsTitle();
            case "demon" -> (TextComponent) MiniMessage.miniMessage().deserialize("<gradient:#f74040:#FF6600:#f74040>fire demon</gradient>");
            default -> null;
        };

        if (prefix != null) {
            registerListener();
            return componentToString(prefix);
        }
        return getStatResult(args);
    }

    private String getStatResult(String args) {
        ProcessedArgs processedArgs = new ProcessedArgs(args);
        if (processedArgs.target == null) {
            logWarning("missing top/server/player selection");
            return null;
        }

        return switch (processedArgs.target) {
            case PLAYER -> getPlayerStatResult(processedArgs);
            case SERVER -> getServerStatResult(processedArgs);
            case TOP -> getTopStatResult(processedArgs);
        };
    }

    private @Nullable String getPlayerStatResult(@NotNull ProcessedArgs processedArgs) {
        StatRequest<Integer> playerRequest = getPlayerRequest(processedArgs);
        if (playerRequest == null) {
            logWarning("playerRequest is null!");
            return null;
        }

        StatResult<Integer> result = playerRequest.execute();
        if (processedArgs.isRawNumberRequest) {
            return result.getNumericalValue().toString();
        }
        return result.getFormattedString();
    }

    private @Nullable String getServerStatResult(@NotNull ProcessedArgs processedArgs) {
        StatRequest<Long> serverRequest = getServerRequest(processedArgs);
        if (serverRequest == null) {
            return null;
        }
        updateCacheIfNeeded(serverRequest);
        StatType statType = StatType.fromRequest(serverRequest);

        CompletableFuture<TopStatResult> future = statCache.get(statType);
        if (Bukkit.isPrimaryThread()) {
            if (!future.isDone()) {
                return "Processing...";
            }
        }
        TopStatResult result = StatCache.tryToGetCompletableFutureResult(future);
        if (result == null) {
            statCache.remove(statType);
            return null;
        }
        else if (processedArgs.isRawNumberRequest) {
            return getRawServerStatResult(result.getNumericalValue()) + "";
        }
        else {
            return getFormattedServerStatResult(result.getNumericalValue(), statType);
        }
    }

    private @Nullable String getTopStatResult(ProcessedArgs processedArgs) {
        StatRequest<LinkedHashMap<String, Integer>> topRequest = getTopRequest(processedArgs);
        if (topRequest == null) {
            return null;
        }
        updateCacheIfNeeded(topRequest);
        StatType statType = StatType.fromRequest(topRequest);

        CompletableFuture<TopStatResult> future = statCache.get(statType);
        if (Bukkit.isPrimaryThread()) {
            if (!future.isDone()) {
                return "Processing...";
            }
        }
        TopStatResult result = StatCache.tryToGetCompletableFutureResult(future);
        if (result == null) {
            statCache.remove(statType);
            return null;
        }
        else if (processedArgs.isRawNumberRequest) {
            int lineNumber = processedArgs.topListSize;
            return getSingleNumberFromTopStatResult(result, lineNumber) + "";
        }
        else {
            return getSingleFormattedTopStatLine(result, processedArgs);
        }
    }

    private @Nullable StatRequest<Integer> getPlayerRequest(@NotNull ProcessedArgs processedArgs) {
        String playerName = processedArgs.playerName;
        if (playerName == null) {
            logWarning("missing or invalid player-name");
            return null;
        }

        RequestGenerator<Integer> requestGenerator = statManager.playerStatRequest(playerName);
        return createRequest(requestGenerator, processedArgs);
    }

    private @Nullable StatRequest<Long> getServerRequest(ProcessedArgs processedArgs) {
        RequestGenerator<Long> requestGenerator = statManager.serverStatRequest();
        return createRequest(requestGenerator, processedArgs);
    }

    private @Nullable StatRequest<LinkedHashMap<String, Integer>> getTopRequest(ProcessedArgs processedArgs) {
        int topListSize = processedArgs.topListSize;

        RequestGenerator<LinkedHashMap<String, Integer>> requestGenerator = statManager.topStatRequest(topListSize);
        return createRequest(requestGenerator, processedArgs);
    }

    private @Nullable <T> StatRequest<T> createRequest(RequestGenerator<T> requestGenerator, ProcessedArgs processedArgs) {
        Statistic stat = processedArgs.getStatistic();
        if (stat == null) {
            logWarning("missing or invalid Statistic");
            return null;
        }

        switch (stat.getType()) {
            case UNTYPED -> {
                return requestGenerator.untyped(stat);
            }
            case BLOCK, ITEM -> {
                Material material = processedArgs.getMaterialSubStat();
                if (material == null) {
                    logWarning("missing or invalid Material");
                    return null;
                }
                return requestGenerator.blockOrItemType(stat, material);

            }
            case ENTITY -> {
                EntityType entityType = processedArgs.getEntitySubStat();
                if (entityType == null) {
                    logWarning("missing or invalid EntityType");
                    return null;
                }
                return requestGenerator.entityType(stat, entityType);
            }
            default -> {
                return null;
            }
        }
    }

    private void updateCacheIfNeeded(StatRequest<?> statRequest) {
        StatType statType = StatType.fromRequest(statRequest);
        if (!statCache.hasRecordOf(statType)) {
            saveToCache(statRequest);
        }
    }

    private void saveToCache(StatRequest<?> statRequest) {
        StatRequest<LinkedHashMap<String, Integer>> newRequest = transformIntoTotalTopRequest(statRequest);
        final CompletableFuture<TopStatResult> future =
                CompletableFuture.supplyAsync(() ->
                        (TopStatResult) newRequest.execute());

        StatType statType = StatType.fromRequest(newRequest);
        statCache.add(statType, future);
    }

    private StatRequest<LinkedHashMap<String, Integer>> transformIntoTotalTopRequest(@NotNull StatRequest<?> statRequest) {
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

    private String getSingleFormattedTopStatLine (TopStatResult topStats, ProcessedArgs processedArgs) {
        int lineNumber = processedArgs.topListSize;
        LinkedHashMap<String, Integer> numbers = topStats.getNumericalValue();
        String[] playerNames = numbers.keySet().toArray(new String[0]);
        String playerName = playerNames[lineNumber-1];
        TextComponent result =
                statFormatter.getFormattedTopStatLine(
                        lineNumber, playerName, numbers.get(playerName), processedArgs.getStatistic());
        return componentToString(result);
    }

    private int getSingleNumberFromTopStatResult(TopStatResult topStats, int lineNumber) {
        LinkedHashMap<String, Integer> numbers = topStats.getNumericalValue();
        String[] playerNames = numbers.keySet().toArray(new String[0]);
        return numbers.get(playerNames[lineNumber-1]);
    }

    private String getFormattedServerStatResult(LinkedHashMap<String, Integer> allStats, StatType statType) {
        Statistic statistic = statType.statistic();
        long result = getRawServerStatResult(allStats);
        String prettySubStat = getPrettySubStatName(statType);

        if (prettySubStat != null) {
            return componentToString(statFormatter.getFormattedServerStat(result, statistic, prettySubStat));
        }
        Unit.Type unitType = Unit.getTypeFromStatistic(statistic);
        if (unitType != Unit.Type.UNTYPED) {
            Unit unit = Unit.getMostSuitableUnit(unitType, result);
            return componentToString(statFormatter.getFormattedServerStat(result, statistic, unit));
        }
        return componentToString(statFormatter.getFormattedServerStat(result, statistic));
    }

    private long getRawServerStatResult(LinkedHashMap<String, Integer> allStats) {
        List<Integer> numbers = allStats
                .values()
                .parallelStream()
                .toList();
        return numbers.parallelStream().mapToLong(Integer::longValue).sum();
    }

    private @Nullable String getPrettySubStatName(StatType statType) {
        String subStatName = statType.getSubStatName();
        if (subStatName == null) {
            return null;
        }
        return statFormatter.BukkitEnumToString(subStatName);
    }

    private @Nullable String componentToString(TextComponent component) {
        if (component == null) {
            return null;
        }
        return statFormatter.TextComponentToString(component);
    }

    public static void logWarning(String msg) {
        Logger myLogger = Logger.getLogger("PlayerStatsExpansion");
        myLogger.warning(msg);
    }
}