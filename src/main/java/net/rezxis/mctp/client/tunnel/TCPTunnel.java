package net.rezxis.mctp.client.tunnel;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import net.rezxis.mctp.client.MinecraftTransport;

public class TCPTunnel {
	
	/*
	 * split - 0x0
	 * start - 0x1
	 * end   - 0x0D,0x0A
	 */
	
	public static void main(String[] args) {
		try {
			build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true) {}
	}
	
	protected static Socket tunnel;
	protected static ArrayList<TCPTCPTunnel> tunnels = new ArrayList<>();
	
	public static void build() throws Exception {
		tunnel = new Socket(MinecraftTransport.config.server,9999);
		sendHeader();
		new Thread(new TunnelListener(tunnel.getInputStream())).start();
	}
	
	public static void sendHeader() throws IOException {
		byte[] header = new byte[] {0x1};
		tunnel.getOutputStream().write(header, 0, header.length);
	}
	
	public static void close() {
		if (tunnel.isConnected())
			try {tunnel.close();} catch(Exception ex) {ex.printStackTrace();}
		for (int i = 0; i < tunnels.size(); i++) {
			tunnels.get(i).close();
		}
	}
}
