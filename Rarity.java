package me.duel;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

import java.util.concurrent.ThreadLocalRandom;

public enum Rarity {
    COMMON("ОБЫЧНОЕ", ChatColor.WHITE, BarColor.WHITE, 50),
    RARE("РЕДКОЕ", ChatColor.GREEN, BarColor.GREEN, 30),
    EPIC("ЭПИЧЕСКОЕ", ChatColor.LIGHT_PURPLE, BarColor.PURPLE, 15),
    LEGENDARY("ЛЕГЕНДАРНОЕ", ChatColor.GOLD, BarColor.YELLOW, 5);

    public final String display;
    public final ChatColor color;
    public final BarColor barColor;
    public final int weight;

    Rarity(String display, ChatColor color, BarColor barColor, int weight) {
        this.display = display;
        this.color = color;
        this.barColor = barColor;
        this.weight = weight;
    }

    public static Rarity random() {
        int total = 0;
        for (Rarity r : values()) total += r.weight;
        int x = ThreadLocalRandom.current().nextInt(total);
        for (Rarity r : values()) {
            x -= r.weight;
            if (x < 0) return r;
        }
        return COMMON;
    }
}
