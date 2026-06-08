package me.rspaae.grandseas.task;

import me.rspaae.grandseas.GrandSeas;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically saves all island and economy data to prevent data loss on crashes.
 * Runs every 5 minutes by default.
 */
public class AutoSaveTask extends BukkitRunnable {

    private final GrandSeas plugin;

    public AutoSaveTask(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getIslandManager().saveIslands();
        plugin.getPointBlockManager().save();
    }
}
