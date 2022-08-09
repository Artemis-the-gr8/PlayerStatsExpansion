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
    public int topListSize;
    public String playerName;
    private String processedArgs;

    static {
        //find the last occurrence of number(s) in a String
        topStatPattern = Pattern.compile("\\d+(?!.*\\d+)");
    }

    //pattern: %playerstats_
    //(raw,)   top(:n),              stat_name(:sub_stat_name)
    //(raw,)   player:player_name,   stat_name(:sub_stat_name)
    //(raw,)   server,               stat_name(:sub_stat_name)

    //(raw_)    top_      stat-name(:sub-stat-name)   (_n)
    //(raw_)    player_   stat-name(:sub-stat-name)    _player-name
    //(raw_)    server_   stat-name(:sub-stat-name)
    public ProcessedArg(String args) {
        PlayerStatsExpansion.logWarning("ProcessedArg constructor called with args: " + args);
        String[] splitArgs = args.split(",");

        this.processedArgs = args;
        isRawNumberRequest = extractRawKeyword();
        target = extractTarget();

        String topExtraction = "";
        String playerExtraction = "";

        if (target == Target.TOP) {
            topListSize = extractTopListSize();
            topExtraction = "int topListSize: " + topListSize + "\n";
            PlayerStatsExpansion.logWarning("topListSize extracted. ProcessedArgs = " + processedArgs);
        }
        else if (target == Target.PLAYER) {
            playerName = extractPlayerName();
            playerExtraction = "String playerName: " + playerName + "\n";
            PlayerStatsExpansion.logWarning("playerName extracted. ProcessedArgs = " + processedArgs);
        }

        PlayerStatsExpansion.logWarning("At the end of all this, we have values: " + "\n"
                + "boolean isRawNumberRequest: " + isRawNumberRequest + "\n"
                + "Target target: " + target + "\n"
                + topExtraction
                + playerExtraction
                + "String processedArgs: " + processedArgs
        );
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
        return getRawNumber;
    }

    /** Attempts to remove and return a player-name from the end of the placeholderString.

     @return the extracted player-name, or null if none were found*/
    private @Nullable String extractPlayerName() {
        int underscoreIndex = processedArgs.lastIndexOf('_');
        if (underscoreIndex != -1) {
            String playerName = processedArgs.substring(underscoreIndex +1);
            processedArgs = processedArgs.replace("_" + playerName, "");
            return playerName;
        }
        return null;
    }

    private @Nullable Target extractTarget() {
        Target localTarget = null;
        if (processedArgs.startsWith("top")) {
            localTarget = Target.TOP;
        } else if (processedArgs.startsWith("server")) {
            localTarget = Target.SERVER;
        } else if (processedArgs.startsWith("player")) {
            localTarget = Target.PLAYER;
        }
        if (localTarget != null) {
            processedArgs = processedArgs.replaceFirst(localTarget.toString().toLowerCase() + "_", "");
            return localTarget;
        }
        return null;
    }

    /** Attempts to remove and return an integer from the end of the placeholderString.

     @return the extracted int, or 1 if no int was found*/
    private int extractTopListSize() {
        Matcher matcher = topStatPattern.matcher(processedArgs);
        PlayerStatsExpansion.logWarning("Attempting to extract topListSize");
        int topListSize = 1;
        if (matcher.find()) {
            try {
                String match = matcher.group();
                PlayerStatsExpansion.logWarning("Match found: " + match);
                topListSize = Integer.parseInt(match);
            } catch (NumberFormatException e) {
                PlayerStatsExpansion.logWarning("NumberFormatException!");
            } catch (Exception e) {
                PlayerStatsExpansion.logWarning("Unexpected Exception! " + e);
                e.printStackTrace();
            }
            processedArgs = processedArgs.replaceFirst("_" + topListSize, "");
        }
        return topListSize;
    }
}