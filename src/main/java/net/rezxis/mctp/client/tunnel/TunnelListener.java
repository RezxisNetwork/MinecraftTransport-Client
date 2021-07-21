package net.rezxis.mctp.client.tunnel;

import java.io.InputStream;
import java.nio.ByteBuffer;

import net.rezxis.mctp.client.MinecraftTransport;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class TunnelListener implements Runnable {

	private InputStream is;
	
	protected TunnelListener(InputStream is) {
		this.is = is;
	}
	
	public void run() {
		try {
			byte[] buffer = new byte[4096];
			int len;
			while (-1 != (len = is.read(buffer))) {
				ByteBuffer bb = ByteBuffer.wrap(buffer, 0, len);
				if (buffer[0] == 0x1) {
					bb.position(1);
					int ipLen = bb.getInt();
					byte[] ipBuf = new byte[ipLen];
					bb.get(ipBuf, 0, ipLen);
					String ip = new String(ipBuf);
					String msg = "[!] Connect IP Address: "+ip;
					String split = "";
					for (int i = 0; i < msg.length(); i++) {
						split += "-";
					}
					Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + split);
					Bukkit.getConsoleSender().sendMessage("");
					Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + msg);
					Bukkit.getConsoleSender().sendMessage("");
					Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + split);
					MinecraftTransport.ip = ip;
				} else if (buffer[0] == 0x2) {
					bb.position(1);
					int id = bb.getInt();
					new Thread(new TCPTCPTunnel(id)).start();
				}
			}
		} catch (Exception ex) {
			if (!ex.getMessage().contains("Socket closed"))
				ex.printStackTrace();
		}
	}
}
