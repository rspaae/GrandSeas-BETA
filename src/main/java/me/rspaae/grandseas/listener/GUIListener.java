package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.gui.AbstractGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof AbstractGUI) {
            event.setCancelled(true);
            AbstractGUI gui = (AbstractGUI) holder;
            gui.handleClick(event);
        }
    }
}
