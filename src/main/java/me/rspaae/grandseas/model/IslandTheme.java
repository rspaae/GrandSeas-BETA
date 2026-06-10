package me.rspaae.grandseas.model;

import org.bukkit.Material;

public enum IslandTheme {
    CLASSIC(Material.GRASS_BLOCK, "Classic Island", "Standard island with grass and an oak tree.", "starter_classic"),
    SANDY(Material.SAND, "Sandy Island", "Tropical-style sandy island.", "starter_sandy"),
    ROCKY(Material.COBBLESTONE, "Rocky Island", "Rocky island with very little soil.", "starter_rocky");

    private final Material icon;
    private final String displayName;
    private final String description;
    private final String structureName;

    IslandTheme(Material icon, String displayName, String description, String structureName) {
        this.icon = icon;
        this.displayName = displayName;
        this.description = description;
        this.structureName = structureName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getStructureName() {
        return structureName;
    }
}
