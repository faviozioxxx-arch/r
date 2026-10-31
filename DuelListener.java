package me.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class DuelListener implements Listener {
    private final DuelPlugin plugin;
    private final DuelManager mgr;

    public DuelListener(DuelPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.manager;
    }

    private static Player damagerPlayer(Entity d) {
        if (d instanceof Player) return (Player) d;
        if (d instanceof Projectile) {
            Object shooter = ((Projectile) d).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    private static MenuHolder holderOf(Inventory inv) {
        if (inv == null) return null;
        InventoryHolder h = inv.getHolder();
        return h instanceof MenuHolder ? (MenuHolder) h : null;
    }

    // ------------------------------------------------------------ меню (забирать ничего нельзя)

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        MenuHolder h = holderOf(e.getView().getTopInventory());
        if (h == null) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        mgr.handleClick((Player) e.getWhoClicked(), h, e.getRawSlot());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (holderOf(e.getView().getTopInventory()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        MenuHolder h = holderOf(e.getInventory());
        if (h == null || h.type != MenuHolder.Type.DRAFT) return;
        if (!(e.getPlayer() instanceof Player)) return;
        final Player p = (Player) e.getPlayer();
        final Duel d = h.duel;
        final Fighter f = h.fighter;
        if (d.state != Duel.State.PICK || f.picked) return;
        // закрыл меню до выбора — открываем снова
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) d.reopen(f);
        }, 2L);
    }

    // ------------------------------------------------------------ урон

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        Duel d = mgr.get(victim);

        if (d == null) {
            if (mgr.pending.containsKey(victim.getUniqueId())) {
                e.setCancelled(true); // после дуэли до возврата вещей урон не проходит
                return;
            }
            // участники дуэли не могут бить посторонних, а посторонние — участников
            if (e instanceof EntityDamageByEntityEvent) {
                Player at = damagerPlayer(((EntityDamageByEntityEvent) e).getDamager());
                if (at != null && mgr.get(at) != null) e.setCancelled(true);
            }
            return;
        }
        if (d.state != Duel.State.FIGHT) {
            e.setCancelled(true);
            return;
        }

        Fighter vf = d.fighterOf(victim);
        Fighter of = d.other(vf);
        Fighter af = null;
        if (e instanceof EntityDamageByEntityEvent) {
            Player at = damagerPlayer(((EntityDamageByEntityEvent) e).getDamager());
            if (at != null) {
                if (!at.getUniqueId().equals(of.p.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
                af = of;
            }
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (!d.internal) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL && (vf.has("no_fall") || vf.has("jump"))) {
                e.setCancelled(true);
                return;
            }
            if (af != null) {
                if (vf.has("dodge") && rnd.nextDouble() < 0.2) {
                    e.setCancelled(true);
                    Util.msg(victim, "&aУВОРОТ!");
                    Util.msg(af.p, "&7" + victim.getName() + " увернулся!");
                    return;
                }
                double mult = 1.0;
                if (af.has("crit_god") && rnd.nextDouble() < 0.35) {
                    mult *= 2.0;
                } else if (af.has("crit") && rnd.nextDouble() < 0.20) {
                    mult *= 1.5;
                }
                if (af.has("berserk") && af.p.getHealth() < Util.maxHealth(af.p) * 0.5) {
                    mult *= 1.3;
                }
                if (mult != 1.0) e.setDamage(e.getDamage() * mult);
            }
        }

        // смертельный удар: дуэль заканчивается без экрана смерти
        double fin = e.getFinalDamage();
        if (victim.getHealth() + victim.getAbsorptionAmount() - fin > 0) return;
        if (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            return; // тотем сработает сам
        }
        e.setCancelled(true);
        if (vf.has("second_chance") && !vf.secondUsed) {
            vf.secondUsed = true;
            victim.setHealth(Math.min(8.0, Util.maxHealth(victim)));
            victim.setFireTicks(0);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 2));
            Util.msg(victim, "&dВторой шанс спас тебя от смерти!");
            Util.msg(of.p, "&7" + victim.getName() + " пережил смертельный удар!");
            return;
        }
        d.end(of, false, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        Duel d = mgr.get(victim);
        if (d == null || d.state != Duel.State.FIGHT || d.internal) return;
        Player at = damagerPlayer(e.getDamager());
        if (at == null) return;
        Fighter vf = d.fighterOf(victim);
        Fighter af = d.fighterOf(at);
        if (af == null || vf == null || af == vf) return;
        double dmg = e.getFinalDamage();
        Abilities.onHit(d, af, vf, dmg);
        if (d.state != Duel.State.FIGHT) return;
        Abilities.onHurt(d, af, vf, dmg);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Duel d = mgr.get(p);
        if (d == null) return;
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
        Fighter f = d.fighterOf(p);
        if (f != null) d.end(d.other(f), false, null);
    }

    // ------------------------------------------------------------ способности

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Duel d = mgr.get(e.getPlayer());
        if (d == null || d.state != Duel.State.FIGHT) return;
        Fighter f = d.fighterOf(e.getPlayer());
        if (f != null && f.has("lasers")) Abilities.laser(d, f, d.other(f));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null) return;
        Duel d = mgr.get(e.getPlayer());
        if (d == null || d.state != Duel.State.FIGHT) return;
        Fighter f = d.fighterOf(e.getPlayer());
        if (f == null) return;
        Abilities.useItem(d, f, d.other(f), it.getType(), () -> e.setCancelled(true));
    }

    // ------------------------------------------------------------ ограничения в дуэли

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Duel d = mgr.get(e.getPlayer());
        if (d == null || d.state != Duel.State.COUNTDOWN) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location n = from.clone();
            n.setYaw(to.getYaw());
            n.setPitch(to.getPitch());
            e.setTo(n);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (mgr.busy(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Duel d = mgr.get(e.getPlayer());
        if (d == null) return;
        if (d.state != Duel.State.FIGHT) {
            e.setCancelled(true);
            return;
        }
        d.placed.add(e.getBlockPlaced());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Duel d = mgr.get(e.getPlayer());
        if (d == null) return;
        if (!d.placed.remove(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (mgr.bypassTp || !mgr.busy(e.getPlayer())) return;
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!mgr.busy(p) || p.hasPermission("duel.admin")) return;
        String m = e.getMessage().toLowerCase();
        if (m.equals("/duel") || m.startsWith("/duel ")) return;
        e.setCancelled(true);
        Util.msg(p, "&cВо время дуэли команды недоступны. Выйти: &e/duel leave");
    }

    // ------------------------------------------------------------ вход / выход

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        mgr.onQuit(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !mgr.busy(p) && Fighter.restoreFromDisk(plugin, p)) {
                Util.msg(p, "&aТвои вещи после прерванной дуэли возвращены.");
            }
        }, 20L);
    }
}
