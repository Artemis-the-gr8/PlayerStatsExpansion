package com.artemis.the.gr8.playerstatsexpansion;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class MyLogger {

    private final static Logger myLogger = Logger.getLogger("PlayerStatsExpansion");
    private final static ArrayList<String> loggedMessages = new ArrayList<>();
    private static Instant storedMessagesLastClearedTimestamp = Instant.now();

    public static void logInfo(String msg) {
        myLogger.info(msg);
    }

    public static void logWarning(String msg) {
        if (storedMessagesNeedClearing()) {
            clearStoredMessages();
        }
        if (!loggedMessages.contains(msg)) {
            loggedMessages.add(msg);
            myLogger.warning(msg);
        }
    }

    public static void playerStatsVersionError() {
        myLogger.warning("You are using v" + PlayerStatsExpansion.VERSION + ", " +
                "which needs PlayerStats v" + PlayerStatsExpansion.NEEDED_PLAYERSTATS_API_VERSION + " to work!" +
                "\n" + "If you are using a newer version of PlayerStats, you need to update the expansion as well, " +
                        "which you can find here: "+ "\n" + "https://api.extendedclip.com/expansions/playerstatsexpansion/" +
                "\n" + "Otherwise, download PlayerStats v" + PlayerStatsExpansion.NEEDED_PLAYERSTATS_API_VERSION +
                " here: https://www.spigotmc.org/resources/playerstats.102347/");
    }

    public static void clear() {
        loggedMessages.clear();
    }

    private static boolean storedMessagesNeedClearing() {
        return storedMessagesLastClearedTimestamp.until(Instant.now(), ChronoUnit.SECONDS) > 30;
    }

    private static void clearStoredMessages() {
        myLogger.info("clearing stored messages...");
        storedMessagesLastClearedTimestamp = Instant.now();
        clear();
    }
}