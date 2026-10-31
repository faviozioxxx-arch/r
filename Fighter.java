package me.duel;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Участник дуэли: хранит выбранные улучшения и сохранённое состояние игрока (инвентарь и т.д.). */
public class Fighter {
    public final DuelPlugin plugin;
    public final Player p;
    public final Set<String> ups = new HashSet<>();
    public final Map<String, Long> cd = new HashMap<>();

    public int hits = 0;
    public boolean secondUsed = false;
    public boolean adrenUsed = false;
    public boolean picked = false;
    public boolean restored = false;
    public List<Upgrade> options = new ArrayList<>();
    public Inventory draftInv;

    private final YamlConfiguration state = new YamlConfiguration();

    public Fighter(DuelPlugin plugin, Player p) {
        this.plugin = plugin;
        this.p = p;
    }

    public boolean has(String id) {
        return ups.contains(id);
    }

    /** true, если способность готова; тогда ставит перезарядку. */
    public boolean ready(String key, long millis) {
        long now = System.currentTimeMillis();
        Long until = cd.get(key);
        if (until != null && until > now) return false;
        cd.put(key, now + millis);
        return true;
    }

    private static File fileFor(DuelPlugin plugin, UUID id) {
        return new File(new File(plugin.getDataFolder(), "saved"), id + ".yml");
    }

    /** Запоминает всё, что нужно вернуть игроку после дуэли, и пишет на диск (на случай краша). */
    public void save() {
        state.set("storage", Arrays.asList(p.getInventory().getStorageContents()));
        state.set("armor", Arrays.asList(p.getInventory().getArmorContents()));
        state.set("extra", Arrays.asList(p.getInventory().getExtraContents()));
        state.set("loc", p.getLocation());
        state.set("gm", p.getGameMode().name());
        state.set("health", p.getHealth());
        state.set("food", p.getFoodLevel());
        state.set("sat", (double) p.getSaturation());
        state.set("effects", new ArrayList<>(p.getActivePotionEffects()));
        File f = fileFor(plugin, p.getUniqueId());
        File dir = f.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку " + dir);
        }
        try {
            state.save(f);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить инвентарь " + p.getName() + ": " + ex.getMessage());
        }
    }

    public void restore() {
        if (restored) return;
        restored = true;
        apply(plugin, p, state);
        File f = fileFor(plugin, p.getUniqueId());
        if (f.exists() && !f.delete()) {
            plugin.getLogger().warning("Не удалось удалить " + f);
        }
    }

    /** Если сервер упал посреди дуэли — возвращаем вещи при входе. */
    public static boolean restoreFromDisk(DuelPlugin plugin, Player p) {
        File f = fileFor(plugin, p.getUniqueId());
        if (!f.exists()) return false;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        apply(plugin, p, y);
        if (!f.delete()) {
            plugin.getLogger().warning("Не удалось удалить " + f);
        }
        return true;
    }

    private static ItemStack[] toArray(List<?> list, int size) {
        ItemStack[] arr = new ItemStack[size];
        if (list == null) return arr;
        for (int i = 0; i < size && i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof ItemStack) arr[i] = (ItemStack) o;
        }
        return arr;
    }

    private static void apply(DuelPlugin plugin, Player p, YamlConfiguration y) {
        p.closeInventory();
        p.getInventory().clear();
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(e.getType());
        }
        Util.resetAttrs(p);
        p.getInventory().setStorageContents(toArray(y.getList("storage"), 36));
        p.getInventory().setArmorContents(toArray(y.getList("armor"), 4));
        p.getInventory().setExtraContents(toArray(y.getList("extra"), 1));
        List<?> effects = y.getList("effects");
        if (effects != null) {
            for (Object o : effects) {
                if (o instanceof PotionEffect) p.addPotionEffect((PotionEffect) o);
            }
        }
        GameMode gm;
        try {
            gm = GameMode.valueOf(y.getString("gm", "SURVIVAL"));
        } catch (IllegalArgumentException ex) {
            gm = GameMode.SURVIVAL;
        }
        p.setGameMode(gm);
        double health = y.getDouble("health", 20.0);
        p.setHealth(Math.max(1.0, Math.min(20.0, health)));
        p.setFoodLevel(y.getInt("food", 20));
        p.setSaturation((float) y.getDouble("sat", 5.0));
        p.setFireTicks(0);
        p.setFallDistance(0f);
        Location loc = y.getLocation("loc");
        if (loc != null && loc.getWorld() != null) {
            plugin.manager.teleport(p, loc);
        }
    }
}
