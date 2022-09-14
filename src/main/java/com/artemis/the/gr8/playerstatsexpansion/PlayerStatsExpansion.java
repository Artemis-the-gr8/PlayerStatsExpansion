package com.artemis.the.gr8.playerstatsexpansion;

import com.artemis.the.gr8.playerstats.api.PlayerStats;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;


public final class PlayerStatsExpansion extends PlaceholderExpansion implements Configurable, Cacheable {

    private final String requiredPlayerStatsVersion = "1.8";

    private static PlayerStatsExpansion instance;
    private static Config config;
    private PlaceholderProvider placeholderProvider;

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
        return "1.2.0";
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "PlayerStats";
    }

    @Override
    public @NotNull Map<String, Object> getDefaults() {
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
        placeholderProvider.clear();
    }

    @Override
    public boolean canRegister() {
        return Bukkit.getPluginManager().isPluginEnabled("PlayerStats");
    }

    @Override
    public boolean register() {
        if (!canRegister()) {
            return false;
        }

        PlayerStats playerStats;
        try {
            playerStats = PlayerStats.getAPI();
            playerStats.getClass().getMethod("getVersion");
            if (!playerStats.getVersion().equalsIgnoreCase(requiredPlayerStatsVersion)) {
                MyLogger.playerStatsVersionWarning(requiredPlayerStatsVersion);
                return false;
            }
        } catch (IllegalStateException | NoClassDefFoundError | NoSuchMethodException e) {
            MyLogger.playerStatsVersionWarning(requiredPlayerStatsVersion);
            return false;
        }

        instance = this;
        placeholderProvider = new PlaceholderProvider(playerStats);
        return super.register();
    }

    public static @NotNull Config getConfig() {
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
    public @Nullable String onRequest(OfflinePlayer player, String args) {
        if (placeholderProvider == null) {
            return null;
        }
        return placeholderProvider.onRequest(args);
    }
}