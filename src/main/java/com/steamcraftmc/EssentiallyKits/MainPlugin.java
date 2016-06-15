package com.steamcraftmc.EssentiallyKits;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
 
public class MainPlugin extends JavaPlugin {
	//private PluginEventListener _listener;
	public final   Logger  _logger;
	public Boolean _exLogging;

	public MainPlugin() {
		_exLogging = true;
		_logger = getLogger();
		_logger.setLevel(Level.ALL);
		_logger.log(Level.CONFIG, "Plugin initializing...");
		
		this.getConfig().options().copyDefaults(true);
    	//_listener = new PluginEventListener(this);
	}

	public void log(Level level, String text) {
		_logger.log(Level.INFO, text);
	}

    @Override
    public void onEnable() {
        File cFile = new File(getDataFolder(), "config.yml");
        if (!cFile.exists()) {
            cFile.getParentFile().mkdirs();
            createConfigFile(getResource("config.yml"), cFile);
            log(Level.INFO, "Configuration file config.yml created!");
        }

        new com.steamcraftmc.EssentiallyKits.Commands.KitCommand(this);
        
        //getServer().getPluginManager().registerEvents(_listener, this);
		//_logger.log(Level.CONFIG, "Plugin listening for events.");
    }

    private void createConfigFile(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (IOException e) {
        	log(Level.SEVERE, e.toString());
        }
    }

    @Override
    public void onDisable() {
    	//HandlerList.unregisterAll(_listener);
    }

}
