package me.rspaae.grandseas.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public class MessageUtil {
    private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINIMESSAGE.deserialize(text);
    }

    public static Component parse(String text, TagResolver... placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINIMESSAGE.deserialize(text, placeholders);
    }
    
    public static TagResolver placeholder(String key, String value) {
        return Placeholder.parsed(key, value);
    }

    public static TagResolver placeholder(String key, Component value) {
        return Placeholder.component(key, value);
    }

    public static void sendMessage(CommandSender sender, String text) {
        if (sender != null && text != null && !text.isEmpty()) {
            sender.sendMessage(parse(text));
        }
    }

    public static void sendMessage(CommandSender sender, String text, TagResolver... placeholders) {
        if (sender != null && text != null && !text.isEmpty()) {
            sender.sendMessage(parse(text, placeholders));
        }
    }
}
