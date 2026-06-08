package me.rspaae.grandseas.task;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.config.GrandSeasSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AcidDamageTask extends BukkitRunnable {

    private final GrandSeas plugin;
    private final GrandSeasSettings settings;
    private final String islandWorldName;
    private final Map<UUID, Long> enteredWaterAt = new HashMap<>();

    public AcidDamageTask(GrandSeas plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.islandWorldName = settings.getWorldName();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long delayMs = settings.getAcidBurnDelaySeconds() * 1000L;
        int effectTicks = settings.getAcidEffectDurationSeconds() * 20;

        // Cleanup memory leak (hapus UUID pemain yang sudah offline)
        enteredWaterAt.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                enteredWaterAt.remove(player.getUniqueId());
                continue;
            }

            if (!player.getWorld().getName().equals(islandWorldName)) {
                enteredWaterAt.remove(player.getUniqueId());
                continue;
            }

            if (player.isOp() && !settings.isAcidDamageOp()) {
                enteredWaterAt.remove(player.getUniqueId());
                continue;
            }

            if (settings.isFullArmorProtectsFromAcid() && hasFullArmor(player)) {
                enteredWaterAt.remove(player.getUniqueId());
                continue;
            }

            UUID uid = player.getUniqueId();

            if (player.isInWater()) {
                Long entered = enteredWaterAt.get(uid);
                if (entered == null) {
                    enteredWaterAt.put(uid, now);
                    continue;
                }
                if (now - entered < delayMs) {
                    continue;
                }

                double damage = getAcidDamage(player);
                player.damage(damage);
                player.sendActionBar(Component.text("☠ THE ACID IS BURNING YOU! ☠", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.5f);
                applyEffects(player, settings.getAcidEffects(), effectTicks);
            } else {
                enteredWaterAt.remove(uid);
            }
        }
    }

    private void applyEffects(Player player, List<PotionEffectType> types, int durationTicks) {
        for (PotionEffectType type : types) {
            int amplifier = type == PotionEffectType.NAUSEA ? 1 : 0;
            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false, false));
        }
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

    private double getAcidDamage(Player player) {
        double damage = 2.0; // Base damage (tanpa armor)
        ItemStack[] armor = player.getInventory().getArmorContents();
        
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR) continue;
            String name = piece.getType().name();
            
            // Pengurangan damage per piece (dikali 4 jika full set)
            if (name.contains("LEATHER_")) damage -= 0.075;     // Full set: -0.3  -> Total: 1.7
            else if (name.contains("CHAINMAIL_")) damage -= 0.1; // Full set: -0.4  -> Total: 1.6
            else if (name.contains("GOLDEN_")) damage -= 0.125;  // Full set: -0.5  -> Total: 1.5
            else if (name.contains("IRON_")) damage -= 0.15;     // Full set: -0.6  -> Total: 1.4
            else if (name.contains("DIAMOND_")) damage -= 0.25;  // Full set: -1.0  -> Total: 1.0
            else if (name.contains("NETHERITE_")) damage -= 0.35;// Full set: -1.4  -> Total: 0.6
        }
        
        return Math.max(0.2, damage); // Pastikan minimal damage 0.2 (biar tetap ada efek) walau pakai armor dewa
    }
}
