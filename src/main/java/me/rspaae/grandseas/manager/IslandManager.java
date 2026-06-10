package me.rspaae.grandseas.manager;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.config.GrandSeasSettings;
import me.rspaae.grandseas.generator.AcidOceanGenerator;
import me.rspaae.grandseas.model.AuditLog;
import me.rspaae.grandseas.model.Island;
import me.rspaae.grandseas.model.IslandTheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages all island operations: creation, deletion, loading, saving,
 * grid coordinate calculation, invite system, and world management.
 */
public class IslandManager {

    private final GrandSeas plugin;
    private final Map<UUID, Island> islands = new HashMap<>();

    // Pending invites: invitee UUID → inviter (owner) UUID
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    public final Set<UUID> islandChatPlayers = new HashSet<>();
    private final Set<UUID> islandCreationInProgress = new HashSet<>();

    /** Admins with active bypass mode (cleared on restart). */
    private final Set<UUID> bypassAdmins = new HashSet<>();

    /** In-memory audit log: island owner UUID → list of events (max 50 per island). */
    private final Map<UUID, java.util.LinkedList<AuditLog>> auditLogs = new HashMap<>();

    private static final int MAX_AUDIT_ENTRIES = 50;

    private World islandWorld;
    private int nextIslandIndex = 0;

    // Data file
    private File dataFile;
    private YamlConfiguration dataConfig;

    public IslandManager(GrandSeas plugin) {
        this.plugin = plugin;
    }

    // ==================== World Management ====================

    /**
     * Creates or loads the custom island world with the AcidOceanGenerator.
     * Must be called during onEnable.
     */
    public String getWorldName() {
        return settings().getWorldName();
    }

    private GrandSeasSettings settings() {
        return plugin.getConfigManager().getSettings();
    }

