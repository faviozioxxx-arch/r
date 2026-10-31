package me.duel;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class Upgrade {
    public final String id;
    public final String name;
    public final String desc;
    public final Rarity rarity;
    public final Material icon;
    public final Consumer<Player> apply;

    public Upgrade(String id, Rarity rarity, Material icon, String name, String desc, Consumer<Player> apply) {
        this.id = id;
        this.rarity = rarity;
        this.icon = icon;
        this.name = name;
        this.desc = desc;
        this.apply = apply;
    }
}
