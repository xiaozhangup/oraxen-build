package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

public class CustomArmorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomArmorRepair(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        Player player = (Player) inventory.getViewers().stream().filter(p -> p.getOpenInventory().getTopInventory() == inventory).findFirst().orElse(null);
        if (player == null) return;
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);
        String firstID = OraxenItems.getIdByItem(first);
        String secondID = OraxenItems.getIdByItem(second);

        if (first == null || second == null) return; // Empty slot
        if (firstID == null) return; // Not a custom item
        Material type = first.getType();
        if (type != Material.LEATHER_HELMET && type != Material.LEATHER_CHESTPLATE && type != Material.LEATHER_LEGGINGS && type != Material.LEATHER_BOOTS)
            return; // Not a custom armor

        if (second.getType() == Material.LEATHER || (!firstID.equals(secondID) && secondID != null)) {
            event.setResult(null);
            player.updateInventory();
        }
    }
}
