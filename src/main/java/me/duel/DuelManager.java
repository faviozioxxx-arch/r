package me.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DuelManager {
    private static class Request {
        UUID from;
        boolean draft;
        BukkitTask expiry;
    }

    private final DuelPlugin plugin;
    private final Map<UUID, Duel> duels = new HashMap<>();
    private final Map<UUID, Request> requests = new HashMap<>();
    /** Игроки, чьи вещи вернутся через пару секунд после конца дуэли. */
    public final Map<UUID, Fighter> pending = new HashMap<>();
    /** true, пока плагин сам телепортирует игроков. */
    public boolean bypassTp = false;

    private final File statsFile;
    private final YamlConfiguration stats;

    public DuelManager(DuelPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.stats = YamlConfiguration.loadConfiguration(statsFile);
    }

    // ------------------------------------------------------------ состояние

    public Duel get(Player p) {
        return duels.get(p.getUniqueId());
    }

    public boolean busy(Player p) {
        return duels.containsKey(p.getUniqueId()) || pending.containsKey(p.getUniqueId());
    }

    public void removeDuel(Duel d) {
        duels.remove(d.a.p.getUniqueId());
        duels.remove(d.b.p.getUniqueId());
    }

    public void teleport(Player p, Location l) {
        boolean prev = bypassTp;
        bypassTp = true;
        try {
            p.teleport(l);
        } finally {
            bypassTp = prev;
        }
    }

    // ------------------------------------------------------------ меню и приглашения

    public void openMenu(Player p, Player target) {
        if (target.getUniqueId().equals(p.getUniqueId())) {
            Util.msg(p, "&cНельзя вызвать самого себя.");
            return;
        }
        if (busy(p)) {
            Util.msg(p, "&cТы уже в дуэли.");
            return;
        }
        if (busy(target)) {
            Util.msg(p, "&cИгрок &e" + target.getName() + "&c сейчас занят.");
            return;
        }
        p.openInventory(Menus.modeMenu(target));
    }

    public void handleClick(Player p, MenuHolder h, int slot) {
        if (h.type == MenuHolder.Type.MODE) {
            if (slot != Menus.SLOT_LEFT && slot != Menus.SLOT_RIGHT) return;
            boolean draft = slot == Menus.SLOT_RIGHT;
            UUID target = h.target;
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                sendRequest(p, target, draft);
            });
        } else if (h.type == MenuHolder.Type.DRAFT) {
            if (slot != Menus.SLOT_LEFT && slot != Menus.SLOT_RIGHT) return;
            final int idx = slot == Menus.SLOT_LEFT ? 0 : 1;
            Duel d = h.duel;
            Fighter f = h.fighter;
            Bukkit.getScheduler().runTask(plugin, () -> d.pick(f, idx));
        }
    }

    private void sendRequest(Player from, UUID targetId, boolean draft) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            Util.msg(from, "&cИгрок вышел с сервера.");
            return;
        }
        if (busy(from) || busy(target)) {
            Util.msg(from, "&cОдин из игроков уже занят.");
            return;
        }
        if (requests.containsKey(target.getUniqueId())) {
            Util.msg(from, "&cУ игрока уже есть непринятое приглашение.");
            return;
        }
        Request r = new Request();
        r.from = from.getUniqueId();
        r.draft = draft;
        UUID tid = target.getUniqueId();
        int timeout = plugin.getConfig().getInt("request-timeout", 30);
        r.expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Request cur = requests.get(tid);
            if (cur == r) {
                requests.remove(tid);
                Player f = Bukkit.getPlayer(r.from);
                Player t = Bukkit.getPlayer(tid);
                if (f != null) Util.msg(f, "&cПриглашение для &e" + (t != null ? t.getName() : "игрока") + "&c истекло.");
                if (t != null) Util.msg(t, "&cПриглашение истекло.");
            }
        }, timeout * 20L);
        requests.put(tid, r);

        String mode = draft ? "&dнеобычную дуэль (драфт)" : "&eобычную дуэль";
        Util.msg(from, "&fТы вызвал &e" + target.getName() + "&f на " + mode + "&f. Ждём ответа " + timeout + " сек.");
        Util.msg(target, "&e" + from.getName() + "&f вызывает тебя на " + mode + "&f!");
        Component accept = Component.text("[ПРИНЯТЬ]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/duel accept"));
        Component deny = Component.text("[ОТКАЗАТЬ]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/duel deny"));
        target.sendMessage(Component.text().append(accept).append(Component.text("   ")).append(deny).build());
    }

    public void accept(Player target) {
        Request r = requests.remove(target.getUniqueId());
        if (r == null) {
            Util.msg(target, "&cУ тебя нет приглашений.");
            return;
        }
        if (r.expiry != null) r.expiry.cancel();
        Player from = Bukkit.getPlayer(r.from);
        if (from == null || !from.isOnline()) {
            Util.msg(target, "&cИгрок, вызвавший тебя, уже вышел.");
            return;
        }
        if (busy(from) || busy(target)) {
            Util.msg(target, "&cОдин из игроков уже занят.");
            return;
        }
        startDuel(from, target, r.draft);
    }

    public void deny(Player target) {
        Request r = requests.remove(target.getUniqueId());
        if (r == null) {
            Util.msg(target, "&cУ тебя нет приглашений.");
            return;
        }
        if (r.expiry != null) r.expiry.cancel();
        Player from = Bukkit.getPlayer(r.from);
        Util.msg(target, "&7Ты отказался от дуэли.");
        if (from != null) Util.msg(from, "&e" + target.getName() + "&c отказался от дуэли.");
    }

    // ------------------------------------------------------------ запуск

    private void startDuel(Player p1, Player p2, boolean draft) {
        Location[] sp = spawns(p1);
        Duel d = new Duel(plugin, p1, p2, draft);
        duels.put(p1.getUniqueId(), d);
        duels.put(p2.getUniqueId(), d);
        d.start(sp[0], sp[1]);
    }

    private Location[] spawns(Player challenger) {
        Location l1 = plugin.getConfig().getLocation("arena.1");
        Location l2 = plugin.getConfig().getLocation("arena.2");
        if (l1 != null && l2 != null && l1.getWorld() != null && l2.getWorld() != null) {
            return new Location[]{l1, l2};
        }
        // арена не настроена: ставим бойцов друг напротив друга там, где стоит вызвавший
        Location base = challenger.getLocation();
        World w = base.getWorld();
        Vector dir = base.getDirection().setY(0);
        if (dir.lengthSquared() < 0.0001) dir = new Vector(1, 0, 0);
        dir.normalize();
        Location far = base.clone().add(dir.clone().multiply(8));
        far.setY(w.getHighestBlockYAt(far) + 1.0);
        Location first = base.clone();
        first.setDirection(dir);
        far.setDirection(dir.clone().multiply(-1));
        return new Location[]{first, far};
    }

    public void setArena(int n, Location l) {
        plugin.getConfig().set("arena." + n, l);
        plugin.saveConfig();
    }

    // ------------------------------------------------------------ статистика

    public int recordWin(UUID winner, UUID loser) {
        int streak = stats.getInt("streak." + winner, 0) + 1;
        stats.set("streak." + winner, streak);
        stats.set("wins." + winner, stats.getInt("wins." + winner, 0) + 1);
        stats.set("streak." + loser, 0);
        stats.set("losses." + loser, stats.getInt("losses." + loser, 0) + 1);
        saveStats();
        return streak;
    }

    public String statsLine(UUID id) {
        return "Победы: " + stats.getInt("wins." + id, 0) + ", поражения: " + stats.getInt("losses." + id, 0)
                + ", винстрик: " + stats.getInt("streak." + id, 0);
    }

    private void saveStats() {
        try {
            stats.save(statsFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить stats.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ события игроков

    public void onQuit(Player p) {
        UUID id = p.getUniqueId();
        Request own = requests.remove(id);
        if (own != null && own.expiry != null) own.expiry.cancel();
        Iterator<Map.Entry<UUID, Request>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            Request r = it.next().getValue();
            if (r.from.equals(id)) {
                if (r.expiry != null) r.expiry.cancel();
                it.remove();
            }
        }
        Duel d = duels.get(id);
        if (d != null) {
            Fighter leaver = d.fighterOf(p);
            d.end(d.other(leaver), false, leaver);
        }
        Fighter pf = pending.remove(id);
        if (pf != null) pf.restore();
    }

    public void forfeit(Player p) {
        Duel d = get(p);
        if (d == null) {
            Util.msg(p, "&cТы не в дуэли.");
            return;
        }
        Fighter f = d.fighterOf(p);
        d.end(d.other(f), false, null);
    }

    public void shutdown() {
        for (Duel d : new HashSet<>(duels.values())) {
            d.end(null, true, null);
        }
        for (Fighter f : new ArrayList<>(pending.values())) {
            if (f.p.isOnline()) f.restore();
        }
        pending.clear();
        for (Request r : requests.values()) {
            if (r.expiry != null) r.expiry.cancel();
        }
        requests.clear();
    }
}
