package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.TextComponent;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.gmail.artemis.the.gr8.playerstats.api.*;
import com.gmail.artemis.the.gr8.playerstats.enums.Unit;
import com.gmail.artemis.the.gr8.playerstats.msg.msgutils.NumberFormatter;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstats.statistic.result.StatResult;
import com.gmail.artemis.the.gr8.playerstatsexpansion.cache.JoinAndQuitListener;
import com.gmail.artemis.the.gr8.playerstatsexpansion.cache.StatCache;
import com.gmail.artemis.the.gr8.playerstatsexpansion.cache.StatListener;
import com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels.LinkedStatResult;
import com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels.ProcessedArgs;
import com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels.StatType;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public final class PlayerStatsExpansion extends PlaceholderExpansion implements Configurable, Cacheable {

    private static ApiFormatter statFormatter;

    private static RequestHandler requestHandler;
    private static StatCache statCache;
    private static StatListener statListener;
    private static JoinAndQuitListener joinAndQuitListener;

    private static int distanceUpdateSetting;
    private static int timeUpdateSetting;
    private static int maxTimeUnits;

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
        configValues.put("max_amount_of_smaller_time_units_to_display", 2);
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
        StatManager statManager = playerStats.getStatManager();
        requestHandler = new RequestHandler(statManager);

        statFormatter = playerStats.getFormatter();
        statCache = StatCache.getInstance();

        loadConfigSettings();
        registerListeners();
        return true;
    }

    private void registerListeners() {
        if (statListener == null) {
            statListener = new StatListener();
            Bukkit.getPluginManager().registerEvents(
                    statListener, PlaceholderAPIPlugin.getInstance());
        }
        if (joinAndQuitListener == null) {
            joinAndQuitListener = new JoinAndQuitListener();
            Bukkit.getPluginManager().registerEvents(
                    joinAndQuitListener, PlaceholderAPIPlugin.getInstance());
        }
    }

    private void loadConfigSettings() {
        distanceUpdateSetting = 10;
        timeUpdateSetting = 10;
        maxTimeUnits = this.getInt("max_amount_of_smaller_time_units_to_display", 2);

//        distanceUpdateSetting = this.getInt("update_interval_in_minutes_for_distance_types", 5) * 60;
//        timeUpdateSetting = this.getInt("update_interval_in_minutes_for_time_types", 5) * 60;
    }

    public static int getTimeUpdateSetting() {
        return timeUpdateSetting;
    }

    public static int getDistanceUpdateSetting() {
        return distanceUpdateSetting;
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
        if (processedArgs.target() == null) {
            MyLogger.logWarning("missing top/server/player selection");
            return null;
        }

        return switch (processedArgs.target()) {
            case PLAYER -> getPlayerStatResult(processedArgs);
            case SERVER -> getServerStatResult(processedArgs);
            case TOP -> getTopStatResult(processedArgs);
        };
    }

    private @Nullable String getPlayerStatResult(@NotNull ProcessedArgs processedArgs) {
        StatRequest<Integer> playerRequest = requestHandler.getPlayerRequest(processedArgs);
        if (playerRequest == null) {
            MyLogger.logWarning("playerRequest is null!");
            return null;
        }
        updateCache(playerRequest);

        StatType statType = StatType.fromRequest(playerRequest);
        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult != null) {
            int stat = linkedResult.get(processedArgs.playerName());
            if (processedArgs.getNumberOnly()) {
                return processedArgs.shouldFormatNumber() ? getFormattedNumber(stat, statType) : stat + "";
            }
            return getFormattedPlayerStatResult(stat, processedArgs.playerName(), statType);
        }
        else {
            StatResult<Integer> result = playerRequest.execute();
            if (processedArgs.getNumberOnly()) {
                int stat = result.getNumericalValue();
                return processedArgs.shouldFormatNumber() ? getFormattedNumber(stat, statType) : stat + "";
            }
            return result.getFormattedString();
        }
    }

    private @Nullable String getServerStatResult(@NotNull ProcessedArgs processedArgs) {
        StatRequest<Long> serverRequest = requestHandler.getServerRequest(processedArgs);
        if (serverRequest == null) {
            MyLogger.logWarning("serverRequest is null!");
            return null;
        }
        updateCache(serverRequest);

        StatType statType = StatType.fromRequest(serverRequest);
        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult == null) {
            return processingMessage();
        }

        long sum = linkedResult.getSumOfAllValues();
        if (!processedArgs.getNumberOnly()) {
            return getFormattedServerStatResult(sum, statType);
        }
        else if (processedArgs.shouldFormatNumber()) {
            return getFormattedNumber(sum, statType);
        }
        return sum + "";
    }

    private @Nullable String getTopStatResult(ProcessedArgs processedArgs) {
        StatRequest<LinkedHashMap<String, Integer>> topRequest = requestHandler.getTopRequest(processedArgs);
        if (topRequest == null) {
            MyLogger.logWarning("topRequest is null!");
            return null;
        }
        updateCache(topRequest);

        StatType statType = StatType.fromRequest(topRequest);
        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult == null) {
            return processingMessage();
        }

        if (!processedArgs.getNumberOnly()) {
            return getSingleFormattedTopStatLine(linkedResult, processedArgs.topListSize(), statType.statistic());
        }
        else {
            int lineNumber = processedArgs.topListSize();
            long statNumber = linkedResult.getValueAtIndex(lineNumber-1);

            if (processedArgs.shouldFormatNumber()) {
                return getFormattedNumber(statNumber, statType);
            }
            return statNumber + "";
        }
    }

    /** Checks if the {@link StatType} of this StatRequest is already stored in the {@link StatCache},
     and adds it to the cache if not.*/
    private void updateCache(StatRequest<?> statRequest) {
        StatType statType = StatType.fromRequest(statRequest);
        if (!statCache.hasRecordOf(statType)) {
            saveToCache(statRequest);
        }
        else if (statCache.updateIntervalHasPassed()) {
            statCache.update();
        }
    }

    private void saveToCache(StatRequest<?> statRequest) {
        MyLogger.logWarning("(main) saving " + statRequest.getStatisticSetting() + " to the Cache...");
        StatRequest<LinkedHashMap<String, Integer>> newRequest = requestHandler.transformIntoTotalTopRequest(statRequest);
        final CompletableFuture<LinkedStatResult> future =
                CompletableFuture.supplyAsync(() ->
                         new LinkedStatResult(newRequest.execute().getNumericalValue())
                );

        StatType statType = StatType.fromRequest(newRequest);
        statCache.add(statType, future);
    }

    private String getFormattedPlayerStatResult(int statNumber, String playerName, StatType statType) {
        Statistic statistic = statType.statistic();
        String subStatName = statType.getSubStatName();
        TextComponent result = statFormatter.formatPlayerStat(playerName, statNumber, statistic, subStatName);

        return componentToString(result);
    }

    private String getSingleFormattedTopStatLine (LinkedStatResult topStats, int lineNumber, Statistic statistic) {
        String playerName = topStats.getKeyAtIndex(lineNumber-1);
        TextComponent result = statFormatter.formatTopStatLine(lineNumber, playerName, topStats.get(playerName), statistic);

        return componentToString(result);
    }

    private String getFormattedServerStatResult(long statNumber, StatType statType) {
        Statistic statistic = statType.statistic();
        String subStatName = statType.getSubStatName();
        TextComponent result = statFormatter.formatServerStat(statNumber, statistic, subStatName);

        return componentToString(result);
    }

    private String getFormattedNumber(long statNumber, StatType statType) {
        NumberFormatter numberFormatter = statFormatter.getNumberFormatter();
        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
        Unit mainUnit = Unit.getMostSuitableUnit(unitType, statNumber);

        return switch (unitType) {
            case UNTYPED -> numberFormatter.formatNumber(statNumber);
            case TIME -> {
                Unit smallUnit = mainUnit.getSmallerUnit(maxTimeUnits);
                yield numberFormatter.formatTimeNumber(statNumber, mainUnit, smallUnit);
            }
            case DAMAGE -> numberFormatter.formatDamageNumber(statNumber, mainUnit);
            case DISTANCE -> numberFormatter.formatDistanceNumber(statNumber, mainUnit);
        };
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