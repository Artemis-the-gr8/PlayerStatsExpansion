package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.Component;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

public final class StatListener implements Listener {

    private static StatCache statCache;

    public StatListener() {
        statCache = StatCache.getInstance();
        Bukkit.getPluginManager().registerEvents(this, PlaceholderAPIPlugin.getInstance());
    }

    @EventHandler
    public void onStatisticIncrementEvent(PlayerStatisticIncrementEvent event) {
        Statistic stat = event.getStatistic();
        Player player = event.getPlayer();
        String message = TestListener.getIDfromStat(stat) + ". " + stat;
        TestListener.adventure().console().sendMessage(defaultMessage(message));

        if (TestListener.getIDfromStat(stat) == TestListener.getCurrentTaskID()) {
            TestListener.adventure().player(player).sendMessage(completedMessage());
            TestListener.sendNextTask(player);
        }
        else if (stat == Statistic.ENDERCHEST_OPENED) {
            TestListener.adventure().player(player).sendMessage(skipMessage());
            TestListener.sendNextTask(player);
        }
    }

    private Component completedMessage() {
        return MiniMessage.miniMessage().deserialize("<gradient:#fae105:#0cf0b7>You did it!</gradient>");
    }

    private Component skipMessage() {
        return MiniMessage.miniMessage().deserialize("<gradient:#e60ede:#f79900:#f5dd0a>Skipping ahead...</gradient>");
    }

    private Component defaultMessage(String message) {
        return MiniMessage.miniMessage().deserialize("<gradient:#f07a0c:red>" + message + "</gradient>");
    }
}