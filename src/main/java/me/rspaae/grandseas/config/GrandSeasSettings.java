package me.rspaae.grandseas.config;

import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed access to config.yml (AcidIsland-style layout + grandseas extensions).
 */
public class GrandSeasSettings {

    private static GrandSeasSettings instance;

    private final FileConfiguration config;

    public GrandSeasSettings(FileConfiguration config) {
        this.config = config;
    }

    public static GrandSeasSettings get() {
        return instance;
    }

    public static void bind(FileConfiguration config) {
        instance = new GrandSeasSettings(config);
    }

    // --- Acid ---

    public boolean isAcidDamageOp() {
        return config.getBoolean("acid.damage-op", true);
    }

    public boolean isAcidDamageChickens() {
        return config.getBoolean("acid.damage-chickens", false);
    }

    public double getAcidPlayerDamage() {
        return firstDouble(
                config.getDouble("acid.damage.acid.player", -1),
                config.getDouble("acid.damage", -1),
                10.0
        );
    }

    public double getAcidMonsterDamage() {
        return config.getDouble("acid.damage.acid.monster", 5.0);
    }

    public double getAcidAnimalDamage() {
        return config.getDouble("acid.damage.acid.animal", 5.0);
    }

    public int getAcidItemDestroySeconds() {
        return config.getInt("acid.damage.acid.item", 0);
    }

    public double getAcidRainDamage() {
        return config.getDouble("acid.damage.rain", 1.0);
    }

    public boolean isAcidSnowDamage() {
        return config.getBoolean("acid.damage.snow", false);
    }

    public int getAcidBurnDelaySeconds() {
        return config.getInt("acid.damage.delay", 2);
    }

    public long getAcidTickInterval() {
        return firstLong(
                config.getLong("acid.damage.tick-interval", -1),
                config.getLong("acid.interval", -1),
                20L
        );
    }

    public boolean isHelmetProtectsFromRain() {
        return config.getBoolean("acid.protection.helmet", false);
    }

    public boolean isFullArmorProtectsFromAcid() {
        return config.getBoolean("acid.protection.full-armor", false);
    }

    public int getAcidEffectDurationSeconds() {
        return config.getInt("acid.acid-effect-duration", 30);
    }

    public int getRainEffectDurationSeconds() {
        return config.getInt("acid.rain-effect-duration", 10);
    }

    public List<PotionEffectType> getAcidEffects() {
        return parseEffects(config.getStringList("acid.effects"));
    }

    public List<PotionEffectType> getRainEffects() {
        return parseEffects(config.getStringList("acid.rain-effects"));
    }

    public boolean isIslandRespawnEnabled() {
        return config.getBoolean("world.flags.ISLAND_RESPAWN", true);
    }

    // --- World ---

    public String getFriendlyWorldName() {
        return config.getString("world.friendly-name", "GrandSeas");
    }

    public String getWorldName() {
        return firstString(
                config.getString("world.world-name"),
                config.getString("world.name"),
                "grandseasWorld"
        );
    }

    public Difficulty getWorldDifficulty() {
        try {
            return Difficulty.valueOf(config.getString("world.difficulty", "NORMAL").toUpperCase());
        } catch (IllegalArgumentException e) {
            return Difficulty.NORMAL;
        }
    }

    public int getDistanceBetweenIslands() {
        return firstInt(
                config.getInt("world.distance-between-islands", -1),
                config.getInt("island.grid-spacing", -1),
                64
        );
    }

    public int getProtectionRange() {
        return config.getInt("world.protection-range", 50);
    }

    public int getDefaultBorderDiameter() {
        int fromLegacy = config.getInt("island.default-size", -1);
        if (fromLegacy > 0) {
            return fromLegacy;
        }
        return getProtectionRange() * 2;
    }

    public int getSeaHeight() {
        return firstInt(
                config.getInt("world.sea-height", -1),
                config.getInt("world.sea-level", -1),
                64
        );
    }

    public int getIslandHeight() {
        return config.getInt("world.island-height", 60);
    }

    public int getStartX() {
        return config.getInt("world.start-x", 0);
    }

    public int getStartZ() {
        return config.getInt("world.start-z", 0);
    }

    public int getOffsetX() {
        return config.getInt("world.offset-x", 0);
    }

    public int getOffsetZ() {
        return config.getInt("world.offset-z", 0);
    }

