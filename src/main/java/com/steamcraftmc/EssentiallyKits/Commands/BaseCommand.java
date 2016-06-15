package com.steamcraftmc.EssentiallyKits.Commands;

import java.util.logging.Level;

import com.steamcraftmc.EssentiallyKits.MainPlugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {

    protected final MainPlugin plugin;
    protected final String permission;
    protected final String cmdName;
    private final int minArgs;
    private final int maxArgs;

    public BaseCommand(MainPlugin plugin, String permission, String command, int minArg, int maxArg) {
        this.plugin = plugin;
        this.permission = permission;
        this.cmdName = command;
        this.minArgs = minArg;
        this.maxArgs = maxArg;
    	this.plugin.getCommand(this.cmdName).setExecutor(this);
    	this.plugin.getCommand(this.cmdName).setPermission(permission);
    }
    
    protected abstract boolean doPlayerCommand(Player player, Command cmd, String[] args) throws Exception;
    
    protected boolean doConsoleCommand(CommandSender sender, Command cmd, String[] args) throws Exception {
    	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Command should be used by a player."));
    	return true;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        
        if (cmd == null || !cmdName.equalsIgnoreCase(cmd.getName()) || 
        	args == null || args.length < minArgs || args.length > maxArgs) {
            return false;
        }

    	try {
	        if (sender instanceof Player) {
		        Player player = (Player) sender;
		        
		        if (!sender.hasPermission(permission)) {
		            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4You do not have permission to this command."));
		            plugin.log(Level.SEVERE, "The user " + player.getName() + " need permission " + permission + " to access to the command " + cmdName);
		            return true;
		        }
		        
					return doPlayerCommand(player, cmd, args);
	        }
	        else {
	        	return doConsoleCommand(sender, cmd, args);
	        }
		} catch (Exception e) {
			plugin.log(Level.SEVERE, e.toString());
			e.printStackTrace();
			sender.sendMessage(e.getMessage());
			return true;
		}
    }
}
