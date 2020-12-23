package net.rezxis.mctp.client.tunnel;

import java.io.InputStream;
import java.io.OutputStream;

public class SocketTransporter implements Runnable {

	private InputStream is;
	private OutputStream os;
	private CloseCallback cb;
	private boolean up;
	
	public SocketTransporter(InputStream is, OutputStream os, CloseCallback cb, boolean up) {
		this.is = is;
		this.os = os;
		this.cb = cb;
		this.up = up;
	}

	@Override
	public void run() {
		try {
			byte[] buffer = new byte[4096];
			int len;
			while (-1 != (len = is.read(buffer))) {
				os.write(buffer, 0, len);
			}
		} catch (Exception ex) {
			if (!ex.getMessage().contains("Socket closed"))
				ex.printStackTrace();
		}
		cb.run();
	}
}