    public Material getWaterBlock() {
        String block = config.getString("world.water-block", "WATER");
        Material mat = Material.getMaterial(block.toUpperCase());
        return mat != null ? mat : Material.WATER;
    }

    public boolean isOceanFloor() {
        return config.getBoolean("world.ocean-floor", true);
    }

    public boolean isMakeCaves() {
        return config.getBoolean("world.make-caves", false);
    }

    public boolean isMakeDecorations() {
        return config.getBoolean("world.make-decorations", true);
    }

    public boolean isMakeStructures() {
        return config.getBoolean("world.make-structures", false);
    }

    public boolean isDefaultSetting(String key, boolean fallback) {
        return config.getBoolean("world.default-island-settings." + key, fallback);
    }

    // --- Island team ---

    public int getMaxTeamSize() {
        return firstInt(
                config.getInt("island.max-team-size", -1),
                config.getInt("island.max-members", -1),
                4
        );
    }

    public int getMaxTrustedSize() {
        return config.getInt("grandseas.island.max-trusted-size", 10);
    }

    public int getInviteTimeoutSeconds() {
        return firstInt(
                config.getInt("island.invite-timeout", -1),
                config.getInt("team.invite-timeout", -1),
                60
        );
    }

    // --- GrandSeas extensions ---

    public List<String> getStarterChestItems() {
        return config.getStringList("grandseas.island.starter-chest-items");
    }

    public int getMaxBorderLevel() {
        return config.getInt("grandseas.island.max-border-level",
                config.getInt("island.max-border-level", 5));
    }

    public int getBorderSizePerLevel() {
        return config.getInt("grandseas.island.border-size-per-level",
                config.getInt("island.border-size-per-level", 15));
    }

    public int getMaxGeneratorLevel() {
        return 5;
    }

    public int getDefaultBlockValue() {
        return config.getInt("grandseas.points.default-block-value",
                config.getInt("points.default-block-value", 0));
    }

    public long getPointsAutoCalcInterval() {
        return config.getLong("grandseas.points.auto-calc-interval",
                config.getLong("points.auto-calc-interval", 6000L));
    }

    public String getUpgradeBorderReq(int level) {
        return config.getString("grandseas.upgrades.border." + level,
                config.getString("upgrades.border." + level, "99,999999999"));
    }

    public String getUpgradeGeneratorReq(int level) {
        return config.getString("grandseas.upgrades.generator." + level,
                config.getString("upgrades.generator." + level, "99,999999999"));
    }

    public org.bukkit.configuration.ConfigurationSection getPointsBlocksSection() {
        org.bukkit.configuration.ConfigurationSection section =
                config.getConfigurationSection("grandseas.points.blocks");
        if (section != null) {
            return section;
        }
        return config.getConfigurationSection("points.blocks");
    }

    public org.bukkit.configuration.ConfigurationSection getBalancePricesSection() {
        org.bukkit.configuration.ConfigurationSection section =
                config.getConfigurationSection("grandseas.balance.prices");
        if (section != null) {
            return section;
        }
        return config.getConfigurationSection("balance.prices");
    }

    public int getBorderSizeForLevel(int borderLevel) {
        return getDefaultBorderDiameter() + (borderLevel - 1) * getBorderSizePerLevel();
    }

    private static List<PotionEffectType> parseEffects(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        List<PotionEffectType> out = new ArrayList<>();
        for (String name : names) {
            PotionEffectType type = PotionEffectType.getByName(name.trim().toUpperCase());
            if (type != null) {
                out.add(type);
            }
        }
        return out;
    }

    private static double firstDouble(double primary, double legacy, double fallback) {
        if (primary >= 0) return primary;
        if (legacy >= 0) return legacy;
        return fallback;
    }

    private static long firstLong(long primary, long legacy, long fallback) {
        if (primary > 0) return primary;
        if (legacy > 0) return legacy;
        return fallback;
    }

    private static int firstInt(int primary, int legacy, int fallback) {
        if (primary > 0) return primary;
        if (legacy > 0) return legacy;
        return fallback;
    }

    private static String firstString(String primary, String legacy, String fallback) {
        if (primary != null && !primary.isEmpty()) return primary;
        if (legacy != null && !legacy.isEmpty()) return legacy;
        return fallback;
    }
}
