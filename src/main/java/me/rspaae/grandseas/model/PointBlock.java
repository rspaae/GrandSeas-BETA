package me.rspaae.grandseas.model;

import org.bukkit.Location;
import org.bukkit.Material;
import java.util.UUID;

public class PointBlock {
    private Location location;
    private Material material;
    private long amount;
    private UUID textDisplayId;

    public PointBlock(Location location, Material material, long amount) {
        this.location = location;
        this.material = material;
        this.amount = amount;
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void addAmount(long add) {
        this.amount += add;
    }

    public void removeAmount(long remove) {
        this.amount -= remove;
    }

    public UUID getTextDisplayId() {
        return textDisplayId;
    }

    public void setTextDisplayId(UUID textDisplayId) {
        this.textDisplayId = textDisplayId;
    }
}
