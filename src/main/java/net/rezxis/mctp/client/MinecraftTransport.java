package net.rezxis.mctp.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import net.rezxis.mctp.client.netty.NettyChannelInitializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftTransport extends JavaPlugin {

	public static MinecraftTransport instance;
	public static String ip;
	public static Config config;
	public static MCTPClient client;
	
	public void onLoad(){
		instance = this;
		try {
			getLogger().info("Loading Config...");
			config = new Config(this);
			getLogger().info("MCTP-Host : "+config.host+":"+config.port);
			getLogger().info("Loaded config!");
		} catch (Exception e1) {
			getLogger().warning("Failed to load config!");
			e1.printStackTrace();
		}
		try {
			getLogger().info("Injecting NettyHandler...");
			inject();
			getLogger().info("Injected NettyHandler!");
		} catch (Exception e) {
			getLogger().warning("Failed to inject NettyHandler!");
			e.printStackTrace();
		}
		client = new MCTPClient(config.host,config.port);
		new Thread(client).start();
	}

	public void onEnable() {
		if (ip != null)
			getLogger().info(ip+" is assigned address to this server.");
	}
	
	public void onDisable() {
		client.close();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("checkip")) {
			sender.sendMessage(ip+" is assigned address to this server.");
		}
		return true;
	}

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
		Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
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
