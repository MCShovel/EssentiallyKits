package com.steamcraftmc.EssentiallyKits.Commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.steamcraftmc.EssentiallyKits.MainPlugin;
import com.steamcraftmc.EssentiallyKits.Utils.MySqlStorage;

public class KitCommand extends BaseCommand {

	final MySqlStorage storage;
	Map<String, ConfigurationSection> allKits;

	public KitCommand(MainPlugin plugin) {
		super(plugin, "essentials.kit", "kit", 0, 1);
		storage = new MySqlStorage(plugin);
	}

	boolean enabled() {
		
		if (allKits != null)
			return allKits.size() >= 0;

        allKits = new HashMap<String, ConfigurationSection>();

    	plugin.log(Level.FINE, "Loading kits...");
        try {
            ConfigurationSection kits = plugin.getConfig().getConfigurationSection("kits");
	        if (kits != null) {
		        Set<String> custom = kits.getKeys(false);
		        if (custom != null) {
			        for (String kit : custom) {
		        		allKits.put(kit, kits.getConfigurationSection(kit));
			        }
		        }
	        }
        }
        catch(Exception e) {
        	e.printStackTrace();
        }

    	plugin.log(Level.INFO, "Loaded kits: " + String.valueOf(allKits.size()));
		return allKits.size() >= 0;
	}	

	@Override
	protected boolean doPlayerCommand(Player player, Command cmd, String[] args) throws Exception {
        if (!enabled()) {
        	return true;
        }
		String kit = (args.length > 0 ? args[0] : "").toLowerCase();

		
        if (kit.equalsIgnoreCase("reload") && player.hasPermission("*")) {
    		plugin.reloadConfig();
    		allKits = null;
    		storage.reload();
    		player.sendMessage(ChatColor.GOLD + "EssentiallyKits configuration reloaded.");
    		return true;
        }
        
        if (kit == null || kit.equalsIgnoreCase("")) {
        	List<String> kits = new ArrayList<String>(allKits.keySet());
        	Collections.sort(kits, String.CASE_INSENSITIVE_ORDER);
        	for (int i = kits.size()-1; i >= 0; i--) {
        		kit = kits.get(i);
        		ConfigurationSection kitcfg = allKits.get(kit);
                if (!hasPermission(player, kit, kitcfg)) {
            		kits.remove(i);
            		continue;
                }
                long duration = getKitDelay(player, kitcfg);
                long lastUsed = storage.getKitLastUsed(player, kit);
                if (duration < 0 && lastUsed > 0) {
                	//use-once, and already used.
            		kits.remove(i);
            		continue;
                }
                else if ((lastUsed + duration) > System.currentTimeMillis()) {
                	kits.set(i, ChatColor.STRIKETHROUGH + kit + ChatColor.RESET);
                }
        	}
        	
        	player.sendMessage(kitList(String.join(", ", kits)));
        	return true;
        }
        else if (allKits.containsKey(kit.toLowerCase())) {
        	ConfigurationSection cfg = allKits.get(kit);

            if (!hasPermission(player, kit, cfg)) {
            	player.sendMessage(kitNoAccess());
            	return true;
            }
            
            long duration = getKitDelay(player, cfg);
            long lastUsed = storage.getKitLastUsed(player, kit);
            if (duration < 0 && lastUsed > 0) {
            	player.sendMessage(kitNoAccess());
            	return true;
            }
            else if ((lastUsed + duration) > System.currentTimeMillis()) {
            	long seconds = ((lastUsed + duration) - System.currentTimeMillis()) / 1000;
            	int time;
            	String unit;
            	if (seconds > 60*60) {
            		time = (int)(seconds / (60*60));
            		unit = "hour";
            	} else if (seconds > 60) {
            		time = (int)(seconds/60);
            		unit = "minute";
            	} else {
            		time = Math.max(1, (int)seconds);
            		unit = "second";
            	}
            	if (time > 1) unit += "s";
            	
            	player.sendMessage(kitTimeRemaining(kit, time, unit));
            	return true;
            }
            if (duration != 0 && !storage.setKitLastUsed(player, kit, System.currentTimeMillis())) {
            	player.sendMessage(kitNoAccess());
            	return true;
            }

        	String announce = formatForPlayer(player, cfg.getString("announce"));
        	if (announce != null && !announce.equalsIgnoreCase("")) {
        		Bukkit.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', announce));
        	}
        	String message = formatForPlayer(player, cfg.getString("message"));
        	if (message != null && !message.equalsIgnoreCase("")) {
        		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        	}

        	boolean dropped = false;
        	List<String> actions = cfg.getStringList("actions");
        	for (int ix = 0; ix < actions.size(); ix++) {
        		String cmdText = formatForPlayer(player, actions.get(ix));
        		plugin.log(Level.INFO, "Execute: " + cmdText);
        		dropped |= !runCommand(cmdText);
        	}

        	player.sendMessage(kitReceived(kit));
        	if (dropped) {
            	player.sendMessage(inventoryFull());
        	}
        	return true;
        }
        else {
        	player.sendMessage(kitNotFound(kit));
        }

        return true;
	}

