package me.duel;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class MenuHolder implements InventoryHolder {
    public enum Type { MODE, DRAFT }

    public final Type type;
    public final UUID target;
    public final Duel duel;
    public final Fighter fighter;
    private Inventory inventory;

    public MenuHolder(Type type, UUID target, Duel duel, Fighter fighter) {
        this.type = type;
        this.target = target;
        this.duel = duel;
        this.fighter = fighter;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
