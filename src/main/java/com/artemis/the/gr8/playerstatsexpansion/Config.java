package com.artemis.the.gr8.playerstatsexpansion;

import com.artemis.the.gr8.playerstats.enums.Unit;
import com.artemis.the.gr8.playerstats.lib.kyori.adventure.text.format.NamedTextColor;
import com.artemis.the.gr8.playerstats.lib.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

public class Config {

    private final Pattern chatColorCode;
    private final Map<String, NamedTextColor> namedTextColorMap;

    protected final double distanceUpdateSetting;
    protected final double timeUpdateSetting;
    protected final Unit maxTimeUnit;
    protected final Unit minTimeUnit;
    protected final Unit distanceUnit;
    protected final Unit damageUnit;

    protected final @Nullable TextColor processingMsgColor;
    protected final @Nullable TextColor playerNameColor;
    protected final @Nullable TextColor statNumberColor;

    public Config(@NotNull PlayerStatsExpansion expansion) {
        chatColorCode = Pattern.compile("(?<=&)(\\d|[a-f]|[k-o])");
        namedTextColorMap = getNamedTextColorMap();

        distanceUpdateSetting = expansion.getDouble("update_interval_in_seconds.distance_statistics", 1.0);
        timeUpdateSetting = expansion.getDouble("update_interval_in_seconds.time_statistics", 1.0);
        maxTimeUnit = Unit.fromString(expansion.getString("display.max_time_unit", "day"));
        minTimeUnit = Unit.fromString(expansion.getString("display.min_time_unit", "second"));
        distanceUnit = Unit.fromString(expansion.getString("display.distance_unit", "blocks"));
        damageUnit = Unit.fromString(expansion.getString("display.damage_unit", "hearts"));

        processingMsgColor = getColor(expansion.getString("display.processing_message_color", "#ADE7FF"));
        playerNameColor = getColor(expansion.getString("display.only_player_name_color", ""));
        statNumberColor = getColor(expansion.getString("display.only_stat_number_color", ""));
    }

    public double getDistanceUpdateSetting() {
        return distanceUpdateSetting - 0.01;
    }

    public double getTimeUpdateSetting() {
        return timeUpdateSetting - 0.01;
    }

    private @Nullable TextColor getColor(String configString) {
        if (configString != null && !configString.isEmpty()) {
            if (configString.contains("#")) {
                return TextColor.fromHexString(configString);
            }
            else if (configString.contains("&")) {
                return getColorFromChatColorCode(configString);
            }
            else {
                return NamedTextColor.NAMES.value(configString);
            }
        }
        return null;
    }

    private @Nullable TextColor getColorFromChatColorCode(String configString) {
        Matcher matcher = chatColorCode.matcher(configString);
        if (matcher.find()) {
            String colorCode = matcher.group();
            return namedTextColorMap.get(colorCode);
        }
        return null;
    }

    private @Unmodifiable Map<String, NamedTextColor> getNamedTextColorMap() {
        return Map.ofEntries(
                entry("0", NamedTextColor.BLACK),
                entry("1", NamedTextColor.DARK_BLUE),
                entry("2", NamedTextColor.DARK_GREEN),
                entry("3", NamedTextColor.DARK_AQUA),
                entry("4", NamedTextColor.DARK_RED),
                entry("5", NamedTextColor.DARK_PURPLE),
                entry("6", NamedTextColor.GOLD),
                entry("7", NamedTextColor.GRAY),
                entry("8", NamedTextColor.DARK_GRAY),
                entry("9", NamedTextColor.BLUE),
                entry("a", NamedTextColor.GREEN),
                entry("b", NamedTextColor.AQUA),
                entry("c", NamedTextColor.RED),
                entry("d", NamedTextColor.LIGHT_PURPLE),
                entry("e", NamedTextColor.YELLOW),
                entry("f", NamedTextColor.WHITE)
        );
    }
}