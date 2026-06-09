package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.config.GrandSeasSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class AcidRainListener extends BukkitRunnable {

    private final GrandSeas plugin;
    private final GrandSeasSettings settings;
    private final String islandWorldName;

    public AcidRainListener(GrandSeas plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.islandWorldName = settings.getWorldName();
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(islandWorldName)) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (player.isOp() && !settings.isAcidDamageOp()) {
                continue;
            }
            if (!player.getWorld().hasStorm() && !player.getWorld().isThundering()) {
                continue;
            }
            if (player.isInWater() || player.isInsideVehicle()) {
                continue;
            }
            if (player.getLocation().getBlock().getLightFromSky() <= 0) {
                continue;
            }

            // Helm Kura-Kura memberikan perlindungan 100% dari Hujan Asam
            if (hasTurtleHelmet(player)) {
                continue;
            }
            if (settings.isFullArmorProtectsFromAcid() && hasFullArmor(player)) {
                continue;
            }

            double rainDamage = settings.getAcidRainDamage();
            
            // Check if player is in a cold biome where it snows instead of raining
            double temp = player.getLocation().getBlock().getTemperature();
            boolean isSnowing = temp < 0.15;
            
            if (isSnowing && !settings.isAcidSnowDamage()) {
                continue;
            }
            
            if (rainDamage <= 0) {
                continue;
            }

            player.damage(rainDamage);
            player.sendActionBar(Component.text("☂ HUJAN ASAM! ☂", NamedTextColor.DARK_RED));
            player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.3f, 0.8f);

            int ticks = settings.getRainEffectDurationSeconds() * 20;
            for (PotionEffectType type : settings.getRainEffects()) {
                player.addPotionEffect(new PotionEffect(type, ticks, 0, false, false, false));
            }
        }
    }

    private boolean hasTurtleHelmet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && helmet.getType() == Material.TURTLE_HELMET;
    }

    private boolean hasFullArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor.length < 4) {
            return false;
        }
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }
}
