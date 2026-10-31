package me.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Duel {
    public enum State { COUNTDOWN, FIGHT, PICK, ENDED }

    private static final Rarity[] ORDER = {Rarity.COMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY};

    public final DuelPlugin plugin;
    public final Fighter a;
    public final Fighter b;
    public final boolean draft;
    public final Set<Block> placed = new HashSet<>();
    public State state = State.COUNTDOWN;
    /** true, пока урон наносится самими способностями (чтобы они не запускали друг друга). */
    public boolean internal = false;
    public int elapsed = 0;

    private final Random rnd = new Random();
    private BossBar bar;
    private BukkitTask task;
    private int countdown;
    private int draftLeft;
    private int pickLeft;
    private int draftNo;
    private Rarity nextRarity = Rarity.RARE;
    private Rarity pickRarity = Rarity.COMMON;

    public Duel(DuelPlugin plugin, Player p1, Player p2, boolean draft) {
        this.plugin = plugin;
        this.a = new Fighter(plugin, p1);
        this.b = new Fighter(plugin, p2);
        this.draft = draft;
    }

    public Fighter fighterOf(Player p) {
        if (a.p.getUniqueId().equals(p.getUniqueId())) return a;
        if (b.p.getUniqueId().equals(p.getUniqueId())) return b;
        return null;
    }

    public Fighter other(Fighter f) {
        return f == a ? b : a;
    }

    // ------------------------------------------------------------ старт

    public void start(Location l1, Location l2) {
        countdown = plugin.getConfig().getInt("countdown", 5);
        a.save();
        b.save();
        prepare(a);
        prepare(b);
        plugin.manager.teleport(a.p, l1);
        plugin.manager.teleport(b.p, l2);
        giveKit(a);
        giveKit(b);

        bar = Bukkit.createBossBar(ChatColor.YELLOW + "Подготовка...", BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(a.p);
        bar.addPlayer(b.p);

        String mode = draft ? ChatColor.LIGHT_PURPLE + "Необычная дуэль (драфт)" : ChatColor.YELLOW + "Обычная дуэль";
        Util.msg(a.p, "&fДуэль против &e" + b.p.getName() + "&f: " + mode);
        Util.msg(b.p, "&fДуэль против &e" + a.p.getName() + "&f: " + mode);

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void prepare(Fighter f) {
        Player p = f.p;
        p.closeInventory();
        p.getInventory().clear();
        p.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(e.getType());
        }
        Util.resetAttrs(p);
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
    }

    private void giveKit(Fighter f) {
        Player p = f.p;
        if (draft) {
            p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
            p.getInventory().addItem(Util.splashHeal(1));
            p.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 16));
            p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));
        } else {
            Util.armor(p, "IRON");
            p.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            p.getInventory().addItem(new ItemStack(Material.BOW));
            p.getInventory().addItem(new ItemStack(Material.ARROW, 16));
            p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
            p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
            p.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        }
    }

    // ------------------------------------------------------------ тик (раз в секунду)

    private void tick() {
        if (state == State.ENDED) return;
        switch (state) {
            case COUNTDOWN:
                if (countdown > 0) {
                    String t = ChatColor.YELLOW + "" + ChatColor.BOLD + countdown;
                    for (Fighter f : new Fighter[]{a, b}) {
                        f.p.sendTitle(t, ChatColor.GRAY + "Приготовься...", 0, 25, 5);
                        f.p.playSound(f.p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    bar.setTitle(ChatColor.YELLOW + "Бой начнётся через " + countdown);
                    countdown--;
                } else {
                    beginFight();
                }
                break;
            case PICK:
                elapsed++;
                pickLeft--;
                bar.setProgress(clamp(pickLeft / (double) plugin.getConfig().getInt("draft.pick-time", 12)));
                for (Fighter f : new Fighter[]{a, b}) {
                    if (!f.picked && f.draftInv != null) Menus.updateClock(f.draftInv, Math.max(pickLeft, 0));
                }
                if (pickLeft <= 0) {
                    for (Fighter f : new Fighter[]{a, b}) {
                        if (!f.picked && !f.options.isEmpty()) pick(f, rnd.nextInt(f.options.size()));
                    }
                }
                checkTimeLimit();
                break;
            case FIGHT:
                elapsed++;
                Abilities.tick(this, a, b);
                if (state != State.FIGHT) return;
                Abilities.tick(this, b, a);
                if (state != State.FIGHT) return;
                if (draft) {
                    draftLeft--;
                    bar.setColor(nextRarity.barColor);
                    bar.setTitle(ChatColor.WHITE + "СЛЕДУЮЩИЙ ДРАФТ: " + nextRarity.color + "" + ChatColor.BOLD
                            + nextRarity.display + ChatColor.GRAY + " (" + Util.time(draftLeft) + ")");
                    bar.setProgress(clamp(draftLeft / (double) plugin.getConfig().getInt("draft.interval", 30)));
                    if (draftLeft <= 0) {
                        startPick(nextRarity);
                        return;
                    }
                } else {
                    int left = plugin.getConfig().getInt("max-time", 600) - elapsed;
                    bar.setColor(BarColor.RED);
                    bar.setTitle(ChatColor.WHITE + "ДУЭЛЬ " + ChatColor.GRAY + "(" + Util.time(left) + ")");
                    bar.setProgress(clamp(left / (double) plugin.getConfig().getInt("max-time", 600)));
                }
                checkTimeLimit();
                break;
            default:
                break;
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private void checkTimeLimit() {
        if (state != State.ENDED && elapsed >= plugin.getConfig().getInt("max-time", 600)) {
            end(null, false, null);
        }
    }

    private void beginFight() {
        for (Fighter f : new Fighter[]{a, b}) {
            f.p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "БОЙ!", "", 0, 20, 10);
            f.p.playSound(f.p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
        }
        if (draft) {
            draftNo = 0;
            startPick(Rarity.COMMON);
        } else {
            state = State.FIGHT;
        }
    }

    // ------------------------------------------------------------ драфт

    private Rarity rarityFor(int n) {
        return n < ORDER.length ? ORDER[n] : Rarity.random();
    }

    private void startPick(Rarity r) {
        state = State.PICK;
        pickRarity = r;
        pickLeft = plugin.getConfig().getInt("draft.pick-time", 12);
        bar.setColor(BarColor.PURPLE);
        bar.setTitle(ChatColor.LIGHT_PURPLE + "ИДЁТ ДРАФТ...");
        bar.setProgress(1.0);
        for (Fighter f : new Fighter[]{a, b}) {
            f.picked = false;
            f.options = Upgrades.roll(r, f.ups, 2, rnd);
            f.draftInv = Menus.draftMenu(this, f, r, pickLeft);
            f.p.openInventory(f.draftInv);
            f.p.playSound(f.p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            Util.msg(f.p, "&dДрафт! " + r.color + r.display + " улучшение&f — выбери одно из двух.");
        }
    }

    public void reopen(Fighter f) {
        if (state == State.PICK && !f.picked && f.p.isOnline() && f.draftInv != null) {
            f.p.openInventory(f.draftInv);
        }
    }

    public void pick(Fighter f, int idx) {
        if (state != State.PICK || f.picked) return;
        if (idx < 0 || idx >= f.options.size()) return;
        f.picked = true;
        Upgrade u = f.options.get(idx);
        f.ups.add(u.id);
        if (u.apply != null) u.apply.accept(f.p);
        f.p.closeInventory();
        f.p.playSound(f.p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        Util.msg(f.p, "&fТы выбрал: " + u.rarity.color + ChatColor.BOLD + u.name + ChatColor.RESET + ChatColor.GRAY + " — " + u.desc);
        Fighter o = other(f);
        Util.msg(o.p, "&7Противник выбрал: " + u.rarity.color + u.name);
        if (a.picked && b.picked) endPick();
    }

    private void endPick() {
        state = State.FIGHT;
        draftNo++;
        nextRarity = rarityFor(draftNo);
        draftLeft = plugin.getConfig().getInt("draft.interval", 30);
        for (Fighter f : new Fighter[]{a, b}) {
            f.p.sendTitle("", ChatColor.GRAY + "Игра продолжается...", 0, 30, 10);
        }
    }

    // ------------------------------------------------------------ урон способностей

    /** Урон от способности: source бьёт target. */
    public void hurt(Fighter source, Fighter target, double amount) {
        if (state != State.FIGHT && state != State.PICK) return;
        boolean prev = internal;
        internal = true;
        try {
            target.p.setNoDamageTicks(0);
            target.p.damage(amount, source.p);
        } finally {
            internal = prev;
        }
    }

    // ------------------------------------------------------------ конец

    public void end(Fighter winner, boolean immediate, Fighter leaver) {
        if (state == State.ENDED) return;
        state = State.ENDED;
        if (task != null) task.cancel();
        if (bar != null) bar.removeAll();
        for (Block bl : placed) bl.setType(Material.AIR);
        placed.clear();

        for (Fighter f : new Fighter[]{a, b}) {
            if (f.p.isOnline() && f.p.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                f.p.closeInventory();
            }
        }

        if (winner != null) {
            Fighter loser = other(winner);
            int streak = plugin.manager.recordWin(winner.p.getUniqueId(), loser.p.getUniqueId());
            winner.p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "ПОБЕДА", ChatColor.GOLD + "winstreak " + streak, 5, 50, 10);
            winner.p.playSound(winner.p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            if (loser != leaver) {
                loser.p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ПОРАЖЕНИЕ", ChatColor.GRAY + "Победил " + winner.p.getName(), 5, 50, 10);
            }
            Util.msg(winner.p, "&aТы победил &e" + loser.p.getName() + "&a! Серия побед: &6" + streak);
            if (loser != leaver) Util.msg(loser.p, "&cТы проиграл игроку &e" + winner.p.getName());
        } else {
            for (Fighter f : new Fighter[]{a, b}) {
                f.p.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + "НИЧЬЯ", "", 5, 50, 10);
            }
        }

        plugin.manager.removeDuel(this);

        // игрок, который вышел, или принудительное завершение — возвращаем вещи сразу
        for (Fighter f : new Fighter[]{a, b}) {
            if (immediate || f == leaver) {
                f.restore();
            } else {
                plugin.manager.pending.put(f.p.getUniqueId(), f);
            }
        }
        if (!immediate) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Fighter f : new Fighter[]{a, b}) {
                    Fighter pf = plugin.manager.pending.remove(f.p.getUniqueId());
                    if (pf != null && f.p.isOnline()) pf.restore();
                }
            }, 70L);
        }
    }
}
