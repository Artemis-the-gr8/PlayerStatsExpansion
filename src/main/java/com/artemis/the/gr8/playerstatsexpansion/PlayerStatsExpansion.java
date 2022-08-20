package com.artemis.the.gr8.playerstatsexpansion;

import com.artemis.the.gr8.lib.kyori.adventure.text.Component;
import com.artemis.the.gr8.lib.kyori.adventure.text.TextComponent;
import com.artemis.the.gr8.lib.kyori.adventure.text.format.TextColor;
import com.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.artemis.the.gr8.playerstats.api.ApiFormatter;
import com.artemis.the.gr8.playerstats.api.PlayerStats;
import com.artemis.the.gr8.playerstats.api.StatManager;
import com.artemis.the.gr8.playerstats.enums.Unit;
import com.artemis.the.gr8.playerstats.msg.msgutils.NumberFormatter;
import com.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.artemis.the.gr8.playerstats.statistic.result.StatResult;
import com.artemis.the.gr8.playerstatsexpansion.cache.JoinAndQuitListener;
import com.artemis.the.gr8.playerstatsexpansion.cache.StatCache;
import com.artemis.the.gr8.playerstatsexpansion.cache.StatListener;
import com.artemis.the.gr8.playerstatsexpansion.datamodels.LinkedStatResult;
import com.artemis.the.gr8.playerstatsexpansion.datamodels.ProcessedArgs;
import com.artemis.the.gr8.playerstatsexpansion.datamodels.StatType;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;


public final class PlayerStatsExpansion extends PlaceholderExpansion implements Configurable, Cacheable {

    private static ApiFormatter statFormatter;
    private static PlayerStatsExpansion instance;
    private static Config config;

    private RequestHandler requestHandler;
    private StatCache statCache;
    private StatListener statListener;
    private JoinAndQuitListener joinAndQuitListener;


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
        return "1.1.0";
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "PlayerStats";
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> configValues = new LinkedHashMap<>();
        configValues.put("update_interval_in_seconds.distance_statistics", 1);
        configValues.put("update_interval_in_seconds.time_statistics", 1);
        configValues.put("display.max_time_unit", "day");
        configValues.put("display.min_time_unit", "second");
        configValues.put("display.distance_unit", "blocks");
        configValues.put("display.damage_unit", "hearts");
        configValues.put("display.processing_message_color", "#ADE7FF");
        configValues.put("display.only_player_name_color", "");
        configValues.put("display.only_stat_number_color", "");

