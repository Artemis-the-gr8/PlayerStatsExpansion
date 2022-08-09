package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.playerstats.enums.Target;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Args {

    private final static Pattern targetPattern;
    private final static Pattern targetTopArgPattern;
    private final static Pattern targetPlayerArgPattern;

    protected boolean isRawNumberRequest;
    protected Target target;
    protected int topListSize;
    protected String playerName;

    private final String[] statIdentifiers;

    static {
        targetPattern = Pattern.compile("(top:)|(player:)|(server)");
        targetTopArgPattern = Pattern.compile("(?<=:)\\d+");
        targetPlayerArgPattern = Pattern.compile("(?<=:)\\w{3,16}");
    }

    //(raw,)   top:n                 stat_name(:sub_stat_name)
    //(raw,)   player:player_name    stat_name(:sub_stat_name)
    //(raw,)   server                stat_name(:sub_stat_name)
    public Args(String args) {
        String[] argsToProcess = args.split(",");
        String[] leftoverArgs = extractAllKeywords(argsToProcess);
        statIdentifiers = leftoverArgs[0].split(":");
    }

    public @Nullable Statistic getStatistic() {
        try {
            return Statistic.valueOf(statIdentifiers[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable Material getMaterialSubStat() {
        if (statIdentifiers.length <= 1) {
            return null;
        }
        return Material.matchMaterial(statIdentifiers[1]);
    }

    public @Nullable EntityType getEntitySubStat() {
        if (statIdentifiers.length <= 1) {
            return null;
        }
        try {
            return EntityType.valueOf(statIdentifiers[1]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String[] extractAllKeywords(String[] argsToProcess) {
        String[] argsWithoutRawKeyword = extractRawKeyword(argsToProcess);
        return extractTargetAndTargetArgs(argsWithoutRawKeyword);
    }

    private String[] extractRawKeyword(String[] argsToProcess) {
        for (String arg : argsToProcess) {
            if (arg.equalsIgnoreCase("raw")) {
                isRawNumberRequest = true;
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
                PlayerStatsExpansion.logWarning("NumberFormatException!");
            } catch (Exception ex) {
                PlayerStatsExpansion.logWarning("Unexpected Exception! " + ex);
            }
        }
        PlayerStatsExpansion.logWarning("No valid rank-number found for top-selection!");
        return 1;
    }

    private String findPlayerName(String playerNameArg) {
        Matcher matcher = targetPlayerArgPattern.matcher(playerNameArg);
        if (matcher.find()) {
            return matcher.group();
        }
        PlayerStatsExpansion.logWarning("No valid player-name found for player-selection!");
        return null;
    }
}