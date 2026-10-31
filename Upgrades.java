package me.duel;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Все улучшения драфта (64 штуки). Пассивные эффекты (флаги) обрабатываются в Abilities и DuelListener
 * по id улучшения, а предметы / эффекты выдаются сразу в apply.
 */
public final class Upgrades {
    private Upgrades() {
    }

    private static final Map<String, Upgrade> BY_ID = new LinkedHashMap<>();

    public static List<Upgrade> all() {
        return new ArrayList<>(BY_ID.values());
    }

    public static Upgrade get(String id) {
        return BY_ID.get(id);
    }

    /** Выбирает n разных улучшений заданной редкости, которых у игрока ещё нет. */
    public static List<Upgrade> roll(Rarity rarity, Set<String> owned, int n, Random rnd) {
        List<Upgrade> pool = new ArrayList<>();
        for (Upgrade u : BY_ID.values()) {
            if (u.rarity == rarity && !owned.contains(u.id)) pool.add(u);
        }
        if (pool.size() < n) {
            for (Upgrade u : BY_ID.values()) {
                if (!owned.contains(u.id) && !pool.contains(u)) pool.add(u);
            }
        }
        if (pool.size() < n) {
            for (Upgrade u : BY_ID.values()) {
                if (!pool.contains(u)) pool.add(u);
            }
        }
        Collections.shuffle(pool, rnd);
        return new ArrayList<>(pool.subList(0, Math.min(n, pool.size())));
    }

    private static void r(String id, Rarity rarity, Material icon, String name, String desc, Consumer<Player> apply) {
        BY_ID.put(id, new Upgrade(id, rarity, icon, name, desc, apply));
    }

    private static void give(Player p, ItemStack it) {
        Util.give(p, it);
    }

