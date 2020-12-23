package net.rezxis.mctp.client.tunnel;

import java.io.InputStream;
import java.nio.ByteBuffer;

import net.rezxis.mctp.client.MinecraftTransport;

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
					String msg = "## reserved ip:"+ip+ " ##";
					String split = "";
					for (int i = 0; i < msg.length(); i++) {
						split += "#";
					}
					System.out.println(split);
					System.out.println(msg);
					System.out.println(split);
					MinecraftTransport.ip = ip;
				} else if (buffer[0] == 0x2) {
					bb.position(1);
					int id = bb.getInt();
					new Thread(new TCPTCPTunnel(id)).start();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
