package com.gmail.artemis.the.gr8.playerstatsexpansion.datamodels;

import com.gmail.artemis.the.gr8.playerstats.enums.Target;
import com.gmail.artemis.the.gr8.playerstatsexpansion.MyLogger;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessedArgs {

    private final static Pattern targetPattern;
    private final static Pattern targetTopArgPattern;
    private final static Pattern targetPlayerArgPattern;

    private boolean isNumberRequest;
    private boolean formatNumber = true;
    private Target target;
    private int topListSize;
    private String playerName;

    private final String[] statIdentifiers;

    static {
        targetPattern = Pattern.compile("(top:)|(player:)|(server)");
        targetTopArgPattern = Pattern.compile("(?<=:)\\d+");
        targetPlayerArgPattern = Pattern.compile("(?<=:)\\w{3,16}");
    }

    //(number:raw)   top:n                 stat_name(:sub_stat_name)
    //(number:raw)   player:player_name    stat_name(:sub_stat_name)
    //(number:raw)   server                stat_name(:sub_stat_name)
    public ProcessedArgs(String args) {
        String[] argsToProcess = args.split(",");
        String[] leftoverArgs = extractAllKeywords(argsToProcess);
        statIdentifiers = leftoverArgs[0].split(":");
    }

    public boolean getNumberOnly() {
        return isNumberRequest;
    }

    public boolean shouldFormatNumber() {
        return formatNumber;
    }

    public Target target() {
        return target;
    }

    public int topListSize() {
        return topListSize;
    }

    public String playerName() {
        return playerName;
    }

    public @Nullable Statistic getStatistic() {
        try {
            return Statistic.valueOf(statIdentifiers[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable Material getMaterialSubStat() {
        return Material.matchMaterial(statIdentifiers[1]);
    }

    public @Nullable EntityType getEntitySubStat() {
        try {
            return EntityType.valueOf(statIdentifiers[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String[] extractAllKeywords(String[] argsToProcess) {
        String[] argsWithoutNumberKeywords = extractNumberKeywords(argsToProcess);
        return extractTargetAndTargetArgs(argsWithoutNumberKeywords);
    }

    private String[] extractNumberKeywords(String[] argsToProcess) {
        for (String arg : argsToProcess) {
            if (arg.contains("number")) {
                if (arg.equalsIgnoreCase("number")) {
                    isNumberRequest = true;
                } else if (arg.equalsIgnoreCase("number:raw")) {
                    formatNumber = false;
                }
                return Arrays.stream(argsToProcess)
                        .filter(string -> !(string.equalsIgnoreCase(arg)))
                        .toArray(String[]::new);
            }
        }
        return argsToProcess;
    }

    private String[] extractTargetAndTargetArgs(String[] argsToProcess) {
        for (String arg : argsToProcess) {
            Matcher matcher = targetPattern.matcher(arg);
            if (matcher.find()) {
                if (arg.startsWith("top:")) {
                    target = Target.TOP;
                    topListSize = findTopListSize(arg);
                }
                else if (arg.contains("player:")) {
                    target = Target.PLAYER;
                    playerName = findPlayerName(arg);
                }
                else {
                    target = Target.SERVER;
                }
                return Arrays.stream(argsToProcess)
                        .filter(string -> !(string.equalsIgnoreCase(arg)))
                        .toArray(String[]::new);
            }
        }
        return argsToProcess;
    }

    private int findTopListSize(String targetArg) {
        Matcher matcher = targetTopArgPattern.matcher(targetArg);
        if (matcher.find()) {
            try {
                String match = matcher.group();
                return Integer.parseInt(match);
            } catch (NumberFormatException e) {
                MyLogger.logWarning("NumberFormatException!");
            } catch (Exception ex) {
                MyLogger.logWarning("Unexpected Exception! " + ex);
            }
        }
        MyLogger.logWarning("No valid line-number found for top-selection!");
        return 1;
    }

    private String findPlayerName(String playerNameArg) {
        Matcher matcher = targetPlayerArgPattern.matcher(playerNameArg);
        if (matcher.find()) {
            return matcher.group();
        }
        MyLogger.logWarning("No valid player-name found for player-selection!");
        return null;
    }
}