    private long getKitDelay(Player player, ConfigurationSection kitcfg) {
    	if (player.hasPermission("essentials.kit.exemptdelay")) {
    		return 0;
    	}
    	long result = kitcfg.getInt("delay", 0);
    	return result * 1000L;
	}

	@SuppressWarnings("deprecation")
	private boolean runCommand(String cmdText) {
		if (cmdText.startsWith("/")) {
			cmdText = cmdText.substring(1);
		}
		
    	String[] args = cmdText.split("\\s+", 6);
    	if (args.length > 2 && args[0].equalsIgnoreCase("give")) {
			Player player = Bukkit.getPlayer(args[1]);
			if (player != null) {
	            Material material = Material.matchMaterial(args[2]);
	
	            if (material == null) {
	                material = Bukkit.getUnsafe().getMaterialFromInternalName(args[2]);
	            }
	
	            if (material != null) {
	                int amount = 1;
	                short data = 0;
	
	                if (args.length > 3) {
	                	try {
	                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
	                	}
                        catch (NumberFormatException e) { 
	                    	plugin.log(Level.SEVERE, "Invalid quantity: " + cmdText);
	                    	amount = 1; 
                    	}
	                }
	            	
                    if (args.length > 4) {
                        try {
                            data = Short.parseShort(args[4]);
                        } 
                        catch (NumberFormatException e) { 
	                    	plugin.log(Level.SEVERE, "Invalid data: " + cmdText);
                        	data = 0; 
                    	}
                    }
	
	                ItemStack stack = new ItemStack(material, amount, data);
	
	                if (args.length > 5) {
	                    try {
	                        stack = Bukkit.getUnsafe().modifyItemStack(stack, args[5]);
	                    } catch (Throwable t) {
	                    	plugin.log(Level.SEVERE, "Invalid tag: " + cmdText);
	                    }
	                }
	
	                PlayerInventory inv = player.getInventory();
	                int emptyIx = inv.firstEmpty();
	                if (emptyIx >= 0) {
	                	inv.setItem(emptyIx, stack);
		                return true;
	                }

                	player.getWorld().dropItemNaturally(player.getLocation(), stack);
	                return false;
	            }
	            else {
                	plugin.log(Level.SEVERE, "Unknown material: " + args[2]);
	            }
			}
    	}
    	
    	Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmdText);
        return true;
	}

	private String formatForPlayer(Player player, String text) {
    	if (text == null) {
    		return null;
    	}
		text = text.replace("@p", player.getName());
		Block loc = player.getLocation().getBlock(); 
		text = text.replace("@x", String.valueOf(loc.getX()));
		text = text.replace("@y", String.valueOf(loc.getY()));
		text = text.replace("@z", String.valueOf(loc.getZ()));
		text = ChatColor.translateAlternateColorCodes('&', text);
		return text;
	}

	private boolean hasPermission(Player user, String kit, ConfigurationSection cfg) {
		String permName = cfg.getString("permission", "essentials.kits." + kit);
		return user.hasPermission(permName) || user.hasPermission("essentials.kits.*");
	}

	private String kitList(String kits) {
    	String msg = plugin.getConfig().getString("formatting.kitList", "&6Kits&f: {kits}");
    	msg = msg.replace("{kits}", kits);
    	return ChatColor.translateAlternateColorCodes('&', msg);
    }

	private String kitReceived(String kit) {
		String msg = plugin.getConfig().getString("formatting.kitReceived", "&6Received kit &c{kit}&6.");
    	msg = msg.replace("{kit}", kit);
    	return ChatColor.translateAlternateColorCodes('&', msg);
	}

	private String kitNoAccess() {
    	String msg = plugin.getConfig().getString("formatting.kitNoAccess", "&cYou do not have access to that kit.");
    	return ChatColor.translateAlternateColorCodes('&', msg);
    }

	private String kitTimeRemaining(String kit, int time, String unit) {
    	String msg = plugin.getConfig().getString("formatting.kitTimeRemaining", "&cPlease wait {time} {unit} before using that.");
    	msg = msg.replace("{kit}", kit);
    	msg = msg.replace("{time}", String.valueOf(time));
    	msg = msg.replace("{unit}", unit);
    	return ChatColor.translateAlternateColorCodes('&', msg);
    }

	private String kitNotFound(String kit) {
    	String msg = plugin.getConfig().getString("formatting.kitNotFound", "&6Kit &c{kit}&6 not found.");
    	msg = msg.replace("{kit}", kit);
    	return ChatColor.translateAlternateColorCodes('&', msg);
    }

	private String inventoryFull() {
		String msg = plugin.getConfig().getString("formatting.inventoryFull", "&cYou inventory was full, some items are on the ground.");
    	return ChatColor.translateAlternateColorCodes('&', msg);
	}

}
