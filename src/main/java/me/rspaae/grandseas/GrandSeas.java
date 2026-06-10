package me.rspaae.grandseas;

import me.rspaae.grandseas.generator.AcidOceanGenerator;
import me.rspaae.grandseas.listener.GUIListener;
import me.rspaae.grandseas.listener.IslandProtectionListener;
import me.rspaae.grandseas.listener.PlayerListener;
import me.rspaae.grandseas.manager.CommandManager;
import me.rspaae.grandseas.manager.ConfigManager;
import me.rspaae.grandseas.manager.IslandManager;
import me.rspaae.grandseas.listener.FluidFlowListener;
import me.rspaae.grandseas.listener.OreGeneratorListener;
import me.rspaae.grandseas.listener.PlayerChatListener;
import me.rspaae.grandseas.listener.PointBlockListener;
import me.rspaae.grandseas.manager.PointBlockManager;
import me.rspaae.grandseas.task.AcidDamageTask;
import me.rspaae.grandseas.task.CoopExpiryTask;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrandSeas extends JavaPlugin {

    private IslandManager islandManager;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private PointBlockManager pointBlockManager;
    private AcidDamageTask acidDamageTask;

    @Override
    public void onEnable() {
        // Step 1: Load configuration files
        this.configManager = new ConfigManager(this);
        this.configManager.loadFiles();

        // Step 2: Initialize island manager and setup world
        this.islandManager = new IslandManager(this);
        this.islandManager.setupWorld();
        this.islandManager.loadIslands();

        // Step 3: Register commands
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();

        // Initialize PointBlockManager
        this.pointBlockManager = new PointBlockManager(this);
        this.pointBlockManager.load();

        // Step 4: Register listeners
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new FluidFlowListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new OreGeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new PointBlockListener(this), this);

        // Acid damage system — event-driven (AcidIsland style).
        // Handles acid water damage + acid rain damage via per-player BukkitRunnables
        // triggered from PlayerMoveEvent, with armor corrosion, immune potion effects,
        // rain cover checks, boat safety, and more.
        this.acidDamageTask = new AcidDamageTask(this);
        getServer().getPluginManager().registerEvents(this.acidDamageTask, this);

        // Check and purge expired co-op sessions every 60 seconds
        new CoopExpiryTask(this).runTaskTimer(this, 1200L, 1200L);

        // Start Auto-Save Task (every 5 minutes = 6000 ticks)
        new me.rspaae.grandseas.task.AutoSaveTask(this).runTaskTimer(this, 6000L, 6000L);

        String version = getDescription().getVersion();
        
        getLogger().info("");
        getLogger().info("   ____                     _ ____                ");
        getLogger().info("  / ___| _ __ __ _ _ __   __| / ___|  ___  __ _ ___ ");
        getLogger().info(" | |  _ | '__/ _` | '_ \\ / _` \\___ \\ / _ \\/ _` / __|");
        getLogger().info(" | |_| || | | (_| | | | | (_| |___) |  __/ (_| \\__ \\");
        getLogger().info("  \\____||_|  \\__,_|_| |_|\\__,_|____/ \\___|\\__,_|___/");
        getLogger().info("");
        getLogger().info("  » Version: " + version);
        getLogger().info("  » Author: rspaae");
        getLogger().info("  » Status: LOADED SUCCESSFULLY");
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        // Save all island data on disable
        if (this.islandManager != null) {
            this.islandManager.saveIslands();
        }
        if (this.pointBlockManager != null) {
            this.pointBlockManager.save();
        }

        // Cleanup acid damage maps
        if (this.acidDamageTask != null) {
            this.acidDamageTask.cleanup();
        }

        getLogger().info("");
        getLogger().info("  [GrandSeas] Saving data and shutting down...");
        getLogger().info("  [GrandSeas] Plugin successfully disabled!");
        getLogger().info("");
    }

    /**
     * Provides the custom chunk generator for the island world.
     * This is called by Bukkit when loading worlds with our generator.
     */
    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new AcidOceanGenerator();
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PointBlockManager getPointBlockManager() {
        return pointBlockManager;
    }

    public AcidDamageTask getAcidDamageTask() {
        return acidDamageTask;
    }
}
