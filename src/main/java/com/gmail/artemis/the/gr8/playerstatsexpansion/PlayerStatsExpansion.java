package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.TextComponent;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import com.gmail.artemis.the.gr8.playerstats.api.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

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
    public String onRequest(OfflinePlayer player, String args) {
        TextComponent result = switch (args) {
            case "prefix" -> playerStats.getFormatter().getPluginPrefix();
            case "rainbowprefix" -> playerStats.getFormatter().getRainbowPluginPrefix();
            case "prefixtitle" -> playerStats.getFormatter().getPluginPrefixAsTitle();
            case "rainbowprefixtitle" -> playerStats.getFormatter().getRainbowPluginPrefixAsTitle();
            case "demon" -> (TextComponent) MiniMessage.miniMessage().deserialize("<gradient:#f74040:#FF6600:#f74040>fire demon</gradient>");
            default -> null;
        };

        if (result != null) {
            return ComponentToString(result);
        }
        if (!Regex.isValid(args)) {
            return null;
        }

    }

    private String ComponentToString(TextComponent component) {
        if (component == null) {
            return null;
        }
        return playerStats.getFormatter().TextComponentToString(component);
    }
}