        return configValues;
    }

    @Override
    public void clear() {
        MyLogger.clear();
        statCache.clear();

        unregisterListeners();
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
        statFormatter = playerStats.getFormatter();

        instance = this;
        config = new Config(this);
        requestHandler = new RequestHandler(statManager);
        statCache = StatCache.getInstance();

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

    private void unregisterListeners() {
        if (statListener != null) {
            HandlerList.unregisterAll(statListener);
        }
        if (joinAndQuitListener != null) {
            HandlerList.unregisterAll(joinAndQuitListener);
        }
    }

    public static Config getConfig() {
        Config localVariable = config;
        if (localVariable != null) {
            return localVariable;
        }

        synchronized (PlayerStatsExpansion.class) {
            if (config == null) {
                config = new Config(instance);
            }
            return config;
        }
    }

    /**format: %playerstats_ (only:number(_raw)|player_name), target(:arg), stat_name:sub_stat_name% */
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
        if (processedArgs.getTitleOnly()) {
            return getTitle(processedArgs);
        }
        else if (processedArgs.target() == null) {
            MyLogger.logWarning("missing top/server/player selection");
            return null;
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

        TextComponent result;
        Unit unit = getDisplayUnitFromStatistic(stat);
        if (unit == null) {
            result = statFormatter.getTopStatTitle(args.topListSize(), stat, args.getSubStatName());
        } else {
            result = statFormatter.getTopStatTitle(args.topListSize(), stat, unit);
        }
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
        int stat;
        if (linkedResult != null) {
            stat = linkedResult.get(args.playerName());
        }
        else {
            StatResult<Integer> result = playerRequest.execute();
            stat = result.getNumericalValue();
        }

        if (args.getNumberOnly()) {
            String number = args.getRawNumber() ? stat + "" : getFormattedNumber(stat, statType);
            return getColoredStatNumber(number);
        }
        else if (args.getPlayerNameOnly()) {
            return getColoredPlayerName(args.playerName());
        }
        return getFormattedPlayerStatResult(stat, args.playerName(), statType);
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
        if (args.getNumberOnly()) {
            String number = args.getRawNumber() ? sum + "" : getFormattedNumber(sum, statType);
            return getColoredStatNumber(number);
        }
        else if (args.getPlayerNameOnly()) {
            return null;
        }
        return getFormattedServerStatResult(sum, statType);
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

        int lineNumber = args.topListSize();
        if (args.getNumberOnly()) {
            long statNumber = linkedResult.getValueAtIndex(lineNumber-1);
            String number = args.getRawNumber() ? statNumber + "" : getFormattedNumber(statNumber, statType);
            return getColoredStatNumber(number);
        }
        else if (args.getPlayerNameOnly()) {
            String playerName = linkedResult.getKeyAtIndex(lineNumber-1);
            return getColoredPlayerName(playerName);
        }
        return getSingleFormattedTopStatLine(linkedResult, args.topListSize(), statType.statistic());
    }

    /**
     * Checks if the {@link StatType} of this StatRequest is already
     * stored in the {@link StatCache}, and adds it to the cache if not.
     */
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
        MyLogger.logInfo("Storing " + statRequest.getStatisticSetting() + " in the cache...");
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
            Unit bigUnit = isNotTooBig(bestUnit) ? bestUnit : config.maxTimeUnit;
            result = statFormatter.formatPlayerStatForTypeTime(playerName, statNumber, statistic, bigUnit, config.minTimeUnit);
        }
        else {
            Unit unit = getDisplayUnitFromStatistic(statistic);
            if (unit == null) {
                String subStatName = statType.getSubStatName();
                result = statFormatter.formatPlayerStat(playerName, statNumber, statistic, subStatName);
            } else {
                result = statFormatter.formatPlayerStat(playerName, statNumber, statistic, unit);
            }
        }
        return componentToString(result);
    }

    private String getSingleFormattedTopStatLine (LinkedStatResult topStats, int lineNumber, Statistic statistic) {
        String playerName = topStats.getKeyAtIndex(lineNumber-1);
        long statNumber = topStats.get(playerName);

        TextComponent result;
        if (Unit.getTypeFromStatistic(statistic) == Unit.Type.TIME) {
            Unit bestUnit = Unit.getMostSuitableUnit(Unit.Type.TIME, statNumber);
            Unit bigUnit = isNotTooBig(bestUnit) ? bestUnit : config.maxTimeUnit;
            result = statFormatter.formatTopStatLineForTypeTime(lineNumber, playerName, statNumber, bigUnit, config.minTimeUnit);
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
            Unit bigUnit = isNotTooBig(bestUnit) ? bestUnit : config.maxTimeUnit;
            result = statFormatter.formatServerStatForTypeTime(statNumber, statistic, bigUnit, config.minTimeUnit);
        }
        else {
            Unit unit = getDisplayUnitFromStatistic(statistic);
            if (unit == null) {
                String subStatName = statType.getSubStatName();
                result = statFormatter.formatServerStat(statNumber, statistic, subStatName);
            } else {
                result = statFormatter.formatServerStat(statNumber, statistic, unit);
            }
        }
        return componentToString(result);
    }

    private String getFormattedNumber(long statNumber, StatType statType) {
        NumberFormatter numberFormatter = statFormatter.getNumberFormatter();
        Unit.Type unitType = Unit.getTypeFromStatistic(statType.statistic());

        return switch (unitType) {
            case UNTYPED -> numberFormatter.formatNumber(statNumber);
            case TIME -> {
                Unit bigTimeUnit = Unit.getMostSuitableUnit(unitType, statNumber);
                Unit bigUnit = isNotTooBig(bigTimeUnit) ? bigTimeUnit : config.maxTimeUnit;
                yield numberFormatter.formatTimeNumber(statNumber, bigUnit, config.minTimeUnit);
            }
            case DAMAGE -> numberFormatter.formatDamageNumber(statNumber, config.damageUnit);
            case DISTANCE -> numberFormatter.formatDistanceNumber(statNumber, config.distanceUnit);
        };
    }

    private String getColoredStatNumber(String statNumber) {
        TextColor color = config.statNumberColor;
        if (color == null) {
            return statNumber;
        }
        TextComponent number = Component.text(statNumber).color(color);
        return componentToString(number);
    }

    private String getColoredPlayerName(String playerName) {
        TextColor color = config.playerNameColor;
        if (color == null) {
            return playerName;
        }
        TextComponent name = Component.text(playerName).color(color);
        return componentToString(name);
    }

    private @Nullable Unit getDisplayUnitFromStatistic(Statistic statistic) {
        Unit.Type unitType = Unit.getTypeFromStatistic(statistic);
        return switch (unitType) {
            case UNTYPED, TIME -> null;
            case DISTANCE -> config.distanceUnit;
            case DAMAGE -> config.damageUnit;
        };
    }

    private boolean isNotTooBig(Unit bigUnit) {
        return switch (config.maxTimeUnit) {
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
        TextColor color = config.processingMsgColor;
        if (color == null) {
            return "Processing...";
        }
        TextComponent msg = Component.text("Processing...").color(color);
        return componentToString(msg);
    }
}