package net.rezxis.mctp.client.tunnel;

import java.net.Socket;
import java.nio.ByteBuffer;

import org.bukkit.Bukkit;

import net.rezxis.mctp.client.MinecraftTransport;

public class TCPTCPTunnel implements Runnable {

	protected int id;
	protected Socket socket;
	protected Socket spigot;
	
	protected TCPTCPTunnel(int id) {
		this.id = id;
	}
	
	@Override
	public void run() {
		try {
			socket = new Socket(MinecraftTransport.config.server, 9999);
			socket.setTcpNoDelay(true);
			byte[] header = ByteBuffer.allocate(5).put((byte)0x2).putInt(id).array();
			socket.getOutputStream().write(header, 0, header.length);
			
			String host = "127.0.0.1";
			if (Bukkit.getServer().getIp() != null && !Bukkit.getServer().getIp().isEmpty()) {
				host = Bukkit.getServer().getIp();
			}
			spigot = new Socket(host, Bukkit.getServer().getPort());
			spigot.setTcpNoDelay(true);
			new Thread(new SocketTransporter(spigot.getInputStream(), socket.getOutputStream(), new CloseCallback(this),true)).start();
			new Thread(new SocketTransporter(socket.getInputStream(), spigot.getOutputStream(), new CloseCallback(this),true)).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		if (socket.isConnected())
			try {socket.close();} catch (Exception ex) {ex.printStackTrace();}
		if (spigot.isConnected())
			try {spigot.close();} catch (Exception ex) {ex.printStackTrace();}
	}
}
