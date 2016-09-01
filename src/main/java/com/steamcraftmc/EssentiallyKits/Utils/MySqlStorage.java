package com.steamcraftmc.EssentiallyKits.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyKits.MainPlugin;

public class MySqlStorage { 
	final MainPlugin plugin;
	String tablePrefix = "esskits_"; 
    private Connection connection;
    private long lastUsed;
	
	public MySqlStorage(MainPlugin plugin) {
		this.plugin = plugin;
		tablePrefix = plugin.getConfig().getString("mysql.prefix", "esskits_");
	}

	public void reload() {
		tablePrefix = plugin.getConfig().getString("mysql.prefix", "esskits_");
		close();
	}
	
	public void close() {
		try {
	        if (connection != null) {
	        	connection.close();
	        }
        }
        catch (SQLException e) {
        	close();
        }
    	connection = null;
	}

	public Connection connect() {

		//Verify stale connections...
		try {
			if ((connection != null && !connection.isClosed()
				&& (System.currentTimeMillis() - lastUsed) > 1000L * 60)) {
					Statement st = connection.createStatement();
					st.executeQuery("SELECT 1;").close();
					st.close();
			}
		}
		catch(SQLException e) {
			close();
		}
		lastUsed = System.currentTimeMillis();
		
        try
        {
            if (connection == null || connection.isClosed()) {
            	String host = plugin.getConfig().getString("mysql.host");
            	String port = plugin.getConfig().getString("mysql.port");
            	String database = plugin.getConfig().getString("mysql.database");
            	String username = plugin.getConfig().getString("mysql.username");
            	String password = plugin.getConfig().getString("mysql.password");

	            Class.forName("com.mysql.jdbc.Driver");
	            connection = DriverManager.getConnection(
	            		"jdbc:mysql://" + host + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf-8", 
	            		username, password);

	            connection.createStatement().execute(
	        		"CREATE TABLE IF NOT EXISTS `" + tablePrefix + "kitusage` ( \n" +
	                "  `ek_playerUUID` VARCHAR(40) NOT NULL, \n" +
	                "  `ek_playerName` VARCHAR(64) NOT NULL, \n" +
	                "  `ek_kitName` VARCHAR(64) NOT NULL, \n" +
	                "  `ek_lastUsed` BIGINT NOT NULL, \n" +
	                "  `ek_countUsed` INT NOT NULL, \n" +
	                "  PRIMARY KEY (`ek_playerUUID`, `ek_kitName`)); \n" +
	    			"");
            }

            return connection;
        }
        catch (ClassNotFoundException e)
        {
        	plugin.log(Level.SEVERE, "ClassNotFoundException! " + e.getMessage());
        }
        catch (SQLException e)
        {
        	plugin.log(Level.SEVERE, "SQLException! " + e.getMessage());
        	e.printStackTrace();
        	close();
        }
    	return null;
	}
	
	public long getKitLastUsed(Player player, String kit) {
        try
        {
        	String query = "SELECT `ek_lastUsed` FROM `" + tablePrefix + "kitusage` " +
        			"WHERE `ek_playerUUID` = '" + player.getUniqueId().toString() + "' " +
        			"AND `ek_kitName` = '" + kit + "';";
            ResultSet result = connect().createStatement().executeQuery(query);
            if (result.first()) {
            	return result.getLong(1);
            }
            return 0;
        }
        catch (SQLException ex)
        {
        	plugin.log(Level.SEVERE, "Error at SQL Query: " + ex.getMessage());
        	ex.printStackTrace();
            close();
        }
        return System.currentTimeMillis();
	}

	public boolean setKitLastUsed(Player player, String kit, long currentTime) {
        try
        {
        	String query = 
        			"INSERT INTO `" + tablePrefix + "kitusage` \n" +
        	    			"(`ek_playerUUID`, `ek_playerName`, `ek_kitName`, `ek_lastUsed`, `ek_countUsed`) \n" +
        	    			"VALUES ( \n" +
        	    			"'" + player.getUniqueId().toString() + "', \n" +
        	    			"'" + player.getName() + "', \n" +
        	    			"'" + kit + "', \n" +
        	    			currentTime + ", \n" +
        	    			"1 ) \n" +
        	    			"ON DUPLICATE KEY UPDATE \n" +
        	    			"`ek_playerName` = '" + player.getName() + "', \n" +
        	    			"`ek_lastUsed` = '" + currentTime + "', \n" +
        	    			"`ek_countUsed` = `ek_countUsed` + 1; \n";
        	    			
            connect().createStatement().executeUpdate(query);
            return true;
        }
        catch (SQLException ex)
        {
        	plugin.log(Level.SEVERE, "Error at SQL Query: " + ex.getMessage());
        	ex.printStackTrace();
            close();
        }
        return false;
	}
}
