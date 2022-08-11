package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.TextComponent;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.gmail.artemis.the.gr8.playerstats.api.*;
import com.gmail.artemis.the.gr8.playerstats.enums.Unit;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstats.statistic.result.StatResult;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public final class PlayerStatsExpansion extends PlaceholderExpansion implements Configurable, Cacheable {

    private static StatManager statManager;
    private static ApiFormatter statFormatter;

    private static StatCache statCache;
    private static StatListener statListener;
    private static int distanceUpdateSetting;
    private static int timeUpdateSetting;

    @Override
    public @NotNull String getIdentifier() {
        return "playerstats";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Artemis";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "PlayerStats";
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("update_interval_in_minutes_for_distance_types", 1);
        configValues.put("update_interval_in_minutes_for_time_types", 1);
        return configValues;
    }

    @Override
    public void clear() {
        MyLogger.clear();
        statCache.clear();
    }

    @Override
    public boolean canRegister() {
        PlayerStats playerStats;
        try {
            playerStats = PlayerStats.getAPI();
        } catch (IllegalStateException e) {
            MyLogger.logWarning("Unable to connect to PlayerStats' API!");
            return false;
        }
        statManager = playerStats.getStatManager();
        statFormatter = playerStats.getFormatter();
        statCache = StatCache.getInstance();

        loadConfigSettings();
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

    private void loadConfigSettings() {
        distanceUpdateSetting = 10;
        timeUpdateSetting = 10;

//        distanceUpdateSetting = this.getInt("update_interval_in_minutes_for_distance_types", 5) * 60;
//        timeUpdateSetting = this.getInt("update_interval_in_minutes_for_time_types", 5) * 60;
    }

    public static int getDistanceUpdateSetting() {
        return distanceUpdateSetting;
    }

    public static int getTimeUpdateSetting() {
        return timeUpdateSetting;
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
            return componentToString(prefix);
        }
        return getStatResult(args);
    }

    private @Nullable String getStatResult(String args) {
        ProcessedArgs processedArgs = new ProcessedArgs(args);
        if (processedArgs.target == null) {
            MyLogger.logWarning("missing top/server/player selection");
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
            MyLogger.logWarning("playerRequest is null!");
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
        StatType statType = StatType.fromRequest(serverRequest);
        updateCache(serverRequest);

        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult == null) {
            return processingMessage();
        }

        long sum = linkedResult.getSumOfAllValues();
        if (processedArgs.isRawNumberRequest) {
            return sum + "";
        }
        else {
            return getFormattedServerStatResult(sum, statType);
        }
    }

    private @Nullable String getTopStatResult(ProcessedArgs processedArgs) {
        StatRequest<LinkedHashMap<String, Integer>> topRequest = getTopRequest(processedArgs);
        if (topRequest == null) {
            return null;
        }
        updateCache(topRequest);
        StatType statType = StatType.fromRequest(topRequest);

        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult == null) {
            return processingMessage();
        }

        int lineNumber = processedArgs.topListSize;
        if (processedArgs.isRawNumberRequest) {
            return linkedResult.getValueAtIndex(lineNumber-1) + "";
        }
        else {
            Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
            return getSingleFormattedTopStatLine(linkedResult, lineNumber, unitType);
        }
    }

    /** Checks if the {@link StatType} of this StatRequest is already stored in the {@link StatCache},
     and adds it to the cache if not.*/
    private void updateCache(StatRequest<?> statRequest) {
        StatType statType = StatType.fromRequest(statRequest);
        if (!statCache.hasRecordOf(statType)) {
            saveToCache(statRequest);
        }
        else if (updateIntervalHasPassed(statType)){
            saveToCache(statRequest);
        }
    }

    private void saveToCache(StatRequest<?> statRequest) {
        MyLogger.logWarning("(main) saving " + statRequest.getStatisticSetting() + " to the Cache...");
        StatRequest<LinkedHashMap<String, Integer>> newRequest = transformIntoTotalTopRequest(statRequest);
        final CompletableFuture<LinkedStatResult> future =
                CompletableFuture.supplyAsync(() ->
                         new LinkedStatResult(newRequest.execute().getNumericalValue())
                );

        StatType statType = StatType.fromRequest(newRequest);
        statCache.update(statType, future);
    }

    private boolean updateIntervalHasPassed(StatType statType) {
        Unit.Type unitTye = Unit.getTypeFromStatistic(statType.statistic());
        if (unitTye == Unit.Type.DISTANCE || unitTye == Unit.Type.TIME) {
            int updateInterval = (unitTye == Unit.Type.DISTANCE) ? distanceUpdateSetting : timeUpdateSetting;
            return statCache.isTimeToUpdate(statType, updateInterval);
        }
        return false;
    }

    private @Nullable StatRequest<Integer> getPlayerRequest(@NotNull ProcessedArgs processedArgs) {
        MyLogger.logWarning("(main) getting playerRequest for [" + processedArgs.getStatistic() + "] [" + processedArgs.playerName + "]");
        String playerName = processedArgs.playerName;
        if (playerName == null) {
            MyLogger.logWarning("missing or invalid player-name");
            return null;
        }

        RequestGenerator<Integer> requestGenerator = statManager.playerStatRequest(playerName);
        return createRequest(requestGenerator, processedArgs);
    }

    private @Nullable StatRequest<Long> getServerRequest(ProcessedArgs processedArgs) {
        MyLogger.logWarning("(main) getting serverRequest for [" + processedArgs.getStatistic() + "]");
        RequestGenerator<Long> requestGenerator = statManager.serverStatRequest();
        return createRequest(requestGenerator, processedArgs);
    }

    private @Nullable StatRequest<LinkedHashMap<String, Integer>> getTopRequest(ProcessedArgs processedArgs) {
        MyLogger.logWarning("(main) getting topRequest for [" + processedArgs.getStatistic() + "] [top: " + processedArgs.topListSize + "]");
        int topListSize = processedArgs.topListSize;

        RequestGenerator<LinkedHashMap<String, Integer>> requestGenerator = statManager.topStatRequest(topListSize);
        return createRequest(requestGenerator, processedArgs);
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

    private StatRequest<LinkedHashMap<String, Integer>> transformIntoTotalTopRequest(@NotNull StatRequest<?> statRequest) {
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

    private String getSingleFormattedTopStatLine (LinkedStatResult topStats, int lineNumber, Unit.Type unitType) {
        String playerName = topStats.getKeyAtIndex(lineNumber-1);
        TextComponent result =
                statFormatter.getTopStatLine(
                        lineNumber, playerName, topStats.get(playerName), unitType);

        return componentToString(result);
    }

    private String getFormattedServerStatResult(long statNumber, StatType statType) {
        Statistic statistic = statType.statistic();
        String prettySubStat = getPrettySubStatName(statType);
        TextComponent result;

        if (prettySubStat != null) {
            result = statFormatter.getServerStat(statNumber, statType.statistic(), prettySubStat);
        } else {
            result = statFormatter.getServerStat(statNumber, statistic);
        }
        return componentToString(result);
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

    private String processingMessage() {
        TextComponent msg = (TextComponent) MiniMessage.miniMessage().deserialize("<#ADE7FF>Processing...");
        return componentToString(msg);
    }
}