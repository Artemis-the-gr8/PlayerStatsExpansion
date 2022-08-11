package com.gmail.artemis.the.gr8.playerstatsexpansion;

import java.util.ArrayList;
import java.util.logging.Logger;

public final class MyLogger {

    private final static ArrayList<String> loggedMessages = new ArrayList<>();

    public static void logWarning(String msg) {
        if (!loggedMessages.contains(msg)) {
            loggedMessages.add(msg);
            Logger myLogger = Logger.getLogger("PlayerStatsExpansion");
            myLogger.warning(msg);
        }
    }

    public static void clear() {
        loggedMessages.clear();
    }
}
