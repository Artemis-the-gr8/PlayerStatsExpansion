package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.gmail.artemis.the.gr8.playerstats.api.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;

public class PlayerStatsExpansion extends PlaceholderExpansion {

    private static PlayerStats playerStats;

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
        }
        return playerStats != null;
    }

    /**format: %playerstats_<\stat_name>_<\sub_stat_name>_<\target>_<\player-name>% */
    @Override
    public String onRequest(OfflinePlayer player, String arg) {
        PlayerStats api = PlayerStats.getAPI();
        if (arg.equalsIgnoreCase("prefix")) {
            return ChatColor.GRAY + "[" + ChatColor.GOLD + "PlayerStats" + ChatColor.GRAY + "]";
        } else if (arg.equalsIgnoreCase("demon")) {
            return Serializer.serialize(MiniMessage.miniMessage().deserialize("<gradient:#f74040:#FF6600:#f74040>fire demon</gradient>"));
        } else if (arg.equalsIgnoreCase("stats")) {
            return api.getTopStats(Statistic.ANIMALS_BRED, null, null).getFormattedString();
        } else if (arg.equalsIgnoreCase("stat")) {
            return api.getPlayerStat(player.getName(), Statistic.KILL_ENTITY, null, EntityType.ZOMBIE).getFormattedString();
        } else if (arg.equalsIgnoreCase("statstring")) {
            return api.getPlayerStat(player.getName(), Statistic.KILL_ENTITY, null, EntityType.ZOMBIE).toString();
        }
        return null;
    }
}