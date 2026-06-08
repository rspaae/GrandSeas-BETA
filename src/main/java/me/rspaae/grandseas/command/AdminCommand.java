package me.rspaae.grandseas.command;

import me.rspaae.grandseas.GrandSeas;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements TabExecutor {
    private final GrandSeas plugin;

    public AdminCommand(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grandseas.admin")) {
            sender.sendMessage("You do not have permission to execute this command.");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("GrandSeas Admin Commands: /isadmin reload");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                // Handle plugin reload
                sender.sendMessage("GrandSeas plugin reloaded.");
                break;
            default:
                sender.sendMessage("Unknown admin subcommand.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("grandseas.admin")) {
            completions.add("reload");
        }
        return completions;
    }
}
