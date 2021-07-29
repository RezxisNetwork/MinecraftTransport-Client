package net.rezxis.mctp.client;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
	
	public String host = "mctp1.nohit.cc";
	public int port = 9998;
	
	public Config(JavaPlugin plugin) throws Exception {
		FileConfiguration configuration = new YamlConfiguration();
		File file = new File(plugin.getDataFolder(),"mctp.yml");
		if (!file.exists()) {
			configuration.set("host", host);
			configuration.set("port", port);
			configuration.save(file);
		} else {
			configuration.load(file);
			host = configuration.getString("host");
			port = configuration.getInt("port");
		}
	}
}
