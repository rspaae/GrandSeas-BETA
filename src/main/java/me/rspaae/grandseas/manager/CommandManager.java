package me.rspaae.grandseas.manager;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.command.IslandAdminCommand;
import me.rspaae.grandseas.command.IslandCommand;

public class CommandManager {
    private final GrandSeas plugin;

    public CommandManager(GrandSeas plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        plugin.getCommand("is").setExecutor(new IslandCommand(plugin));
        plugin.getCommand("is").setTabCompleter(new IslandCommand(plugin));
        plugin.getCommand("isadmin").setExecutor(new IslandAdminCommand(plugin));
        plugin.getCommand("isadmin").setTabCompleter(new IslandAdminCommand(plugin));
    }
}
