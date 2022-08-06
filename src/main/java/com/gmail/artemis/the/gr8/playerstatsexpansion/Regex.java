package com.gmail.artemis.the.gr8.playerstatsexpansion;

import java.util.regex.Pattern;

public final class Regex {

    private final static Pattern requiredPattern;
    private final static Pattern topPattern;

    static{
        requiredPattern = Pattern.compile("^top|server|player", Pattern.CASE_INSENSITIVE);
        topPattern = Pattern.compile("^top[0-9]+", Pattern.CASE_INSENSITIVE);
    }

    //   %playerstats_
    //             top
    //                    _statname                      //should work too
    //                            (:substatname)
    //                             _n                   //will return no 1 by default
    //             server
    //                    _statname
    //                            (:substatname)
    //             player
    //                   _statname
    //                            (:substatname)
    //                             _playername          //will do /me if no playername is specified

    // conditions:
    // * top/player/server at the start: ^top|server|player
    // * raw at the end: $raw


    public static boolean isValid(String args) {
        return requiredPattern.matcher(args).matches();
    }

    public static int isTop(String args) {

    }
}
