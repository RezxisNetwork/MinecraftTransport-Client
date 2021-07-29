package net.rezxis.mctp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.rezxis.mctp.client.proxied.ProxiedConnection;
import net.rezxis.mctp.client.util.PacketDecoder;
import net.rezxis.mctp.client.util.PacketEncoder;
import org.bukkit.Bukkit;

@ChannelHandler.Sharable
public class MCTPClient extends ChannelInboundHandlerAdapter implements Runnable {

    private Channel channel;
    private String host;
    private int port;

    public MCTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        Bootstrap bs = new Bootstrap();
        EventLoopGroup worker = new NioEventLoopGroup();
        try{
            bs.channel(NioSocketChannel.class)
                    .group(worker)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new PacketEncoder());
                            ch.pipeline().addLast(new PacketDecoder(), MCTPClient.this);
                        }
                    });
            channel = bs.connect(host, port).sync().channel();
        } catch(Exception e) {
            e.printStackTrace();
            MinecraftTransport.instance.getLogger().warning("failed to connect to MCTP server. reconnecting after 5 seconds.");
            Bukkit.getScheduler().runTaskLaterAsynchronously(MinecraftTransport.instance, new Runnable() {
                @Override
                public void run() {
                    MCTPClient.this.run();
                }
            }, 20 * 5);
            return;
        }
        sendInitPacket();
        MinecraftTransport.instance.getLogger().info("connected to MCTP server.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        int op = buf.readByte();
        if (op == MCTPVars.CODE_READY) {
            String host = readString(buf);
            int port = buf.readInt();
            MinecraftTransport.ip = host + ":" + port;
            MinecraftTransport.instance.getLogger().info("connection has been authenticated.");
            MinecraftTransport.instance.getLogger().info(host + ":" + port+" is assigned address to this server.");
        } else if (op == MCTPVars.CODE_NEW) {
            long secret = buf.readLong();
            long id = buf.readLong();
            new Thread(new ProxiedConnection(secret,id)).start();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!MinecraftTransport.instance.disabling) {
            MinecraftTransport.instance.getLogger().warning("disconnected from MCTP server. reconnecting after 5 seconds.");
            Bukkit.getScheduler().runTaskLaterAsynchronously(MinecraftTransport.instance, new Runnable() {
                @Override
                public void run() {
                    MCTPClient.this.run();
                }
            }, 20 * 5);
        }
    }

    private String readString(ByteBuf buf){
        int length = buf.readInt();

        byte[] strBuf = new byte[length];

        buf.readBytes(strBuf);

        return new String(strBuf);
    }

    private ByteBuf createPacket(int size){
        return Unpooled.buffer(size, size);
    }

    private void sendInitPacket() {
        ByteBuf packet = createPacket(1);
        packet.writeByte(MCTPVars.CODE_INIT);
        channel.writeAndFlush(packet);
    }

    public void close() {
        try {
            channel.close().sync().awaitUninterruptibly();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
