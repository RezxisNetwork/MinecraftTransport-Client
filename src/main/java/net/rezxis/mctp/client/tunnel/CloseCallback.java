package net.rezxis.mctp.client.tunnel;

public class CloseCallback implements Runnable {

	private TCPTCPTunnel tunnel;
	
	public CloseCallback(TCPTCPTunnel tunnel) {
		this.tunnel = tunnel;
	}
	
	@Override
	public void run() {
		if (tunnel.spigot.isConnected())
			try { tunnel.spigot.close(); } catch (Exception ex) {ex.printStackTrace();}
		if (tunnel.socket.isConnected())
			try { tunnel.socket.close(); } catch (Exception ex) {ex.printStackTrace();}
		if (TCPTunnel.tunnels.contains(tunnel))
			TCPTunnel.tunnels.remove(tunnel);
	}
}
