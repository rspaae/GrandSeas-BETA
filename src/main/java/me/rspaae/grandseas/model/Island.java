package me.rspaae.grandseas.model;

import me.rspaae.grandseas.config.GrandSeasSettings;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Island {
    private UUID owner;
    private Set<UUID> members;
    private Set<UUID> trustedPlayers;
    private Set<UUID> bannedPlayers;
    /** Co-op players: UUID -> expiry timestamp (epoch ms). */
    private Map<UUID, Long> coopPlayers;
    private Location center;
    private Location home;
    private IslandTheme theme;
    private int borderSize;      // Diameter of the island border (in blocks)
    private int borderLevel;     // Upgrade level of the border (1 = default)
    private int generatorLevel;  // Upgrade level of the generator (1 = default)
    private long points;         // Island level / points
    private double balance;      // Island currency (Bank)
    private long createdAt;
    private String name;
    private Map<String, Object> settings;
    
    private transient final java.util.concurrent.atomic.AtomicBoolean calculating = new java.util.concurrent.atomic.AtomicBoolean(false);

    /** Default co-op duration: 60 minutes */
    public static final long COOP_DEFAULT_DURATION_MS = TimeUnit.MINUTES.toMillis(60);

    public static int getDefaultBorderSize() {
        GrandSeasSettings s = GrandSeasSettings.get();
        return s != null ? s.getDefaultBorderDiameter() : 100;
    }

    public static int getBorderSizePerLevel() {
        GrandSeasSettings s = GrandSeasSettings.get();
        return s != null ? s.getBorderSizePerLevel() : 15;
    }

    public static int getMaxBorderLevel() {
        GrandSeasSettings s = GrandSeasSettings.get();
        return s != null ? s.getMaxBorderLevel() : 5;
    }

    public static int getMaxGeneratorLevel() {
        return 5;
    }

    public static final String SETTING_ALLOW_VISITORS = "allow-visitors";
    public static final String SETTING_MOB_SPAWNING = "mob-spawning";
    public static final String SETTING_PVP = "pvp";
    
    // Privacy settings for Island Top
    public static final String SETTING_SHOW_BALANCE = "show-balance-top";
    public static final String SETTING_SHOW_MEMBERS = "show-members-top";

    // Trusted Settings
    public static final String SETTING_TRUSTED_BUILD = "trusted-build";
    public static final String SETTING_TRUSTED_INTERACT = "trusted-interact";

    public Island(UUID owner, Location center, IslandTheme theme) {
        this.owner = owner;
        this.center = center;
        this.theme = theme;
        this.members = new HashSet<>();
        this.trustedPlayers = new HashSet<>();
        this.bannedPlayers = new HashSet<>();
        this.coopPlayers = new HashMap<>();
        this.settings = new HashMap<>();
        this.borderLevel = 1;
        this.generatorLevel = 1;
        this.borderSize = getDefaultBorderSize();
        this.points = 0;
        this.balance = 0.0;
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(owner);
        this.name = p != null ? p.getName() + "'s Island" : "Island " + owner.toString().substring(0, 8);
        this.createdAt = System.currentTimeMillis();
        initDefaultSettings();
    }

    private void initDefaultSettings() {
        GrandSeasSettings cfg = GrandSeasSettings.get();
        boolean visitors = cfg != null ? cfg.isDefaultSetting("allow-visitors", true) : true;
        boolean mobs = cfg != null ? cfg.isDefaultSetting("mob-spawning", false) : false;
        boolean pvp = cfg != null ? cfg.isDefaultSetting("pvp", false) : false;
        settings.putIfAbsent(SETTING_ALLOW_VISITORS, visitors);
        settings.putIfAbsent(SETTING_MOB_SPAWNING, mobs);
        settings.putIfAbsent(SETTING_PVP, pvp);
        settings.putIfAbsent(SETTING_SHOW_BALANCE, true);
        settings.putIfAbsent(SETTING_SHOW_MEMBERS, true);
        settings.putIfAbsent(SETTING_TRUSTED_BUILD, true);
        settings.putIfAbsent(SETTING_TRUSTED_INTERACT, true);
    }

    public String getName() { return name != null ? name : "Pulau " + owner.toString().substring(0, 8); }
    public void setName(String name) { this.name = name; }
    
    public java.util.concurrent.atomic.AtomicBoolean getCalculatingFlag() { return calculating; }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID member) {
        this.members.add(member);
    }

    public void removeMember(UUID member) {
        this.members.remove(member);
    }

    /**
     * Check if a player is the owner or a member of this island.
     */
    public boolean isMemberOrOwner(UUID uuid) {
        return owner.equals(uuid) || members.contains(uuid);
    }

    public boolean isMemberOrOwnerOrTrusted(UUID uuid) {
        return isMemberOrOwner(uuid) || isTrusted(uuid);
    }

    /**
     * Returns true if the player is owner, member, trusted, OR an active (non-expired) co-op player.
     */
    public boolean isMemberOrOwnerOrTrustedOrCoop(UUID uuid) {
        return isMemberOrOwner(uuid) || isTrusted(uuid) || isActiveCoop(uuid);
    }

    // ==================== Trust System ====================

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    public void setTrustedPlayers(Set<UUID> trustedPlayers) {
        this.trustedPlayers = trustedPlayers;
    }

    public void addTrusted(UUID uuid) {
        trustedPlayers.add(uuid);
    }

    public void removeTrusted(UUID uuid) {
        trustedPlayers.remove(uuid);
    }

    public boolean isTrusted(UUID uuid) {
        return trustedPlayers.contains(uuid);
    }

    // ==================== Co-op System ====================

    public Map<UUID, Long> getCoopPlayers() {
        return coopPlayers;
    }

    public void setCoopPlayers(Map<UUID, Long> coopPlayers) {
        this.coopPlayers = coopPlayers != null ? coopPlayers : new HashMap<>();
    }

    /**
     * Add a player as a co-op with a given duration in milliseconds.
     */
    public void addCoop(UUID uuid, long durationMs) {
        coopPlayers.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public void removeCoop(UUID uuid) {
        coopPlayers.remove(uuid);
    }

    /**
     * Returns true only if the player is in the co-op list AND their session hasn't expired.
     */
    public boolean isActiveCoop(UUID uuid) {
        Long expiry = coopPlayers.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            // Auto-clean expired entry
            coopPlayers.remove(uuid);
            return false;
        }
        return true;
    }

    public boolean isCoop(UUID uuid) {
        return coopPlayers.containsKey(uuid);
    }

    /**
     * Returns the expiry timestamp (epoch ms) for a co-op player, or -1 if not found.
     */
    public long getCoopExpiry(UUID uuid) {
        return coopPlayers.getOrDefault(uuid, -1L);
    }

    /**
     * Remove all expired co-op entries. Called by CoopExpiryTask.
     * Returns set of UUIDs that were removed.
     */
    public Set<UUID> purgeExpiredCoop() {
        long now = System.currentTimeMillis();
        Set<UUID> expired = new HashSet<>();
        coopPlayers.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                expired.add(entry.getKey());
                return true;
            }
            return false;
        });
        return expired;
    }

    // ==================== Ban System ====================

    public Set<UUID> getBannedPlayers() {
        return bannedPlayers;
    }

    public void setBannedPlayers(Set<UUID> bannedPlayers) {
        this.bannedPlayers = bannedPlayers;
    }

    public void banPlayer(UUID uuid) {
        bannedPlayers.add(uuid);
        // Remove from members if they were one
        members.remove(uuid);
    }

    public void unbanPlayer(UUID uuid) {
        bannedPlayers.remove(uuid);
    }

    public boolean isBanned(UUID uuid) {
        return bannedPlayers.contains(uuid);
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public IslandTheme getTheme() {
        return theme;
    }

    public void setTheme(IslandTheme theme) {
        this.theme = theme;
    }

    public int getBorderSize() {
        return borderSize;
    }

    public void setBorderSize(int borderSize) {
        this.borderSize = borderSize;
    }

    public int getBorderLevel() {
        return borderLevel;
    }

    public void setBorderLevel(int borderLevel) {
        this.borderLevel = Math.min(getMaxBorderLevel(), Math.max(1, borderLevel));
    }

    public int getGeneratorLevel() {
        return generatorLevel;
    }

    public void setGeneratorLevel(int generatorLevel) {
        this.generatorLevel = Math.min(getMaxGeneratorLevel(), Math.max(1, generatorLevel));
    }

    public boolean upgradeGenerator() {
        if (this.generatorLevel < getMaxGeneratorLevel()) {
            this.generatorLevel++;
            return true;
        }
        return false;
    }

    /**
     * Upgrade the border to the next level.
     * @return true if upgrade was successful, false if already at max level
     */
    public boolean upgradeBorder() {
        if (borderLevel >= getMaxBorderLevel()) {
            return false;
        }
        borderLevel++;
        borderSize = getDefaultBorderSize() + (borderLevel - 1) * getBorderSizePerLevel();
        return true;
    }

    /**
     * Get the border radius (half of borderSize).
     */
    public int getBorderRadius() {
        return borderSize / 2;
    }

    /**
     * Check if a location is within this island's border.
     */
    public boolean isWithinBorder(Location loc) {
        if (center == null || loc == null) return false;
        if (center.getWorld() == null || loc.getWorld() == null) return false;
        if (!center.getWorld().equals(loc.getWorld())) return false;

        int radius = getBorderRadius();
        double dx = Math.abs(loc.getX() - center.getX());
        double dz = Math.abs(loc.getZ() - center.getZ());

        return dx <= radius && dz <= radius;
    }

    public long getPoints() {
        return points;
    }

    public int getIslandLevel() {
        // Sistem Level SkyBlock Klasik: 1 Level = 100 Poin
        // Jadi kalau ada 500 poin, levelnya 5.
        // Math.max(1, ...) memastikan level minimal adalah 1.
        return Math.max(1, (int) (this.points / 100));
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    public void removeBalance(double amount) {
        this.balance = Math.max(0, this.balance - amount);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
        initDefaultSettings();
    }

    public void setSetting(String key, Object value) {
        this.settings.put(key, value);
    }

    /**
     * Get a boolean setting with a default fallback.
     */
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultValue;
    }

    /**
     * Toggle a boolean setting and return the new value.
     */
    public boolean toggleSetting(String key, boolean defaultValue) {
        boolean current = getBooleanSetting(key, defaultValue);
        boolean newValue = !current;
        settings.put(key, newValue);
        return newValue;
    }

    public boolean isAllowVisitors() {
        return getBooleanSetting(SETTING_ALLOW_VISITORS, true);
    }

    public boolean isMobSpawning() {
        return getBooleanSetting(SETTING_MOB_SPAWNING, false);
    }

    public boolean isPvpEnabled() {
        return getBooleanSetting(SETTING_PVP, false);
    }

    public boolean isShowBalanceOnTop() {
        return getBooleanSetting(SETTING_SHOW_BALANCE, true);
    }

    public boolean isShowMembersOnTop() {
        return getBooleanSetting(SETTING_SHOW_MEMBERS, true);
    }
}
