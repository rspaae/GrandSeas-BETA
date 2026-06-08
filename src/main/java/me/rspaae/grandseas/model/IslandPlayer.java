package me.rspaae.grandseas.model;

import java.util.UUID;

public class IslandPlayer {
    private UUID uuid;
    private Island island;
    private String role;

    public IslandPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Island getIsland() {
        return island;
    }

    public void setIsland(Island island) {
        this.island = island;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
