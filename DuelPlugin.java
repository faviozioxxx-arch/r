package me.duel;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DuelPlugin extends JavaPlugin {
    public DuelManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new DuelManager(this);
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
        PluginCommand cmd = getCommand("duel");
        if (cmd != null) {
            DuelCommand dc = new DuelCommand(this);
            cmd.setExecutor(dc);
            cmd.setTabCompleter(dc);
        }
        getLogger().info("DuelDraft включён. Улучшений в драфте: " + Upgrades.all().size());
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
    }
}
