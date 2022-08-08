package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.TextComponent;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.gmail.artemis.the.gr8.playerstats.api.*;
import com.gmail.artemis.the.gr8.playerstats.enums.Target;
import com.gmail.artemis.the.gr8.playerstats.statistic.request.StatRequest;
import com.gmail.artemis.the.gr8.playerstats.statistic.result.StatResult;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

public class PlayerStatsExpansion extends PlaceholderExpansion {

    private static PlayerStats playerStats;
    private static StatManager statManager;
    private static Formatter statFormatter;


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
        try {
            playerStats = PlayerStats.getAPI();
        } catch (IllegalStateException e) {
            Bukkit.getLogger().warning("Unable to connect to PlayerStats' API!");
            return false;
        }
        statManager = playerStats.getStatManager();
        statFormatter = playerStats.getFormatter();
        return true;
    }

    /**format: %playerstats_<\stat_name>_<\sub_stat_name>_<\target>_<\player-name>% */
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
            return ComponentToString(prefix);
        }
        return getStatResult(args);
    }

    private String getStatResult(String args) {
        PlaceholderString placeholderArgs = new PlaceholderString(args);
        boolean getRawNumber = placeholderArgs.extractRawKeyword();

        Target target = placeholderArgs.extractTarget();
        if (target == null) {
            logWarning("missing top/server/player selection");
            return null;
        }

        switch (target) {
            case PLAYER -> {
                StatRequest<Integer> playerRequest = getPlayerRequest(placeholderArgs);
                if (playerRequest != null) {
                    return getPlayerStatResult(playerRequest, getRawNumber);
                }
            }
            case SERVER -> {
                //check cache
                //if in cache: convert from cache
                //if not in cache: put in cache
                StatRequest<Long> serverRequest = getServerRequest(placeholderArgs);

            }
            case TOP -> {
                //check cache
                //if in cache: convert from cache
                //if not in cache: put in cache
                StatRequest<LinkedHashMap<String, Integer>> topRequest = getTopRequest(placeholderArgs);
            }
        }
        return null;
    }

    private String getPlayerStatResult(@NotNull StatRequest<Integer> statRequest, boolean getRawNumber) {
        StatResult<Integer> statResult = statRequest.execute();
        if (getRawNumber) {
            return statResult.getNumericalValue().toString();
        }
        return statResult.getFormattedString();
    }

    private String getServerStatResult(@NotNull StatRequest<Long> statRequest, boolean getRawNumber) {
        if (!isLoadedInCache(statRequest.getStatistic())) {
            //calculate and store in cache
        }
        //get long from cache -> method in cache
    }

    private String getTopStatResult(@NotNull StatRequest<LinkedHashMap<String, Integer>> statRequest, boolean getRawNumber) {
        if (!isLoadedInCache(statRequest.getStatistic())) {
            //calculate and store in cache
        }
        //get specific line from cache -> method in cache
    }

    private boolean isLoadedInCache(Statistic statistic) {
        StatCache cache = StatCache.getInstance();
        return cache.hasRecordOf(statistic);
    }

    private @Nullable StatRequest<Integer> getPlayerRequest(PlaceholderString args) {
        String playerName = args.extractPlayerName();
        if (playerName == null) {
            logWarning("missing or invalid player-name");
            return null;
        }

        RequestGenerator<Integer> requestGenerator = statManager.playerStatRequest(playerName);
        return createRequest(requestGenerator, args);
    }

    private @Nullable StatRequest<Long> getServerRequest(PlaceholderString args) {
        RequestGenerator<Long> requestGenerator = statManager.serverStatRequest();
        return createRequest(requestGenerator, args);
    }

    private @Nullable StatRequest<LinkedHashMap<String, Integer>> getTopRequest(PlaceholderString args) {
        int topListSize = args.extractTopListSize();

        RequestGenerator<LinkedHashMap<String, Integer>> requestGenerator = statManager.topStatRequest(topListSize);
        return createRequest(requestGenerator, args);
    }

    private @Nullable <T> StatRequest<T> createRequest(RequestGenerator<T> requestGenerator, PlaceholderString args) {
        Statistic stat = args.getStatistic();
        if (stat == null) {
            logWarning("missing or invalid Statistic");
            return null;
        }

        switch (stat.getType()) {
            case UNTYPED -> {
                return requestGenerator.untyped(stat);
            }
            case BLOCK, ITEM -> {
                Material material = args.getMaterialSubStat();
                if (material == null) {
                    logWarning("missing or invalid Material");
                    return null;
                }
                return requestGenerator.blockOrItemType(stat, material);

            }
            case ENTITY -> {
                EntityType entityType = args.getEntitySubStat();
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

    private @Nullable String ComponentToString(TextComponent component) {
        if (component == null) {
            return null;
        }
        return playerStats.getFormatter().TextComponentToString(component);
    }

    private void logWarning(String msg) {
        Bukkit.getLogger().warning(msg);
    }
}