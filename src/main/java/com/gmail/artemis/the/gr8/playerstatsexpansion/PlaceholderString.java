package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.enums.Target;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderString {

    private final static Pattern topStatPattern;
    private String args;

    static{
        topStatPattern = Pattern.compile("\\d");
    }

    //pattern: %playerstats_
    //(raw_)    top_      stat-name(:sub-stat-name)   (_n)
    //(raw_)    player_   stat-name(:sub-stat-name)    _player-name
    //(raw_)    server_   stat-name(:sub-stat-name)
    public PlaceholderString(String args) {
        this.args = args;
    }

    /** Checks if the placeholderString starts with "raw", and if so, removes it.

     @return whether "raw" was found and removed*/
    public boolean extractRawKeyword() {
        boolean getRawNumber = args.startsWith("raw");
        if (getRawNumber) {
            args = args.replaceFirst("raw_", "");
        }
        return getRawNumber;
    }

    /** Attempts to remove and return a player-name from the end of the placeholderString.

     @return the extracted player-name, or null if none were found*/
    public @Nullable String extractPlayerName() {
        int underscoreIndex = args.lastIndexOf('_');
        if (underscoreIndex != -1) {
            String playerName = args.substring(underscoreIndex +1);
            args = args.replace("_" + playerName, "");
            return playerName;
        }
        return null;
    }

    public @Nullable Target extractTarget() {
        Target target = null;
        if (args.startsWith("top")) {
            target = Target.TOP;
        } else if (args.startsWith("server")) {
            target = Target.SERVER;
        } else if (args.startsWith("player")) {
            target = Target.PLAYER;
        }
        if (target != null) {
            args = args.replaceFirst(target.toString().toLowerCase(), "");
            return target;
        }
        return null;
    }

    /** Attempts to remove and return an integer from the end of the placeholderString.

     @return the extracted int, or 1 if no int was found*/
    public int extractTopListSize() {
        try {
            int size = getTopListSize(args);
            args = args.replaceFirst("_" + size, "");
            return size;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public @Nullable Statistic getStatistic() {
        String[] identifiers = args.split(":");
        return getStatistic(identifiers[0]);
    }

    public @Nullable Material getMaterialSubStat() {
        String[] identifiers = args.split(":");
        if (identifiers.length <= 1) {
            return null;
        }
        return getMaterial(identifiers[1]);
    }

    public @Nullable EntityType getEntitySubStat() {
        String[] identifiers = args.split(":");
        if (identifiers.length <= 1) {
            return null;
        }
        return getEntityType(identifiers[1]);
    }

    private int getTopListSize(String args) throws NumberFormatException {
        Matcher matcher = topStatPattern.matcher(args);
        return Integer.parseInt(matcher.group());
    }

    private @Nullable Statistic getStatistic(String splitArg) {
        try {
            return Statistic.valueOf(splitArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private @Nullable Material getMaterial(String splitArg) {
        return Material.matchMaterial(splitArg.toUpperCase());
    }

    private @Nullable EntityType getEntityType(String splitArg) {
        try {
            return EntityType.valueOf(splitArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}