package com.steamcraftmc.EssentiallyKits;

import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PluginEventListener implements Listener {
	private final MainPlugin  plugin;

	public PluginEventListener(MainPlugin plugin) {
		this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
    	plugin.log(Level.INFO, "Player " + event.getPlayer().getName() + " joined.");
    }

	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
    }
}
