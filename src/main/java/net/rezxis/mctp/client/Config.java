package net.rezxis.mctp.client;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
	
	public String server;
	
	public Config(JavaPlugin plugin) throws Exception {
		FileConfiguration configuration = new YamlConfiguration();
		File file = new File(plugin.getDataFolder(),"mctp.yml");
		if (!file.exists()) {
			configuration.set("server", "mctp.lunac.xyz");
			configuration.save(file);
		} else {
			configuration.load(file);
			server = configuration.getString("server");
		}
	}
}
