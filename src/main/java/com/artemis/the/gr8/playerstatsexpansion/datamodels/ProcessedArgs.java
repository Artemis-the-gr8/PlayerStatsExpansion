package com.artemis.the.gr8.playerstatsexpansion.datamodels;

import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstatsexpansion.MyLogger;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProcessedArgs {

    private final static Pattern targetPattern;
    private final static Pattern targetTitlePattern;
    private final static Pattern targetTopArgPattern;
    private final static Pattern targetPlayerArgPattern;

    private boolean isTitleRequest;
    private boolean isNumberRequest;
    private boolean numberRequestIsRaw;
    private boolean isPlayerNameRequest;

    private Target target;
    private int topListSize;
    private String playerName;

    private final String[] statIdentifiers;

    static {
        targetPattern = Pattern.compile("(title)|(top:)|(player:)|(server)|(me)");
        targetTitlePattern = Pattern.compile("(title):?(\\d+)?");
        targetTopArgPattern = Pattern.compile("(?<=:)\\d+");
        targetPlayerArgPattern = Pattern.compile("(?<=:)\\w{3,16}");
    }

    public ProcessedArgs(OfflinePlayer player, String args) throws IllegalArgumentException {
        String[] argsToProcess = splitAroundCommas(args);
        String[] whiteSpaceStrippedArgs = stripWhiteSpaces(argsToProcess);
        String[] leftoverArgs = extractAllKeywords(player, whiteSpaceStrippedArgs);
        statIdentifiers = leftoverArgs[0].split(":");
    }

    public boolean getTitleOnly() {
        return isTitleRequest;
    }

    public boolean getNumberOnly() {
        return isNumberRequest;
    }

    public boolean getRawNumber() {
        return numberRequestIsRaw;
    }

    public boolean getPlayerNameOnly() {
        return isPlayerNameRequest;
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
            return Statistic.valueOf(statIdentifiers[0].toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable Material getMaterialSubStat() {
        if (statIdentifiers.length < 2) {
            return null;
        }
        return Material.matchMaterial(statIdentifiers[1]);
    }

    public @Nullable EntityType getEntitySubStat() {
        if (statIdentifiers.length < 2) {
            return null;
        }
        try {
            return EntityType.valueOf(statIdentifiers[1].toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable String getSubStatName() {
        if (statIdentifiers.length < 2) {
            return null;
        }
        return statIdentifiers[1];
    }

    private String[] splitAroundCommas(String argsToProcess) throws IllegalArgumentException {
        if (!argsToProcess.contains(",")) {
            MyLogger.logWarning("Arguments need to be separated by commas!");
            throw new IllegalArgumentException();
        }
        return argsToProcess.split(",");
    }

    private String[] stripWhiteSpaces(String[] argsToProcess) {
        return Arrays.stream(argsToProcess)
                .parallel()
                .map(arg -> arg.replaceAll(" ", ""))
                .toArray(String[]::new);
    }

    private String[] extractAllKeywords(OfflinePlayer player, String[] argsToProcess) {
        String[] argsToProcessMinusOptionalKeywords = extractOptionalKeywords(argsToProcess);
        return extractTargetAndTargetArgs(player, argsToProcessMinusOptionalKeywords);
    }

    private String[] extractOptionalKeywords(@NotNull String[] argsToProcess) {
        for (String arg : argsToProcess) {
            if (arg.contains("only:")) {
                if (arg.startsWith("only:number")) {
                    isNumberRequest = true;
                }
                if (arg.equalsIgnoreCase("only:number_raw")) {
                    numberRequestIsRaw = true;
                }

                else if (arg.startsWith("only:player")) {
                    isPlayerNameRequest = true;
                }
                return Arrays.stream(argsToProcess)
                        .parallel()
                        .filter(string -> !(string.equalsIgnoreCase(arg)))
                        .toArray(String[]::new);
            }
        }
        return argsToProcess;
    }

    private String[] extractTargetAndTargetArgs(@Nullable OfflinePlayer player, @NotNull String[] argsToProcess) {
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
                else if (targetTitlePattern.matcher(arg).find()) {
                    isTitleRequest = true;
                    topListSize = silentlyFindTopListSize(arg);
                }
                else if (arg.equalsIgnoreCase("me") && player != null) {
                    target = Target.PLAYER;
                    playerName = player.getName();
                }
                else {
                    target = Target.SERVER;
                }
                return Arrays.stream(argsToProcess)
                        .parallel()
                        .filter(string -> !(string.equalsIgnoreCase(arg)))
                        .toArray(String[]::new);
            }
        }
        return argsToProcess;
    }

    private int findTopListSize(String targetArg) {
        int topListSize = silentlyFindTopListSize(targetArg);
        if (topListSize == 0) {
            MyLogger.logWarning("No valid line-number found for top-selection!");
            return 1;
        }
        return topListSize;
    }

    private int silentlyFindTopListSize(String targetArg) {
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
        return 0;
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