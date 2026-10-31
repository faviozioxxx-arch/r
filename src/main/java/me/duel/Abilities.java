package me.duel;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Логика активных и пассивных способностей улучшений. */
public final class Abilities {
    private Abilities() {
    }

    private static double rnd() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /** Эффекты, которые срабатывают после обычного (не внутреннего) удара. */
    public static void onHit(Duel d, Fighter af, Fighter vf, double dmg) {
        Player a = af.p;
        Player v = vf.p;
        af.hits++;

        if (af.has("heal_hit")) Util.heal(a, 1.0);
        if (af.has("lifesteal")) Util.heal(a, dmg * 0.2);
        if (af.has("vamp_lord")) Util.heal(a, dmg * 0.5);

        if (af.has("fire_hit")) v.setFireTicks(Math.max(v.getFireTicks(), 60));
        if (af.has("flame_blade")) v.setFireTicks(Math.max(v.getFireTicks(), 120));
        if (af.has("poison")) v.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        if (af.has("slow_hit")) v.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
        if (af.has("wither_hit")) v.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));

        if (af.has("lightning") && af.hits % 5 == 0) {
            v.getWorld().strikeLightningEffect(v.getLocation());
            d.hurt(af, vf, 4.0);
        }
        if (d.state != Duel.State.FIGHT) return;

        if (af.has("explosive") && af.hits % 6 == 0) {
            v.getWorld().createExplosion(v.getLocation(), 0F, false, false);
            v.setVelocity(v.getVelocity().add(new Vector(0, 0.6, 0)));
            d.hurt(af, vf, 3.0);
        }
        if (d.state != Duel.State.FIGHT) return;

        if (af.has("web_hit") && rnd() < 0.25) {
            Block b = v.getLocation().getBlock();
            if (b.getType() == Material.AIR) {
                b.setType(Material.COBWEB);
                d.placed.add(b);
                d.plugin.getServer().getScheduler().runTaskLater(d.plugin, () -> {
                    if (d.placed.remove(b) && b.getType() == Material.COBWEB) b.setType(Material.AIR);
                }, 60L);
            }
        }

        if (af.has("shadow") && rnd() < 0.2) {
            Location vl = v.getLocation();
            Vector back = vl.getDirection().setY(0);
            if (back.lengthSquared() > 0.0001) {
                back.normalize().multiply(-1.5);
                Location to = vl.clone().add(back);
                if (to.getBlock().isPassable() && to.clone().add(0, 1, 0).getBlock().isPassable()) {
                    to.setDirection(vl.toVector().subtract(to.toVector()));
                    d.plugin.manager.teleport(a, to);
                    a.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                }
            }
        }
    }

    /** Реакция на полученный удар: шипы и зеркало. */
    public static void onHurt(Duel d, Fighter af, Fighter vf, double dmg) {
        if (vf.has("thorns")) d.hurt(vf, af, Math.max(1.0, dmg * 0.25));
        if (d.state != Duel.State.FIGHT) return;
        if (vf.has("mirror")) d.hurt(vf, af, Math.max(1.0, dmg * 0.4));
    }

    /** Лазеры из глаз по нажатию SHIFT. */
    public static void laser(Duel d, Fighter f, Fighter o) {
        if (!f.ready("laser", 2500)) return;
        Player p = f.p;
        Player t = o.p;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        for (double i = 0.5; i <= 20; i += 0.5) {
            Location pt = eye.clone().add(dir.clone().multiply(i));
            p.getWorld().spawnParticle(Particle.REDSTONE, pt, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.3f));
        }
        p.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1f, 1.6f);
        Vector to = t.getLocation().add(0, 1, 0).toVector().subtract(eye.toVector());
        double dist = to.length();
        if (dist <= 20 && dist > 0.1 && to.normalize().dot(dir) > 0.97 && p.hasLineOfSight(t)) {
            d.hurt(f, o, 4.0);
        }
    }

    /** Предметы-способности (ПКМ). */
    public static void useItem(Duel d, Fighter f, Fighter o, Material type, PlayerInteractEventHolder h) {
        Player p = f.p;
        if (type == Material.FIRE_CHARGE && f.has("fireball")) {
            h.cancel();
            if (!f.ready("fb", 800)) return;
            SmallFireball fb = p.launchProjectile(SmallFireball.class);
            fb.setDirection(p.getLocation().getDirection());
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getAmount() <= 1) {
                p.getInventory().setItemInMainHand(null);
            } else {
                ItemStack rest = hand.clone();
                rest.setAmount(hand.getAmount() - 1);
                p.getInventory().setItemInMainHand(rest);
            }
        } else if (type == Material.FEATHER && f.has("dash")) {
            if (!f.ready("dash", 4000)) {
                Util.msg(p, "&cРывок ещё перезаряжается!");
                return;
            }
            Vector v = p.getLocation().getDirection().multiply(1.8);
            v.setY(Math.max(v.getY(), 0.0) + 0.35);
            p.setVelocity(v);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1f);
        } else if (type == Material.CLOCK && f.has("timestop")) {
            if (!f.ready("timestop", 20000)) {
                Util.msg(p, "&cОстановка времени ещё перезаряжается!");
                return;
            }
            Player t = o.p;
            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 9));
            t.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60, 128));
            Util.msg(t, "&d" + p.getName() + " остановил время! Ты заморожен на 3 сек.");
            Util.msg(p, "&dВремя остановлено!");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        } else if (type == Material.BLAZE_ROD && f.has("dragon")) {
            if (!f.ready("dragon", 8000)) {
                Util.msg(p, "&cДыхание дракона ещё перезаряжается!");
                return;
            }
            Location eye = p.getEyeLocation();
            Vector dir = eye.getDirection().normalize();
            for (double i = 1; i <= 8; i += 0.5) {
                Location pt = eye.clone().add(dir.clone().multiply(i));
                p.getWorld().spawnParticle(Particle.FLAME, pt, 4, i * 0.06, i * 0.06, i * 0.06, 0.01);
            }
            p.getWorld().playSound(eye, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.2f);
            Player t = o.p;
            Vector to = t.getLocation().add(0, 1, 0).toVector().subtract(eye.toVector());
            double dist = to.length();
            if (dist <= 8 && dist > 0.1 && to.normalize().dot(dir) > 0.75) {
                t.setFireTicks(Math.max(t.getFireTicks(), 80));
                d.hurt(f, o, 5.0);
            }
        }
    }

    /** Секундные пассивные способности. */
    public static void tick(Duel d, Fighter f, Fighter o) {
        Player p = f.p;
        if (f.has("adrenaline") && !f.adrenUsed && p.getHealth() <= 6.0) {
            f.adrenUsed = true;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 1));
            Util.msg(p, "&cАдреналин! Скорость III и Сила II на 8 секунд.");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 1f);
        }
        if (f.has("stars") && d.elapsed % 6 == 0) {
            Location l = o.p.getLocation().add(0, 3, 0);
            for (int i = 0; i < 8; i++) {
                o.p.getWorld().spawnParticle(Particle.FLAME, l.clone().add(0, -i * 0.4, 0), 3, 0.1, 0, 0.1, 0.01);
            }
            o.p.getWorld().playSound(o.p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            d.hurt(f, o, 3.0);
        }
    }

    /** Маленькая обёртка, чтобы не тащить событие Bukkit в логику. */
    public interface PlayerInteractEventHolder {
        void cancel();
    }
}
