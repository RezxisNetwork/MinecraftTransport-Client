package net.rezxis.mctp.client.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import net.rezxis.mctp.client.haproxy.HAProxyMessage;
import net.rezxis.mctp.client.haproxy.HAProxyMessageDecoder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.bukkit.Bukkit;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

	public static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().substring(23);
	private static Field fieldL;
	private final ChannelInitializer<SocketChannel> oldChildHandler;
	private final Method oldChildHandlerMethod;
	
	public NettyChannelInitializer(ChannelInitializer<SocketChannel> oldChildHandler) throws Exception {
		this.oldChildHandler = oldChildHandler;
		this.oldChildHandlerMethod = this.oldChildHandler.getClass().getDeclaredMethod("initChannel", Channel.class);
		this.oldChildHandlerMethod.setAccessible(true);
	}
	
	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		this.oldChildHandlerMethod.invoke(this.oldChildHandler, channel);
		
		if (channel.pipeline().get("haproxy-decoder") == null )
			channel.pipeline().addAfter("timeout", "haproxy-decoder", new HAProxyMessageDecoder());
		if (channel.pipeline().get("haproxy-handler") == null )
			channel.pipeline().addAfter("haproxy-decoder", "haproxy-handler", new ChannelInboundHandlerAdapter(){
        	@Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof HAProxyMessage) {
                    HAProxyMessage message = (HAProxyMessage) msg;
                    String realaddress = message.sourceAddress();
                    int realport = message.sourcePort();
                    
                    SocketAddress socketaddr = new InetSocketAddress(realaddress, realport);

            		fieldL.set(channel.pipeline().get("packet_handler"), socketaddr);
                } else {
                    super.channelRead(ctx, msg);
                }
            }
		});
	}
	
	static {
		try {
			Class<?> NetworkManagerClz = Class.forName("net.minecraft.server."+NMS_VERSION+".NetworkManager");
			for (Field field : NetworkManagerClz.getDeclaredFields()) {
				if (field.getType().getName().contains("java.net.SocketAddress")) {
					fieldL = field;
				}
			}
			fieldL.setAccessible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}