    public void setupWorld() {
        GrandSeasSettings cfg = settings();
        String worldName = cfg.getWorldName();
        islandWorld = Bukkit.getWorld(worldName);

        if (islandWorld == null) {
            plugin.getLogger().info("Creating island world '" + worldName + "'...");
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new AcidOceanGenerator());
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            islandWorld = creator.createWorld();

            if (islandWorld != null) {
                islandWorld.setDifficulty(Difficulty.NORMAL);
                islandWorld.setSpawnLocation(0, AcidOceanGenerator.SEA_LEVEL + 1, 0);
                islandWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                islandWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                islandWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                islandWorld.setStorm(false);
                islandWorld.setThundering(false);
                islandWorld.setTime(6000);
                // Matikan semua mob spawn limit
                islandWorld.setMonsterSpawnLimit(0);
                islandWorld.setAnimalSpawnLimit(0);
                islandWorld.setWaterAnimalSpawnLimit(0);
                islandWorld.setAmbientSpawnLimit(0);
                plugin.getLogger().info("Island world created successfully!");
            } else {
                plugin.getLogger().severe("Failed to create island world!");
            }
        } else {
            plugin.getLogger().info("Island world '" + worldName + "' loaded.");
        }
    }

    public World getIslandWorld() {
        return islandWorld;
    }

    // ==================== Grid System (Spiral) ====================

    /**
     * Calculate the grid position for a given island index using a square spiral pattern.
     * Index 0 = (0, 0), then spirals outward.
     */
    private Location calculateGridLocation(int index) {
        if (index == 0) {
            return new Location(islandWorld, 0, 0, 0);
        }

        // Square spiral algorithm
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int segmentLength = 1;
        int segmentPassed = 0;
        int turnsMade = 0;

        for (int i = 0; i < index; i++) {
            x += dx;
            z += dz;
            segmentPassed++;

            if (segmentPassed == segmentLength) {
                segmentPassed = 0;
                // Turn right (clockwise)
                int temp = dx;
                dx = -dz;
                dz = temp;
                turnsMade++;

                if (turnsMade % 2 == 0) {
                    segmentLength++;
                }
            }
        }

        GrandSeasSettings cfg = settings();
        int spacing = cfg.getDistanceBetweenIslands();
        int baseX = cfg.getStartX() + cfg.getOffsetX();
        int baseZ = cfg.getStartZ() + cfg.getOffsetZ();
        return new Location(islandWorld, baseX + (long) x * spacing, AcidOceanGenerator.SEA_LEVEL, baseZ + (long) z * spacing);
    }

    // ==================== Island CRUD ====================

    /**
     * Create a new island for a player with the specified theme.
     * This is the main entry point called from the GUI.
     */
    public void createIsland(Player player, IslandTheme theme) {
        UUID uuid = player.getUniqueId();

        if (islands.containsKey(uuid)) {
            player.sendMessage(Component.text("❌ You already have an island!", NamedTextColor.RED));
            return;
        }

        if (islandWorld == null) {
            player.sendMessage(Component.text("❌ Island world is not ready yet. Contact an admin.", NamedTextColor.RED));
            return;
        }

        if (islandCreationInProgress.contains(uuid)) {
            player.sendMessage(Component.text("⏳ Please wait, your island is being created!", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("⏳ Preparing your island...", NamedTextColor.AQUA));
        islandCreationInProgress.add(uuid);

        // Hitung posisi grid
        Location gridCenter = calculateGridLocation(nextIslandIndex);
        nextIslandIndex++;
        
        // BUG FIX: Segera simpan index ke file untuk mencegah overlapping grid jika server crash
        saveIslands();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                int cx = gridCenter.getBlockX();
                int cz = gridCenter.getBlockZ();
                
                // Puncak pulau sejajar dengan air (y=63)
                int surfaceY = AcidOceanGenerator.SEA_LEVEL; 

                // ===== Tentukan Material Sesuai Tema =====
                Material topMat;
                Material bottomMat;
                org.bukkit.TreeType treeType;

                switch (theme) {
                    case SANDY:
                        topMat = Material.SAND;
                        bottomMat = Material.SANDSTONE;
                        treeType = org.bukkit.TreeType.JUNGLE; // Pohon ala tropis
                        break;
                    case ROCKY:
                        topMat = Material.STONE;
                        bottomMat = Material.COBBLESTONE;
                        treeType = org.bukkit.TreeType.REDWOOD; // Pohon spruce/pinus
                        break;
                    case CLASSIC:
                    default:
                        topMat = Material.GRASS_BLOCK;
                        bottomMat = Material.DIRT;
                        treeType = org.bukkit.TreeType.BIG_TREE; // Pohon oak besar
                        break;
                }

                // ===== Classic SkyBlock Hemisphere =====
                int radius = 5; // Diperbesar agar tidak kekecilan
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= 0; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            // Bentuk setengah bola (hemisphere) terbalik
                            if (x*x + y*y + z*z <= radius*radius) {
                                Material mat = (y == 0) ? topMat : bottomMat;
                                islandWorld.getBlockAt(cx + x, surfaceY + y, cz + z).setType(mat, false);
                            }
                        }
                    }
                }
                
                // Bedrock di titik terbawah tengah pulau
                islandWorld.getBlockAt(cx, surfaceY - radius, cz).setType(Material.BEDROCK, false);

                // ===== Pohon Sesuai Tema =====
                // Tanam pohon agak ke pinggir agar tidak nyangkut saat spawn
                islandWorld.generateTree(new Location(islandWorld, cx - 2, surfaceY + 1, cz), treeType);

                // ===== Starter Chest =====
                // Taruh chest di sisi yang berlawanan dengan pohon
                int chestX = cx + 2, chestZ = cz;
                org.bukkit.block.Block chestBlock = islandWorld.getBlockAt(chestX, surfaceY + 1, chestZ);
                chestBlock.setType(Material.CHEST, false);

                if (chestBlock.getState() instanceof org.bukkit.block.Chest chest) {
                    org.bukkit.inventory.Inventory inv = chest.getInventory();
                    
                    List<String> items = plugin.getConfigManager().getSettings().getStarterChestItems();
                    if (items != null && !items.isEmpty()) {
                        java.util.List<Integer> availableSlots = new java.util.ArrayList<>();
                        for (int i = 0; i < inv.getSize(); i++) availableSlots.add(i);
                        java.util.Collections.shuffle(availableSlots);
                        
                        int slotIndex = 0;
                        for (String itemStr : items) {
                            if (slotIndex >= availableSlots.size()) break;
                            try {
                                String[] split = itemStr.split(":");
                                Material m = Material.valueOf(split[0].toUpperCase());
                                int amt = split.length > 1 ? Integer.parseInt(split[1]) : 1;
                                inv.setItem(availableSlots.get(slotIndex), new org.bukkit.inventory.ItemStack(m, amt));
                                slotIndex++;
                            } catch (Exception ignored) {}
                        }
                    } else {
                        // Fallback
                        inv.setItem(0, new org.bukkit.inventory.ItemStack(Material.ICE, 2));
                        inv.setItem(1, new org.bukkit.inventory.ItemStack(Material.LAVA_BUCKET, 1));
                    }
                }

                // Spawn di tengah pulau yang kosong (tinggi dilebihkan 1 block agar aman)
                Location spawnLoc = new Location(islandWorld, cx + 0.5, surfaceY + 2, cz + 0.5, 0f, 0f);

                // Daftarkan island
                Island island = new Island(uuid, gridCenter, theme);
                island.setHome(spawnLoc);
                islands.put(uuid, island);
                saveIslands();

                // Teleport ke island
                player.teleport(spawnLoc);
                updatePlayerBorder(player, spawnLoc);

                player.playSound(spawnLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.sendTitle("§b§l🏝 Welcome!", "§7Your island is ready! Check the chest to get started.", 10, 60, 20);
                player.sendMessage(Component.text("🏝 Island created successfully!", NamedTextColor.GREEN));
                player.sendMessage(Component.text("💡 Open the chest at the edge of the platform for your starter items!", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("💡 Place ", NamedTextColor.GRAY)
                        .append(Component.text("Ice", NamedTextColor.AQUA))
                        .append(Component.text(" + ", NamedTextColor.GRAY))
                        .append(Component.text("Lava", NamedTextColor.RED))
                        .append(Component.text(" to start the ore generator!", NamedTextColor.GRAY)));

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create island for " + player.getName(), e);
                player.sendMessage(Component.text("❌ An error occurred while creating the island.", NamedTextColor.RED));
                nextIslandIndex--;
                saveIslands();
            } finally {
                islandCreationInProgress.remove(uuid);
            }
        });
    }

    /**
     * Get a player's island by their UUID.
     * Checks both as owner and as member.
     */
    public Island getIsland(UUID uuid) {
        // Check if they own an island
        Island owned = islands.get(uuid);
        if (owned != null) return owned;

        // Check if they're a member of another island
        for (Island island : islands.values()) {
            if (island.getMembers().contains(uuid)) {
                return island;
            }
        }
        return null;
    }

    /**
     * Get an island by owner UUID only (not member lookup).
     */
    public Island getIslandByOwner(UUID ownerUuid) {
        return islands.get(ownerUuid);
    }

    /**
     * Find which island a location belongs to (if any).
     */
    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        if (!location.getWorld().getName().equals(getWorldName())) return null;

        for (Island island : islands.values()) {
            if (island.isWithinBorder(location)) {
                return island;
            }
        }
        return null;
    }

    /**
     * Check if a player can interact at a specific location.
     * Returns true if:
     * - The location is not in the island world
     * - The location is within the player's own island border
     * - The player has admin permission
     */
    public boolean canInteract(Player player, Location location) {
        if (location == null || location.getWorld() == null) return true;
        if (!location.getWorld().getName().equals(getWorldName())) return true;
        if (player.hasPermission("grandseas.admin.bypass")) return true;
        if (bypassAdmins.contains(player.getUniqueId())) return true;

        Island island = getIslandAt(location);
        if (island == null) {
            // No island here — block interaction in the void ocean
            return false;
        }

        if (island.isMemberOrOwner(player.getUniqueId())) return true;

        if (island.isTrusted(player.getUniqueId())) {
            Boolean trustedInteract = (Boolean) island.getSettings().get(Island.SETTING_TRUSTED_INTERACT);
            return trustedInteract != null && trustedInteract;
        }

        // Co-op players can interact
        if (island.isActiveCoop(player.getUniqueId())) return true;

        return false;
    }

    /**
     * Check if a player can build/break at a specific location.
     */
    public boolean canBuild(Player player, Location location) {
        if (location == null || location.getWorld() == null) return true;
        if (!location.getWorld().getName().equals(getWorldName())) return true;
        if (player.hasPermission("grandseas.admin.bypass")) return true;
        if (bypassAdmins.contains(player.getUniqueId())) return true;

        Island island = getIslandAt(location);
        if (island == null) {
            return false;
        }

        if (island.isMemberOrOwner(player.getUniqueId())) return true;

        if (island.isTrusted(player.getUniqueId())) {
            Boolean trustedBuild = (Boolean) island.getSettings().get(Island.SETTING_TRUSTED_BUILD);
            return trustedBuild != null && trustedBuild;
        }

        // Co-op players can build
        if (island.isActiveCoop(player.getUniqueId())) return true;

        return false;
    }

    /**
     * Delete an island — clears blocks, teleports players out, removes data.
     */
    public void deleteIsland(UUID ownerId) {
        Island island = islands.get(ownerId);
        if (island == null) return;

        teleportAllMembersOut(island);
        if (island.getCenter() != null && islandWorld != null) {
            clearIslandBlocks(island);
        }
        islands.remove(ownerId);
        saveIslands();
    }

    /** Overload: delete by Island object directly (for admin command) */
    public void deleteIsland(Island island) {
        deleteIsland(island.getOwner());
    }

    /** Teleport a player to a specific island's home (for admin tp) */
    public void teleportToIsland(Player player, Island island) {
        Location dest = island.getHome() != null ? island.getHome() : island.getCenter();
        if (dest == null) {
            player.sendMessage(Component.text("Island location not found!", NamedTextColor.RED));
            return;
        }
        player.teleport(dest);
        updatePlayerBorder(player, dest);
    }

    /**
     * Teleport all online members and the owner out of the island world.
     */
    private void teleportAllMembersOut(Island island) {
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();

        // Owner
        Player owner = Bukkit.getPlayer(island.getOwner());
        if (owner != null && owner.isOnline()) {
            if (owner.getWorld().getName().equals(getWorldName())) {
                owner.teleport(spawn);
                owner.setWorldBorder(null);
            }
        }

        // Members
        for (UUID memberId : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                if (member.getWorld().getName().equals(getWorldName())) {
                    member.teleport(spawn);
                    member.setWorldBorder(null);
                }
                member.sendMessage(Component.text("⚠ The island you were on has been deleted.", NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Clear all blocks in the island area back to ocean.
     */
    private void clearIslandBlocks(Island island) {
        Location center = island.getCenter();
        int radius = island.getBorderRadius();
        int seaLevel = AcidOceanGenerator.SEA_LEVEL;

        // Run block clearing slightly delayed to avoid lag spikes
        Bukkit.getScheduler().runTask(plugin, () -> {
            int cx = center.getBlockX();
            int cz = center.getBlockZ();

            // Only clear a reasonable area around the island (not the full border)
            int clearRadius = Math.min(radius, 25); // island is small, clear a limited area

            // Hapus semua PointBlock Storage beserta hologramnya
            for (me.rspaae.grandseas.model.PointBlock pb : new java.util.ArrayList<>(plugin.getPointBlockManager().getAll())) {
                if (island.isWithinBorder(pb.getLocation())) {
                    plugin.getPointBlockManager().removePointBlock(pb.getLocation());
                }
            }

            for (int x = cx - clearRadius; x <= cx + clearRadius; x++) {
                for (int z = cz - clearRadius; z <= cz + clearRadius; z++) {
                    // Above sea level → AIR
                    for (int y = seaLevel + 1; y <= seaLevel + 20; y++) {
                        islandWorld.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                    // At and below sea level → WATER (restore ocean)
                    for (int y = seaLevel; y >= seaLevel - 10; y--) {
                        Material current = islandWorld.getBlockAt(x, y, z).getType();
                        if (current != Material.WATER && current != Material.BEDROCK) {
                            islandWorld.getBlockAt(x, y, z).setType(Material.WATER, false);
                        }
                    }
                }
            }
        });
    }

    public void updateIsland(Island island) {
        if (island != null) {
            islands.put(island.getOwner(), island);
            saveIslands();
        }
    }

    public Map<UUID, Island> getAllIslands() {
        return Collections.unmodifiableMap(islands);
    }

    // ==================== Admin Bypass ====================

    /**
     * Toggle admin bypass mode. Returns true if bypass is now ACTIVE, false if disabled.
     */
    public boolean toggleBypass(UUID adminUuid) {
        if (bypassAdmins.contains(adminUuid)) {
            bypassAdmins.remove(adminUuid);
            return false;
        } else {
            bypassAdmins.add(adminUuid);
            return true;
        }
    }

    public boolean hasBypass(UUID adminUuid) {
        return bypassAdmins.contains(adminUuid);
    }

    // ==================== Audit Log ====================

    /**
     * Add an audit log entry for a specific island (by owner UUID).
     * The log is capped at MAX_AUDIT_ENTRIES (50) entries per island.
     */
    public void addAuditLog(UUID islandOwner, AuditLog log) {
        java.util.LinkedList<AuditLog> logs = auditLogs.computeIfAbsent(islandOwner, k -> new java.util.LinkedList<>());
        logs.addFirst(log); // newest first
        if (logs.size() > MAX_AUDIT_ENTRIES) {
            logs.removeLast();
        }
    }

    /**
     * Get audit logs for an island. Returns an empty list if none exist.
     */
    public java.util.List<AuditLog> getAuditLog(UUID islandOwner) {
        return auditLogs.getOrDefault(islandOwner, new java.util.LinkedList<>());
    }

    // ==================== Admin Backup ====================

    /**
     * Create a backup of an island's data to plugins/GrandSeas/backups/.
     * Does NOT delete the island.
     * @return the backup file, or null on failure.
     */
    public File backupIsland(Island island) {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
        if (ownerName == null) ownerName = island.getOwner().toString().substring(0, 8);

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File backupFile = new File(backupDir, timestamp + "_" + ownerName + ".yml");

        YamlConfiguration backup = new YamlConfiguration();
        backup.set("owner", island.getOwner().toString());
        backup.set("name", island.getName());
        backup.set("points", island.getPoints());
        backup.set("balance", island.getBalance());
        backup.set("border-size", island.getBorderSize());
        backup.set("border-level", island.getBorderLevel());
        backup.set("generator-level", island.getGeneratorLevel());
        backup.set("created-at", island.getCreatedAt());
        backup.set("backed-up-at", System.currentTimeMillis());

        java.util.List<String> memberList = new java.util.ArrayList<>();
        for (UUID m : island.getMembers()) memberList.add(m.toString());
        backup.set("members", memberList);

        try {
            backup.save(backupFile);
            return backupFile;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write island backup!", e);
            return null;
        }
    }

    // ==================== Team / Invite System ====================

    /**
     * Send an invite to a player to join an island.
     */
    public boolean invitePlayer(Player owner, Player target) {
        UUID ownerUuid = owner.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Island island = islands.get(ownerUuid);
        if (island == null) return false;

        // Check max members
        int maxTeam = settings().getMaxTeamSize();
        if (island.getMembers().size() + 1 >= maxTeam) {
            return false;
        }

        // Store the invite
        pendingInvites.put(targetUuid, ownerUuid);

        // Schedule expiry
        int timeout = settings().getInviteTimeoutSeconds();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID pending = pendingInvites.get(targetUuid);
            if (pending != null && pending.equals(ownerUuid)) {
                pendingInvites.remove(targetUuid);
                // Notify
                Player ownerPlayer = Bukkit.getPlayer(ownerUuid);
                Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (ownerPlayer != null && ownerPlayer.isOnline()) {
                    ownerPlayer.sendMessage(Component.text("⏰ Your invitation to " + target.getName() + " has expired.", NamedTextColor.GRAY));
                }
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(Component.text("⏰ The invitation from " + owner.getName() + " has expired.", NamedTextColor.GRAY));
                }
            }
        }, timeout * 20L); // Convert seconds to ticks

        return true;
    }

    /**
     * Check if a player has a pending invite.
     */
    public boolean hasPendingInvite(UUID targetUuid) {
        return pendingInvites.containsKey(targetUuid);
    }

    /**
     * Get the island owner UUID who sent the invite.
     */
    public UUID getInviter(UUID targetUuid) {
        return pendingInvites.get(targetUuid);
    }

    /**
     * Accept a pending invite — adds the player as a member.
     */
    public boolean acceptInvite(Player target) {
        UUID targetUuid = target.getUniqueId();
        UUID ownerUuid = pendingInvites.get(targetUuid);
        if (ownerUuid == null) return false;

        Island island = islands.get(ownerUuid);
        if (island == null) {
            pendingInvites.remove(targetUuid);
            return false;
        }

        int maxTeam = settings().getMaxTeamSize();
        if (island.getMembers().size() + 1 >= maxTeam) {
            pendingInvites.remove(targetUuid);
            return false;
        }

        island.addMember(targetUuid);
        pendingInvites.remove(targetUuid);
        saveIslands();
        return true;
    }

    /**
     * Deny a pending invite.
     */
    public void denyInvite(UUID targetUuid) {
        pendingInvites.remove(targetUuid);
    }

    /**
     * Kick a member from an island.
     */
    public boolean kickMember(UUID ownerId, UUID memberId) {
        Island island = islands.get(ownerId);
        if (island == null) return false;
        if (!island.getMembers().contains(memberId)) return false;

        island.removeMember(memberId);
        saveIslands();

        // Teleport kicked player out if online and in island world
        Player kicked = Bukkit.getPlayer(memberId);
        if (kicked != null && kicked.isOnline() && kicked.getWorld().getName().equals(getWorldName())) {
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            kicked.teleport(spawn);
            kicked.setWorldBorder(null);
        }

        return true;
    }

    /**
     * Let a member leave an island voluntarily.
     */
    public boolean leaveIsland(UUID memberId) {
        Island island = getIsland(memberId);
        if (island == null) return false;
        if (island.getOwner().equals(memberId)) return false; // Owner can't leave

        island.removeMember(memberId);
        saveIslands();

        // Teleport out if in island world
        Player leaving = Bukkit.getPlayer(memberId);
        if (leaving != null && leaving.isOnline() && leaving.getWorld().getName().equals(getWorldName())) {
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            leaving.teleport(spawn);
            leaving.setWorldBorder(null);
        }

        return true;
    }

    /**
     * Transfer island ownership from the current owner to a member.
     */
    public boolean promoteToOwner(UUID currentOwnerId, UUID newOwnerId) {
        Island island = islands.get(currentOwnerId);
        if (island == null) return false;
        if (!island.getMembers().contains(newOwnerId)) return false;

        // Remove new owner from members, add old owner as member
        island.removeMember(newOwnerId);
        island.addMember(currentOwnerId);
        island.setOwner(newOwnerId);

        // Re-key in map
        islands.remove(currentOwnerId);
        islands.put(newOwnerId, island);

        saveIslands();
        return true;
    }

    public Map<UUID, UUID> getPendingInvites() {
        return pendingInvites;
    }

    // ==================== World Border Visuals ====================

    /**
     * Updates the vanilla WorldBorder visualization for a player based on their location.
     * If they are inside an island, they will see that island's border.
     * If they leave the island world, their border is reset.
     */
    public void updatePlayerBorder(Player player, Location location) {
        if (location == null || location.getWorld() == null) return;
        
        if (!location.getWorld().getName().equals(getWorldName())) {
            // Reset to default world border
            player.setWorldBorder(null);
            return;
        }

        Island island = getIslandAt(location);
        if (island != null) {
            org.bukkit.WorldBorder border = Bukkit.createWorldBorder();
            border.setCenter(island.getCenter());
            // borderSize is the diameter
            border.setSize(island.getBorderSize());
            // Make the border static (no warning time/distance)
            border.setWarningDistance(0);
            border.setWarningTime(0);
            
            player.setWorldBorder(border);
        } else {
            // If they are somehow in the ocean outside an island, reset border
            player.setWorldBorder(null);
        }
    }

    // ==================== Data Persistence ====================

    public void saveIslands() {
        if (dataFile == null) return;

        dataConfig = new YamlConfiguration();
        dataConfig.set("next-island-index", nextIslandIndex);

        for (Map.Entry<UUID, Island> entry : islands.entrySet()) {
            String path = "islands." + entry.getKey().toString();
            Island island = entry.getValue();
            ConfigurationSection islandSec = dataConfig.createSection(path);

            islandSec.set("owner", island.getOwner().toString());
            islandSec.set("theme", island.getTheme().name());
            islandSec.set("border-size", island.getBorderSize());
            islandSec.set("border-level", island.getBorderLevel());
            islandSec.set("generator-level", island.getGeneratorLevel());
            islandSec.set("name", island.getName());
            islandSec.set("points", island.getPoints());
            islandSec.set("balance", island.getBalance());
            islandSec.set("created-at", island.getCreatedAt());

            // Save center location
            if (island.getCenter() != null) {
                islandSec.set("center.world", island.getCenter().getWorld().getName());
                islandSec.set("center.x", island.getCenter().getBlockX());
                islandSec.set("center.y", island.getCenter().getBlockY());
                islandSec.set("center.z", island.getCenter().getBlockZ());
            }

            // Save home location
            if (island.getHome() != null) {
                islandSec.set("home.world", island.getHome().getWorld().getName());
                islandSec.set("home.x", island.getHome().getX());
                islandSec.set("home.y", island.getHome().getY());
                islandSec.set("home.z", island.getHome().getZ());
                islandSec.set("home.yaw", island.getHome().getYaw());
                islandSec.set("home.pitch", island.getHome().getPitch());
            }

            // Save members
            List<String> memberList = new ArrayList<>();
            for (UUID member : island.getMembers()) {
                memberList.add(member.toString());
            }
            islandSec.set("members", memberList);

            // Save trusted players
            List<String> trustedList = new ArrayList<>();
            for (UUID trusted : island.getTrustedPlayers()) {
                trustedList.add(trusted.toString());
            }
            islandSec.set("trusted", trustedList);

            // Save banned players
            List<String> banList = new ArrayList<>();
            for (UUID banned : island.getBannedPlayers()) {
                banList.add(banned.toString());
            }
            islandSec.set("banned", banList);

            // Save coop players
            List<String> coopList = new ArrayList<>();
            for (Map.Entry<UUID, Long> coopEntry : island.getCoopPlayers().entrySet()) {
                coopList.add(coopEntry.getKey().toString() + ":" + coopEntry.getValue());
            }
            islandSec.set("coop", coopList);

            // Save settings
            Map<String, Object> settings = island.getSettings();
            for (Map.Entry<String, Object> setting : settings.entrySet()) {
                islandSec.set("settings." + setting.getKey(), setting.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save island data!", e);
        }
    }

    public void loadIslands() {
        // Ensure data directory exists
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        dataFile = new File(dataDir, "islands.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create island data file!", e);
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        nextIslandIndex = dataConfig.getInt("next-island-index", 0);

        ConfigurationSection islandsSection = dataConfig.getConfigurationSection("islands");
        if (islandsSection == null) {
            plugin.getLogger().info("No existing island data found. Starting fresh.");
            return;
        }

        for (String key : islandsSection.getKeys(false)) {
            try {
                ConfigurationSection islandSec = islandsSection.getConfigurationSection(key);
                if (islandSec == null) continue;
                
                UUID ownerUuid = UUID.fromString(islandSec.getString("owner"));

                // Load center location
                String centerWorldName = islandSec.getString("center.world", getWorldName());
                World centerWorld = Bukkit.getWorld(centerWorldName);
                if (centerWorld == null) centerWorld = islandWorld; // fallback
                int cx = islandSec.getInt("center.x");
                int cy = islandSec.getInt("center.y");
                int cz = islandSec.getInt("center.z");
                Location center = new Location(centerWorld, cx, cy, cz);

                // Load theme
                String themeName = islandSec.getString("theme", "CLASSIC");
                IslandTheme theme;
                try {
                    theme = IslandTheme.valueOf(themeName);
                } catch (IllegalArgumentException e) {
                    theme = IslandTheme.CLASSIC;
                }

                // Create island object
                Island island = new Island(ownerUuid, center, theme);
                island.setBorderSize(islandSec.getInt("border-size", Island.getDefaultBorderSize()));
                island.setBorderLevel(islandSec.getInt("border-level", 1));
                island.setGeneratorLevel(islandSec.getInt("generator-level", 1));
                island.setPoints(islandSec.getLong("points", 0L));
                island.setBalance(islandSec.getDouble("balance", 0.0));
                island.setCreatedAt(islandSec.getLong("created-at", System.currentTimeMillis()));
                
                String name = islandSec.getString("name");
                if (name != null && !name.isEmpty()) {
                    island.setName(name);
                }

                // Load home location
                if (islandSec.contains("home.world")) {
                    String homeWorldName = islandSec.getString("home.world", getWorldName());
                    World homeWorld = Bukkit.getWorld(homeWorldName);
                    if (homeWorld == null) homeWorld = islandWorld;
                    double hx = islandSec.getDouble("home.x");
                    double hy = islandSec.getDouble("home.y");
                    double hz = islandSec.getDouble("home.z");
                    float hyaw = (float) islandSec.getDouble("home.yaw");
                    float hpitch = (float) islandSec.getDouble("home.pitch");
                    island.setHome(new Location(homeWorld, hx, hy, hz, hyaw, hpitch));
                }

                // Load members
                List<String> memberList = islandSec.getStringList("members");
                for (String memberStr : memberList) {
                    try {
                        island.addMember(UUID.fromString(memberStr));
                    } catch (IllegalArgumentException ignored) {}
                }

                // Load trusted
                List<String> trustedList = islandSec.getStringList("trusted");
                Set<UUID> trustedSet = new HashSet<>();
                for (String trustedStr : trustedList) {
                    try {
                        trustedSet.add(UUID.fromString(trustedStr));
                    } catch (IllegalArgumentException ignored) {}
                }
                island.setTrustedPlayers(trustedSet);

                // Load banned players
                List<String> banList = islandSec.getStringList("banned");
                Set<UUID> bannedSet = new HashSet<>();
                for (String bannedStr : banList) {
                    try {
                        bannedSet.add(UUID.fromString(bannedStr));
                    } catch (IllegalArgumentException ignored) {}
                }
                island.setBannedPlayers(bannedSet);

                // Load coop players
                List<String> coopList = islandSec.getStringList("coop");
                Map<UUID, Long> coopMap = new HashMap<>();
                long now = System.currentTimeMillis();
                for (String coopStr : coopList) {
                    try {
                        String[] parts = coopStr.split(":");
                        if (parts.length == 2) {
                            UUID coopUuid = UUID.fromString(parts[0]);
                            long expiry = Long.parseLong(parts[1]);
                            // Only load non-expired entries
                            if (expiry > now) {
                                coopMap.put(coopUuid, expiry);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                island.setCoopPlayers(coopMap);

                // Load settings
                ConfigurationSection settingsSection = islandSec.getConfigurationSection("settings");
                if (settingsSection != null) {
                    Map<String, Object> settings = new HashMap<>();
                    for (String settingKey : settingsSection.getKeys(false)) {
                        settings.put(settingKey, settingsSection.get(settingKey));
                    }
                    island.setSettings(settings);
                }

                islands.put(ownerUuid, island);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load island data for key: " + key, e);
            }
        }

        plugin.getLogger().info("Loaded " + islands.size() + " islands.");
    }

    // ==================== Island Chat ====================

    public boolean toggleIslandChat(UUID playerId) {
        if (islandChatPlayers.contains(playerId)) {
            islandChatPlayers.remove(playerId);
            return false;
        } else {
            islandChatPlayers.add(playerId);
            return true;
        }
    }

    public boolean isIslandChat(UUID playerId) {
        return islandChatPlayers.contains(playerId);
    }
}
