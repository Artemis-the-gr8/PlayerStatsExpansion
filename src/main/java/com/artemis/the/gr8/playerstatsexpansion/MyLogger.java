package com.artemis.the.gr8.playerstatsexpansion;

import java.util.ArrayList;
import java.util.logging.Logger;

public final class MyLogger {

    private final static ArrayList<String> loggedMessages = new ArrayList<>();
    private final static Logger myLogger = Logger.getLogger("PlayerStatsExpansion");

    public static void logInfo(String msg) {
        myLogger.info(msg);
    }

    public static void logWarning(String msg) {
        if (!loggedMessages.contains(msg)) {
            loggedMessages.add(msg);
            myLogger.warning(msg);
        }
    }

    public static void playerStatsVersionWarning(String version) {
        myLogger.warning("This version of the PlayerStatsExpansion needs at least v" +
                version + " of PlayerStats! " +
                "Download the latest version here: https://www.spigotmc.org/resources/playerstats.102347/");
    }

    public static void clear() {
        loggedMessages.clear();
    }
}