package net.rezxis.mctp.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import net.rezxis.mctp.client.netty.NettyChannelInitializer;
import net.rezxis.mctp.client.tunnel.TCPTunnel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftTransport extends JavaPlugin {
	
	public static String ip;
	public static Config config;
	
	public void onLoad(){
		try {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "============================================================");
			Bukkit.getConsoleSender().sendMessage("");
			Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE +" M C T P - P R O J E C T");
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Publish a server without opening ports.");
			Bukkit.getConsoleSender().sendMessage("");
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Forked and operated by Lunac.");
			Bukkit.getConsoleSender().sendMessage("");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "============================================================");
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Loading config...");
			config = new Config(this);
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Relay Host: " + config.server);
			if (config.server == null) Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Config has been generated. Please reboot the server!");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Config loaded!");
		} catch (Exception e1) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to load Config. Please report any problems to the developers.");
			e1.printStackTrace();
		}
		try {
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Injecting NettyHandler.");
			inject();
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "NettyHandler injected!");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to inject NettyHandler.");
			e.printStackTrace();
		}
		try {
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Building tunnel.");
			TCPTunnel.build();
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Built tunnel!");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to create the tunnel. > " + e);
		}
	}
	
	public void onDisable() {
		TCPTunnel.close();
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("checkip")) {
			sender.sendMessage(ip+" is assigned to this server.");
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private void inject() throws Exception {
		Method serverGetHandle = Bukkit.getServer().getClass().getDeclaredMethod("getServer");
		Object minecraftServer = serverGetHandle.invoke(Bukkit.getServer());
		
		Method serverConnectionMethod = null;
		for(Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
			if(!method.getReturnType().getSimpleName().equals("ServerConnection")) {
				continue;
			}
			serverConnectionMethod = method;
			break;
		}
		Object serverConnection = Objects.requireNonNull(serverConnectionMethod).invoke(minecraftServer);
		List<ChannelFuture> channelFutureList = null;
		for (Field field : serverConnection.getClass().getDeclaredFields()) {
			if (field.getType().getName().contains("List") ) {
				if (((Class<?>)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0]).getName().contains("ChannelFuture")) {
					field.setAccessible(true);
					channelFutureList = (List<ChannelFuture>) field.get(serverConnection);
				}
			}
		}
		if (channelFutureList == null) {
			throw new Exception("Failed to get channelFutureList.");
		}
		
		for (ChannelFuture channelFuture : channelFutureList) {
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			ChannelHandler serverBootstrapAcceptor = channelPipeline.first();
			System.out.println(serverBootstrapAcceptor.getClass().getName());
			ChannelInitializer<SocketChannel> oldChildHandler = ReflectionUtils.getPrivateField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, ChannelInitializer.class, "childHandler");
			if (oldChildHandler instanceof NettyChannelInitializer)
				break;
			ReflectionUtils.setFinalField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, "childHandler", new NettyChannelInitializer(oldChildHandler));
		}
	}
}
