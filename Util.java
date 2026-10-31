package me.duel;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Util {
    private Util() {
    }

    public static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "Дуэль" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE;

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void msg(Player p, String s) {
        p.sendMessage(PREFIX + c(s));
    }

    public static ItemStack named(Material m, int amount, String name, String... lore) {
        ItemStack it = new ItemStack(m, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(c(name));
            if (lore.length > 0) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(c(s));
                meta.setLore(l);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack ench(ItemStack it, Enchantment e, int level) {
        it.addUnsafeEnchantment(e, level);
        return it;
    }

    public static void give(Player p, ItemStack it) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack r : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), r);
        }
    }

    public static ItemStack splashHeal(int amount) {
        ItemStack it = new ItemStack(Material.SPLASH_POTION, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta pm = (PotionMeta) meta;
            pm.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1), true);
            pm.setColor(Color.RED);
            pm.setDisplayName(c("&cЗелье лечения"));
            it.setItemMeta(pm);
        }
        return it;
    }

    public static void perm(Player p, PotionEffectType type, int amplifier) {
        p.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false));
    }

    public static void armor(Player p, String prefix) {
        p.getInventory().setHelmet(new ItemStack(Material.valueOf(prefix + "_HELMET")));
        p.getInventory().setChestplate(new ItemStack(Material.valueOf(prefix + "_CHESTPLATE")));
        p.getInventory().setLeggings(new ItemStack(Material.valueOf(prefix + "_LEGGINGS")));
        p.getInventory().setBoots(new ItemStack(Material.valueOf(prefix + "_BOOTS")));
    }

    public static void setAttr(Player p, Attribute a, double value) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst != null) inst.setBaseValue(value);
    }

    public static void addAttr(Player p, Attribute a, double delta) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst != null) inst.setBaseValue(inst.getBaseValue() + delta);
    }

    public static void resetAttrs(Player p) {
        setAttr(p, Attribute.GENERIC_MAX_HEALTH, 20.0);
        setAttr(p, Attribute.GENERIC_ATTACK_DAMAGE, 1.0);
        setAttr(p, Attribute.GENERIC_ATTACK_SPEED, 4.0);
        setAttr(p, Attribute.GENERIC_MOVEMENT_SPEED, 0.1);
        setAttr(p, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.0);
        setAttr(p, Attribute.GENERIC_ARMOR, 0.0);
    }

    public static double maxHealth(Player p) {
        AttributeInstance inst = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return inst == null ? 20.0 : inst.getValue();
    }

    public static void heal(Player p, double amount) {
        p.setHealth(Math.min(maxHealth(p), p.getHealth() + amount));
    }

    public static void addMaxHealth(Player p, double amount) {
        addAttr(p, Attribute.GENERIC_MAX_HEALTH, amount);
        heal(p, amount);
    }

    public static String time(int seconds) {
        if (seconds < 0) seconds = 0;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
