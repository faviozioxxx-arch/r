package me.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class Menus {
    private Menus() {
    }

    public static final int SLOT_LEFT = 11;
    public static final int SLOT_RIGHT = 15;

    private static void fill(Inventory inv) {
        ItemStack pane = Util.named(Material.GRAY_STAINED_GLASS_PANE, 1, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    /** Меню выбора режима: обычная дуэль / необычная (драфт). Забирать из него ничего нельзя. */
    public static Inventory modeMenu(Player target) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.MODE, target.getUniqueId(), null, null);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Дуэль: " + target.getName());
        holder.setInventory(inv);
        fill(inv);
        inv.setItem(SLOT_LEFT, Util.named(Material.IRON_SWORD, 1, "&e&lОБЫЧНАЯ ДУЭЛЬ",
                "&7Классический бой 1 на 1.",
                "&7У обоих одинаковый набор:",
                "&7железная броня, меч, лук, еда.",
                "",
                "&aНажми, чтобы вызвать " + target.getName()));
        inv.setItem(SLOT_RIGHT, Util.named(Material.NETHER_STAR, 1, "&d&lНЕОБЫЧНАЯ ДУЭЛЬ &7(Драфт)",
                "&7Каждые 30 секунд вам обоим дают",
                "&7на выбор 2 усиления — берёшь одно.",
                "&7Редкость растёт: " + Rarity.COMMON.color + "обычное&7 → " + Rarity.RARE.color + "редкое&7 → "
                        + Rarity.EPIC.color + "эпическое&7 → " + Rarity.LEGENDARY.color + "легендарное",
                "&7Всего " + Upgrades.all().size() + " разных улучшений!",
                "",
                "&aНажми, чтобы вызвать " + target.getName()));
        inv.setItem(22, Util.named(Material.PLAYER_HEAD, 1, "&fПротивник: &e" + target.getName()));
        return inv;
    }

    /** Меню драфта: два варианта улучшения. */
    public static Inventory draftMenu(Duel duel, Fighter f, Rarity rarity, int seconds) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.DRAFT, null, duel, f);
        Inventory inv = Bukkit.createInventory(holder, 27,
                rarity.color + "" + ChatColor.BOLD + rarity.display + " УЛУЧШЕНИЕ");
        holder.setInventory(inv);
        fill(inv);
        updateClock(inv, seconds);
        inv.setItem(22, Util.named(Material.PAPER, 1, "&7Тебе и твоему противнику дают на выбор",
                "&72 усиления, причём выбрать можно только одно!"));
        for (int i = 0; i < f.options.size() && i < 2; i++) {
            Upgrade u = f.options.get(i);
            ItemStack it = new ItemStack(u.icon);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(u.rarity.color + "" + ChatColor.BOLD + u.name);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + u.desc);
                lore.add("");
                lore.add(u.rarity.color + u.rarity.display);
                lore.add(ChatColor.GREEN + "Нажми, чтобы выбрать");
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            inv.setItem(i == 0 ? SLOT_LEFT : SLOT_RIGHT, it);
        }
        return inv;
    }

    public static void updateClock(Inventory inv, int seconds) {
        inv.setItem(4, Util.named(Material.CLOCK, 1, "&eОсталось " + seconds + " сек."));
    }
}
