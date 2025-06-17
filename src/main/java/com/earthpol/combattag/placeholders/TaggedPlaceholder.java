package com.earthpol.combattag.placeholders;

import com.earthpol.combattag.combat.CombatHandler;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class TaggedPlaceholder extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "combattag"; // %myplugin_...%
    }

    @Override
    public String getAuthor() {
        return "EarthPol";
    }

    @Override
    public String getVersion() {
        return "2.0.3";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("is_tagged")) {
            if (CombatHandler.isTagged(player)) {
                return "true";
            }
            return "false";
        }
        if(identifier.equals("time_left")) {
            if (CombatHandler.getRemaining(player) != -1) {
                return "" + CombatHandler.getRemaining(player);
            }
            return "-1";
        }

        return null; // Unknown placeholder
    }
}