    static {
        // ================= ОБЫЧНЫЕ (17) =================
        r("iron_set", Rarity.COMMON, Material.IRON_CHESTPLATE, "Сет железки",
                "Полный комплект железной брони", p -> Util.armor(p, "IRON"));
        r("heal_hit", Rarity.COMMON, Material.GLISTERING_MELON_SLICE, "Лечение при атаке",
                "Каждый удар лечит тебя на 1 HP", null);
        r("gapples", Rarity.COMMON, Material.GOLDEN_APPLE, "Золотые яблоки",
                "3 золотых яблока", p -> give(p, new ItemStack(Material.GOLDEN_APPLE, 3)));
        r("speed1", Rarity.COMMON, Material.SUGAR, "Быстрые ноги",
                "Скорость I до конца дуэли", p -> Util.perm(p, PotionEffectType.SPEED, 0));
        r("iron_sword", Rarity.COMMON, Material.IRON_SWORD, "Железный меч",
                "Железный меч в инвентарь", p -> give(p, new ItemStack(Material.IRON_SWORD)));
        r("bow", Rarity.COMMON, Material.BOW, "Лук и стрелы",
                "Лук и 16 стрел", p -> {
                    give(p, new ItemStack(Material.BOW));
                    give(p, new ItemStack(Material.ARROW, 16));
                });
        r("shield", Rarity.COMMON, Material.SHIELD, "Щит",
                "Щит в левую руку", p -> p.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD)));
        r("steak", Rarity.COMMON, Material.COOKED_BEEF, "Стейки",
                "16 жареных стейков", p -> give(p, new ItemStack(Material.COOKED_BEEF, 16)));
        r("hearts", Rarity.COMMON, Material.RED_DYE, "Дополнительные сердца",
                "+2 сердца к максимуму здоровья", p -> Util.addMaxHealth(p, 4.0));
        r("sharp1", Rarity.COMMON, Material.GRINDSTONE, "Точильный камень",
                "+1 к урону рукой и оружием", p -> Util.addAttr(p, Attribute.GENERIC_ATTACK_DAMAGE, 1.0));
        r("fire_hit", Rarity.COMMON, Material.FLINT_AND_STEEL, "Поджигатель",
                "Удары поджигают врага на 3 сек", null);
        r("no_fall", Rarity.COMMON, Material.FEATHER, "Лёгкие ботинки",
                "Ты не получаешь урон от падения", null);
        r("heal_pots", Rarity.COMMON, Material.POTION, "Зелья лечения",
                "2 взрывных зелья лечения", p -> give(p, Util.splashHeal(2)));
        r("cobweb", Rarity.COMMON, Material.COBWEB, "Паутина",
                "6 паутин для ловушек", p -> give(p, new ItemStack(Material.COBWEB, 6)));
        r("pearls", Rarity.COMMON, Material.ENDER_PEARL, "Жемчуг Эндера",
                "2 жемчуга Эндера", p -> give(p, new ItemStack(Material.ENDER_PEARL, 2)));
        r("kb_stick", Rarity.COMMON, Material.STICK, "Палка отбрасывания",
                "Палка с Отбрасыванием II", p -> give(p, Util.ench(
                        Util.named(Material.STICK, 1, "&fПалка отбрасывания"), Enchantment.KNOCKBACK, 2)));
        r("blocks", Rarity.COMMON, Material.STONE, "Строитель",
                "48 блоков камня", p -> give(p, new ItemStack(Material.STONE, 48)));

        // ================= РЕДКИЕ (18) =================
        r("poison", Rarity.RARE, Material.SPIDER_EYE, "Ядовитые удары",
                "Удары отравляют врага на 3 сек", null);
        r("strength1", Rarity.RARE, Material.BLAZE_POWDER, "Сила",
                "Сила I до конца дуэли", p -> Util.perm(p, PotionEffectType.INCREASE_DAMAGE, 0));
        r("regen", Rarity.RARE, Material.GHAST_TEAR, "Регенерация",
                "Регенерация I до конца дуэли", p -> Util.perm(p, PotionEffectType.REGENERATION, 0));
        r("lifesteal", Rarity.RARE, Material.REDSTONE, "Вампиризм",
                "Лечишься на 20% от нанесённого урона", null);
        r("no_kb", Rarity.RARE, Material.LEATHER_CHESTPLATE, "Неотталкиваемая куртка",
                "Тебя невозможно отбросить", p -> {
                    p.getInventory().setChestplate(Util.named(Material.LEATHER_CHESTPLATE, 1, "&bНеотталкиваемая куртка"));
                    Util.setAttr(p, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
                });
        r("diamond_chest", Rarity.RARE, Material.DIAMOND_CHESTPLATE, "Алмазный нагрудник",
                "Алмазный нагрудник", p -> p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE)));
        r("crossbow", Rarity.RARE, Material.CROSSBOW, "Арбалет",
                "Арбалет с Быстрой зарядкой II", p -> {
                    give(p, Util.ench(new ItemStack(Material.CROSSBOW), Enchantment.QUICK_CHARGE, 2));
                    give(p, new ItemStack(Material.ARROW, 16));
                });
        r("thorns", Rarity.RARE, Material.CACTUS, "Шипы",
                "Враг получает 25% урона обратно", null);
        r("dodge", Rarity.RARE, Material.LEATHER_BOOTS, "Уворот",
                "20% шанс полностью избежать удара", null);
        r("slow_hit", Rarity.RARE, Material.PACKED_ICE, "Ледяной клинок",
                "Удары замедляют врага на 2 сек", null);
        r("wither_hit", Rarity.RARE, Material.WITHER_ROSE, "Иссушение",
                "Удары накладывают Иссушение на 3 сек", null);
        r("jump", Rarity.RARE, Material.SLIME_BALL, "Прыгун",
                "Прыгучесть II и нет урона от падения", p -> Util.perm(p, PotionEffectType.JUMP, 1));
        r("fire_res", Rarity.RARE, Material.MAGMA_CREAM, "Огнеупорность",
                "Полный иммунитет к огню", p -> Util.perm(p, PotionEffectType.FIRE_RESISTANCE, 0));
        r("absorb", Rarity.RARE, Material.GOLD_INGOT, "Золотое сердце",
                "Поглощение III (+3 сердца сверху)", p -> Util.perm(p, PotionEffectType.ABSORPTION, 2));
        r("egapple", Rarity.RARE, Material.ENCHANTED_GOLDEN_APPLE, "Зачарованное яблоко",
                "1 зачарованное золотое яблоко", p -> give(p, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE)));
        r("fireball", Rarity.RARE, Material.FIRE_CHARGE, "Огненные шары",
                "8 огненных шаров (ПКМ — бросить)", p -> give(p,
                        Util.named(Material.FIRE_CHARGE, 8, "&6Огненный шар", "&7ПКМ — запустить")));
        r("fast_atk", Rarity.RARE, Material.CLOCK, "Быстрая атака",
                "Скорость атаки +1.5", p -> Util.addAttr(p, Attribute.GENERIC_ATTACK_SPEED, 1.5));
        r("chain_set", Rarity.RARE, Material.CHAINMAIL_CHESTPLATE, "Кольчуга",
                "Полный комплект кольчуги", p -> Util.armor(p, "CHAINMAIL"));

        // ================= ЭПИЧЕСКИЕ (16) =================
        r("lasers", Rarity.EPIC, Material.END_ROD, "ШИФТ — лазеры из глаз",
                "Нажми SHIFT, глядя на врага (4 урона)", null);
        r("lightning", Rarity.EPIC, Material.LIGHTNING_ROD, "Громовой удар",
                "Каждый 5-й удар бьёт молнией (+4)", null);
        r("crit", Rarity.EPIC, Material.IRON_AXE, "Критический удар",
                "20% шанс нанести x1.5 урона", null);
        r("diamond_set", Rarity.EPIC, Material.DIAMOND_CHESTPLATE, "Алмазный сет",
                "Полный комплект алмазной брони", p -> Util.armor(p, "DIAMOND"));
        r("titan", Rarity.EPIC, Material.TOTEM_OF_UNDYING, "Титан",
                "+5 сердец к максимуму здоровья", p -> Util.addMaxHealth(p, 10.0));
        r("berserk", Rarity.EPIC, Material.NETHERITE_AXE, "Берсерк",
                "+30% урона, когда здоровья меньше половины", null);
        r("second_chance", Rarity.EPIC, Material.HEART_OF_THE_SEA, "Второй шанс",
                "Один раз выживаешь при смертельном ударе", null);
        r("ghost", Rarity.EPIC, Material.PHANTOM_MEMBRANE, "Призрак",
                "Невидимость и Скорость II", p -> {
                    Util.perm(p, PotionEffectType.INVISIBILITY, 0);
                    Util.perm(p, PotionEffectType.SPEED, 1);
                });
        r("dash", Rarity.EPIC, Material.FEATHER, "Рывок",
                "Перо: ПКМ — рывок вперёд (КД 4 сек)", p -> give(p,
                        Util.named(Material.FEATHER, 1, "&bРывок", "&7ПКМ — рывок вперёд")));
        r("hard_blade", Rarity.EPIC, Material.DIAMOND_SWORD, "Закалённый клинок",
                "Алмазный меч: Острота II, Огонь I", p -> give(p, Util.ench(Util.ench(
                        new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 2), Enchantment.FIRE_ASPECT, 1)));
        r("power_bow", Rarity.EPIC, Material.BOW, "Лук Мощь III",
                "Лук: Мощь III, Отбрасывание I + 24 стрелы", p -> {
                    give(p, Util.ench(Util.ench(new ItemStack(Material.BOW),
                            Enchantment.ARROW_DAMAGE, 3), Enchantment.ARROW_KNOCKBACK, 1));
                    give(p, new ItemStack(Material.ARROW, 24));
                });
        r("trident", Rarity.EPIC, Material.TRIDENT, "Трезубец",
                "Трезубец с Верностью III", p -> give(p, Util.ench(new ItemStack(Material.TRIDENT), Enchantment.LOYALTY, 3)));
        r("web_hit", Rarity.EPIC, Material.STRING, "Паутинные удары",
                "25% шанс опутать врага паутиной", null);
        r("explosive", Rarity.EPIC, Material.TNT, "Взрывные удары",
                "Каждый 6-й удар взрывает врага (+3)", null);
        r("mirror", Rarity.EPIC, Material.GLASS, "Зеркало урона",
                "Враг получает 40% нанесённого тебе урона", null);
        r("adrenaline", Rarity.EPIC, Material.HONEY_BOTTLE, "Адреналин",
                "При 3 сердцах: Скорость III и Сила II на 8 сек", null);

        // ================= ЛЕГЕНДАРНЫЕ (13) =================
        r("netherite_set", Rarity.LEGENDARY, Material.NETHERITE_CHESTPLATE, "Незеритовый сет",
                "Полный комплект незеритовой брони", p -> Util.armor(p, "NETHERITE"));
        r("totem", Rarity.LEGENDARY, Material.TOTEM_OF_UNDYING, "Тотем бессмертия",
                "Тотем в левой руке", p -> p.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING)));
        r("god_sword", Rarity.LEGENDARY, Material.NETHERITE_SWORD, "Меч богов",
                "Незеритовый меч: Острота V, Огонь II", p -> give(p, Util.ench(Util.ench(
                        Util.named(Material.NETHERITE_SWORD, 1, "&6Меч богов"), Enchantment.DAMAGE_ALL, 5),
                        Enchantment.FIRE_ASPECT, 2)));
        r("giant", Rarity.LEGENDARY, Material.IRON_BLOCK, "Гигант",
                "+8 сердец и сопротивление отбрасыванию", p -> {
                    Util.addMaxHealth(p, 16.0);
                    Util.setAttr(p, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
                });
        r("vamp_lord", Rarity.LEGENDARY, Material.NETHER_WART, "Король вампиров",
                "Лечишься на 50% от нанесённого урона", null);
        r("stars", Rarity.LEGENDARY, Material.NETHER_STAR, "Падающие звёзды",
                "Каждые 6 сек звезда бьёт врага (3 урона)", null);
        r("timestop", Rarity.LEGENDARY, Material.CLOCK, "Остановка времени",
                "Часы: ПКМ — враг замирает на 3 сек (КД 20)", p -> give(p,
                        Util.named(Material.CLOCK, 1, "&dОстановка времени", "&7ПКМ — заморозить врага")));
        r("shadow", Rarity.LEGENDARY, Material.ENDER_EYE, "Теневой шаг",
                "20% шанс телепортироваться за спину врага", null);
        r("dragon", Rarity.LEGENDARY, Material.BLAZE_ROD, "Дыхание дракона",
                "Жезл: ПКМ — огненный конус (КД 8 сек)", p -> give(p,
                        Util.named(Material.BLAZE_ROD, 1, "&6Дыхание дракона", "&7ПКМ — огненное дыхание")));
        r("immortal", Rarity.LEGENDARY, Material.DIAMOND_BLOCK, "Бессмертный",
                "Сопротивление II и Регенерация I", p -> {
                    Util.perm(p, PotionEffectType.DAMAGE_RESISTANCE, 1);
                    Util.perm(p, PotionEffectType.REGENERATION, 0);
                });
        r("crit_god", Rarity.LEGENDARY, Material.NETHERITE_SCRAP, "Мастер критов",
                "35% шанс нанести двойной урон", null);
        r("feast", Rarity.LEGENDARY, Material.CAKE, "Пир богов",
                "8 золотых яблок и 2 зачарованных", p -> {
                    give(p, new ItemStack(Material.GOLDEN_APPLE, 8));
                    give(p, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                });
        r("flame_blade", Rarity.LEGENDARY, Material.BLAZE_ROD, "Огненный клинок",
                "Удары жгут 6 сек, ты неуязвим к огню", p -> Util.perm(p, PotionEffectType.FIRE_RESISTANCE, 0));
    }
}
