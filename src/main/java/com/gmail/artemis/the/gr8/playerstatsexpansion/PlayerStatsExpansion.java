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
    private static Unit maxTimeUnit;
    private static Unit minTimeUnit;


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
        configValues.put("display.max_time_unit", "day");
        configValues.put("display.min_time_unit", "minute");
        configValues.put("update_interval.distance_statistics", 60);
        configValues.put("update_interval.time_statistics", 60);
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
        maxTimeUnit = Unit.fromString(this.getString("display.max_time_unit", "day"));
        minTimeUnit = Unit.fromString(this.getString("display.min_time_unit", "minute"));

        distanceUpdateSetting = this.getInt("update_interval.distance_statistics", 60);
        timeUpdateSetting = this.getInt("update_interval.time_statistics", 60);
    }

    public static int getTimeUpdateSetting() {
        return timeUpdateSetting;
    }

    public static int getDistanceUpdateSetting() {
        return distanceUpdateSetting;
    }

    /**format: %playerstats_ (title:n), (number:raw), target(:arg), stat_name:sub_stat_name% */
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
        try {
            return getStatResult(args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private @Nullable String getStatResult(String args) {
        ProcessedArgs processedArgs = new ProcessedArgs(args);
        if (processedArgs.target() == null) {
            MyLogger.logWarning("missing top/server/player selection");
            return null;
        }
        else if (processedArgs.getTitleOnly()) {
            return getTitle(processedArgs);
        }

        return switch (processedArgs.target()) {
            case PLAYER -> getPlayerStatResult(processedArgs);
            case SERVER -> getServerStatResult(processedArgs);
            case TOP -> getTopStatResult(processedArgs);
        };
    }

    private String getTitle(ProcessedArgs args) {
        Statistic stat = args.getStatistic();
        if (stat == null) {
            return null;
        }
        TextComponent result = statFormatter.getTopStatTitle(args.topListSize(), stat, args.getSubStatName());
        return componentToString(result);
    }

    private @Nullable String getPlayerStatResult(@NotNull ProcessedArgs args) {
        StatRequest<Integer> playerRequest = requestHandler.getPlayerRequest(args);
        if (playerRequest == null) {
            return null;
        }
        updateCache(playerRequest);

        StatType statType = StatType.fromRequest(playerRequest);
        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult != null) {
            int stat = linkedResult.get(args.playerName());
            if (args.getNumberOnly()) {
                return args.shouldFormatNumber() ? getFormattedNumber(stat, statType) : stat + "";
            }
            return getFormattedPlayerStatResult(stat, args.playerName(), statType);
        }
        else {
            StatResult<Integer> result = playerRequest.execute();
            if (args.getNumberOnly()) {
                int stat = result.getNumericalValue();
                return args.shouldFormatNumber() ? getFormattedNumber(stat, statType) : stat + "";
            }
            return result.getFormattedString();
        }
    }

    private @Nullable String getServerStatResult(@NotNull ProcessedArgs args) {
        StatRequest<Long> serverRequest = requestHandler.getServerRequest(args);
        if (serverRequest == null) {
            return null;
        }
        updateCache(serverRequest);

        StatType statType = StatType.fromRequest(serverRequest);
        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult == null) {
            return processingMessage();
        }

        long sum = linkedResult.getSumOfAllValues();
        if (!args.getNumberOnly()) {
            return getFormattedServerStatResult(sum, statType);
        }
        else if (args.shouldFormatNumber()) {
            return getFormattedNumber(sum, statType);
        }
        return sum + "";
    }

    private @Nullable String getTopStatResult(ProcessedArgs args) {
        StatRequest<LinkedHashMap<String, Integer>> topRequest = requestHandler.getTopRequest(args);
        if (topRequest == null) {
            return null;
        }
        updateCache(topRequest);

        StatType statType = StatType.fromRequest(topRequest);
        LinkedStatResult linkedResult = statCache.tryToGetCompletableFutureResult(statType);
        if (linkedResult == null) {
            return processingMessage();
        }

        if (!args.getNumberOnly()) {
            return getSingleFormattedTopStatLine(linkedResult, args.topListSize(), statType.statistic());
        }
        else {
            int lineNumber = args.topListSize();
            long statNumber = linkedResult.getValueAtIndex(lineNumber-1);

            if (args.shouldFormatNumber()) {
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
        else if (statCache.needsUpdatingYet(statType)) {
            statCache.update(statType);
        }
    }

    private void saveToCache(StatRequest<?> statRequest) {
        MyLogger.logInfo("(main) saving " + statRequest.getStatisticSetting() + " to the Cache...");
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

        TextComponent result;
        if (Unit.getTypeFromStatistic(statistic) == Unit.Type.TIME) {
            Unit bestUnit = Unit.getMostSuitableUnit(Unit.Type.TIME, statNumber);
            Unit bigUnit = isNotTooBig(bestUnit) ? bestUnit : maxTimeUnit;
            result = statFormatter.formatPlayerStatForTypeTime(playerName, statNumber, statistic, bigUnit, minTimeUnit);
        }
        else {
            String subStatName = statType.getSubStatName();
            result = statFormatter.formatPlayerStat(playerName, statNumber, statistic, subStatName);
        }
        return componentToString(result);
    }

    private String getSingleFormattedTopStatLine (LinkedStatResult topStats, int lineNumber, Statistic statistic) {
        String playerName = topStats.getKeyAtIndex(lineNumber-1);
        long statNumber = topStats.get(playerName);

        TextComponent result;
        if (Unit.getTypeFromStatistic(statistic) == Unit.Type.TIME) {
            Unit bestUnit = Unit.getMostSuitableUnit(Unit.Type.TIME, statNumber);
            Unit bigUnit = isNotTooBig(bestUnit) ? bestUnit : maxTimeUnit;
            result = statFormatter.formatTopStatLineForTypeTime(lineNumber, playerName, statNumber, bigUnit, minTimeUnit);
        }
        else {
            result = statFormatter.formatTopStatLine(lineNumber, playerName, topStats.get(playerName), statistic);
        }
        return componentToString(result);
    }

    private String getFormattedServerStatResult(long statNumber, StatType statType) {
        Statistic statistic = statType.statistic();

        TextComponent result;
        if (Unit.getTypeFromStatistic(statistic) == Unit.Type.TIME) {
            Unit bestUnit = Unit.getMostSuitableUnit(Unit.Type.TIME, statNumber);
            Unit bigUnit = isNotTooBig(bestUnit) ? bestUnit : maxTimeUnit;
            result = statFormatter.formatServerStatForTypeTime(statNumber, statistic, bigUnit, minTimeUnit);
        }
        else {
            String subStatName = statType.getSubStatName();
            result = statFormatter.formatServerStat(statNumber, statistic, subStatName);
        }
        return componentToString(result);
    }

    private String getFormattedNumber(long statNumber, StatType statType) {
        NumberFormatter numberFormatter = statFormatter.getNumberFormatter();
        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());
        Unit mainUnit = Unit.getMostSuitableUnit(unitType, statNumber);

        return switch (unitType) {
            case UNTYPED -> numberFormatter.formatNumber(statNumber);
            case TIME -> {
                Unit bigUnit = isNotTooBig(mainUnit) ? mainUnit : maxTimeUnit;
                yield numberFormatter.formatTimeNumber(statNumber, bigUnit, minTimeUnit);
            }
            case DAMAGE -> numberFormatter.formatDamageNumber(statNumber, mainUnit);
            case DISTANCE -> numberFormatter.formatDistanceNumber(statNumber, mainUnit);
        };
    }

    private boolean isNotTooBig(Unit bigUnit) {
        return switch (maxTimeUnit) {
            case DAY -> true;
            case HOUR -> bigUnit != Unit.DAY;
            case MINUTE -> !(bigUnit == Unit.HOUR || bigUnit == Unit.DAY);
            case SECOND -> bigUnit == Unit.SECOND;
            default -> false;
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