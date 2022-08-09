package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.enums.Target;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProcessedArg {

    private final static Pattern topStatPattern;

    public final boolean isRawNumberRequest;
    public final Target target;
    public final int topListSize;
    public final String playerName;
    private String processedArgs;

    static {
        topStatPattern = Pattern.compile("\\d");
    }

    //pattern: %playerstats_
    //(raw_)    top_      stat-name(:sub-stat-name)   (_n)
    //(raw_)    player_   stat-name(:sub-stat-name)    _player-name
    //(raw_)    server_   stat-name(:sub-stat-name)
    public ProcessedArg(String args) {
        this.processedArgs = args;
        isRawNumberRequest = extractRawKeyword();
        target = extractTarget();
        topListSize = extractTopListSize();
        playerName = extractPlayerName();
    }

    public @Nullable Statistic getStatistic() {
        String[] identifiers = processedArgs.split(":");
        try {
            return Statistic.valueOf(identifiers[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable Material getMaterialSubStat() {
        String[] identifiers = processedArgs.split(":");
        if (identifiers.length <= 1) {
            return null;
        }
        return Material.matchMaterial(identifiers[1]);
    }

    public @Nullable EntityType getEntitySubStat() {
        String[] identifiers = processedArgs.split(":");
        if (identifiers.length <= 1) {
            return null;
        }
        try {
            return EntityType.valueOf(identifiers[1]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Checks if the placeholderString starts with "raw", and if so, removes it.

     @return whether "raw" was found and removed*/
    private boolean extractRawKeyword() {
        boolean getRawNumber = processedArgs.startsWith("raw");
        if (getRawNumber) {
            processedArgs = processedArgs.replaceFirst("raw_", "");
        }
        System.out.println("extractRawKeyWord args: " + processedArgs);
        return getRawNumber;
    }

    /** Attempts to remove and return a player-name from the end of the placeholderString.

     @return the extracted player-name, or null if none were found*/
    private @Nullable String extractPlayerName() {
        int underscoreIndex = processedArgs.lastIndexOf('_');
        if (underscoreIndex != -1) {
            String playerName = processedArgs.substring(underscoreIndex +1);
            processedArgs = processedArgs.replace("_" + playerName, "");
            System.out.println("extractPlayerName args: " + processedArgs);
            return playerName;
        }
        System.out.println("No underscore-index found");
        return null;
    }

    private @Nullable Target extractTarget() {
        System.out.println("before extracting target: " + processedArgs);
        Target target = null;
        if (processedArgs.startsWith("top")) {
            target = Target.TOP;
        } else if (processedArgs.startsWith("server")) {
            target = Target.SERVER;
        } else if (processedArgs.startsWith("player")) {
            target = Target.PLAYER;
        }
        if (target != null) {
            processedArgs = processedArgs.replaceFirst(target.toString().toLowerCase() + "_", "");
            System.out.println("after extracting target: " + processedArgs);
            return target;
        }
        return null;
    }

    /** Attempts to remove and return an integer from the end of the placeholderString.

     @return the extracted int, or 1 if no int was found*/
    private int extractTopListSize() {
        Matcher matcher = topStatPattern.matcher(processedArgs);
        int topListSize;
        try {
            topListSize = Integer.parseInt(matcher.group());
        } catch (NumberFormatException e) {
            topListSize = 1;
        }
        processedArgs = processedArgs.replaceFirst("_" + topListSize, "");
        System.out.println("Extracting topListSize: " + processedArgs);
        return topListSize;
    }
}