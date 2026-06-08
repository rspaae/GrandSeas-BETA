package me.rspaae.grandseas.manager;

import me.rspaae.grandseas.gui.AbstractGUI;
import org.bukkit.entity.Player;

public class GUIManager {

    /**
     * Opens a specific GUI for a player.
     *
     * @param player The player to open the GUI for
     * @param gui    The GUI to open
     */
    public void openGUI(Player player, AbstractGUI gui) {
        player.openInventory(gui.getInventory());
    }
}
