package com.gmail.artemis.the.gr8.playerstatsexpansion;

import com.gmail.artemis.the.gr8.lib.kyori.adventure.platform.bukkit.BukkitAudiences;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.Component;
import com.gmail.artemis.the.gr8.lib.kyori.adventure.text.minimessage.MiniMessage;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TestListener implements Listener {

    private static int testProgress;

    private static BukkitAudiences adventure;
    private static ConcurrentHashMap<Integer, Statistic> getStat;
    private static ConcurrentHashMap<Statistic, Integer> getID;

    public TestListener() {
        createStatIDs();
        new StatListener();
        Bukkit.getPluginManager().registerEvents(this, PlaceholderAPIPlugin.getInstance());
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        sendTaskLater(() -> test(event.getPlayer()));
    }

    private static void sendTaskLater(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLaterAsynchronously(PlaceholderAPIPlugin.getInstance(), 20);
    }

    private void test(Player player) {
        sendNextTask(player);
    }

    public static void sendNextTask(Player player) {
        int statID = getNextTaskID();
        Statistic stat = getStatfromID(statID);
        String message = statID + ": " + stat;

        sendTaskLater(() ->
                adventure().player(player).sendMessage(getPrettyMessage(message)));
    }

    public static int getCurrentTaskID() {
        return testProgress;
    }

    private static int getNextTaskID() {
        return testProgress += 1;
    }

    public static Statistic getStatfromID(int ID) {
        return getStat.get(ID);
    }

    public static int getIDfromStat(Statistic stat) {
        return getID.get(stat);
    }

    public static BukkitAudiences adventure() {
        if (adventure != null) {
            return adventure;
        }
        adventure = BukkitAudiences.create(PlaceholderAPIPlugin.getInstance());
        return adventure;
    }

    private void createStatIDs() {
        AtomicInteger id = new AtomicInteger();
        getID = new ConcurrentHashMap<>();
        getStat = new ConcurrentHashMap<>();

        Arrays.stream(Statistic.values())
                .forEach(stat -> {
                    getID.put(stat, id.incrementAndGet());
                    getStat.put(id.get(), stat);
                });
    }

    public static Component getPrettyMessage(String statNoAndName) {
        Random random = new Random();
        int choice = random.nextInt(11);
        String msg = switch (choice) {
            case 1 -> "<gradient:#7402d1:#e31bc5:#7402d1>" +statNoAndName + "</gradient>";
            case 2 -> "<gradient:#f74040:#FF6600:#f74040>" + statNoAndName + "</gradient>";
            case 3 -> "<gradient:blue:#b01bd1:blue>" + statNoAndName + "</gradient>";
            case 4 -> "<gradient:#f73bdb:#fc8bec:#f73bdb>" + statNoAndName + "</gradient>";
            case 5 -> "<gradient:gold:#fc7f03:-1>" + statNoAndName + "</gradient>";
            case 6 -> "<gradient:blue:#03befc:blue>" + statNoAndName + "</gradient>";
            case 7 -> "<gradient:#03b6fc:#f73bdb>" + statNoAndName + "</gradient>";
            case 8 -> "<gradient:gold:#00ff7b:#03b6fc>" + statNoAndName + "</gradient>";
            case 9 -> "<gradient:gold:#ff245e:#a511f0:#7c0aff>" + statNoAndName + "</gradient>";
            case 10 -> "<gradient:#14f7a0:#4287f5>" + statNoAndName + "</gradient>";
            default -> "<gradient:#00ff7b:#03befc:blue>" + statNoAndName + "</gradient>";
        };
        return MiniMessage.miniMessage().deserialize(msg);
    }
}