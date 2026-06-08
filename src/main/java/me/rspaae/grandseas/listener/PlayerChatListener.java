package me.rspaae.grandseas.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final GrandSeas plugin;

    public PlayerChatListener(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player is in island chat mode
        if (plugin.getIslandManager().isIslandChat(playerId)) {
            Island island = plugin.getIslandManager().getIsland(playerId);
            
            // If they don't have an island anymore but are in chat mode, toggle it off
            if (island == null) {
                plugin.getIslandManager().toggleIslandChat(playerId);
                return;
            }

            // Cancel the global chat event
            event.setCancelled(true);

            // Extract the message text
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            // Format: [ISLAND] PlayerName: Message
            Component chatFormat = Component.text("[ISLAND] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(message, NamedTextColor.WHITE));

            // Send to owner
            Player owner = Bukkit.getPlayer(island.getOwner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(chatFormat);
            }

            // Send to members
            for (UUID memberId : island.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage(chatFormat);
                }
            }
            
            // Send to console for logging
            plugin.getLogger().info("[Island Chat - " + island.getName() + "] " + player.getName() + ": " + message);
        }
    }
}
