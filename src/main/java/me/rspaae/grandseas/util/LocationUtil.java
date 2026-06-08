package me.rspaae.grandseas.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    public static String serialize(Location location) {
        if (location == null) return null;
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "world";
        return worldName + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ() + ";" + location.getYaw() + ";" + location.getPitch();
    }

    public static Location deserialize(String serialized) {
        if (serialized == null || serialized.isEmpty()) return null;
        String[] parts = serialized.split(";");
        if (parts.length < 4) return null;

        try {
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = 0f;
            float pitch = 0f;

            if (parts.length >= 6) {
                yaw = Float.parseFloat(parts[4]);
                pitch = Float.parseFloat(parts[5]);
            }

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
