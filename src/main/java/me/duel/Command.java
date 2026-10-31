package me.duel;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final DuelPlugin plugin;

    public DuelCommand(DuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков.");
            return true;
        }
        Player p = (Player) sender;
        DuelManager mgr = plugin.manager;
        if (!p.hasPermission("duel.use")) {
            Util.msg(p, "&cНет прав.");
            return true;
        }
        if (args.length == 0) {
            Util.msg(p, "&fИспользование: &e/duel <ник>&f, &e/duel accept&f, &e/duel deny&f, &e/duel leave&f, &e/duel stats");
            return true;
        }
        String a0 = args[0].toLowerCase();
        switch (a0) {
            case "accept":
                mgr.accept(p);
                return true;
            case "deny":
                mgr.deny(p);
                return true;
            case "leave":
                mgr.forfeit(p);
                return true;
            case "stats":
                Util.msg(p, mgr.statsLine(p.getUniqueId()));
                return true;
            case "setarena":
                if (!p.hasPermission("duel.admin")) {
                    Util.msg(p, "&cНет прав.");
                    return true;
                }
                if (args.length < 2 || !(args[1].equals("1") || args[1].equals("2"))) {
                    Util.msg(p, "&fИспользование: &e/duel setarena 1&f или &e/duel setarena 2&f (встань на нужную точку)");
                    return true;
                }
                mgr.setArena(Integer.parseInt(args[1]), p.getLocation());
                Util.msg(p, "&aТочка арены " + args[1] + " сохранена.");
                return true;
            default:
                break;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Util.msg(p, "&cИгрок &e" + args[0] + "&c не найден онлайн.");
            return true;
        }
        mgr.openMenu(p, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String pre = args[0].toLowerCase();
            List<String> subs = new ArrayList<>(Arrays.asList("accept", "deny", "leave", "stats"));
            if (sender.hasPermission("duel.admin")) subs.add("setarena");
            for (String s : subs) if (s.startsWith(pre)) out.add(s);
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(pre) && !pl.getName().equals(sender.getName())) {
                    out.add(pl.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setarena")) {
            out.add("1");
            out.add("2");
        }
        return out;
    }
}
