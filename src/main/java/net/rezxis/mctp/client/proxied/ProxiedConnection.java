package net.rezxis.mctp.client.proxied;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.rezxis.mctp.client.ChannelMirror;
import net.rezxis.mctp.client.MCTPVars;
import net.rezxis.mctp.client.MinecraftTransport;
import net.rezxis.mctp.client.util.PacketEncoder;

import java.util.ArrayList;

@ChannelHandler.Sharable
public class ProxiedConnection extends ChannelInboundHandlerAdapter implements Runnable {

    private long secret;
    private long id;
    private Channel mctp;
    private Channel minecraft;

    public ProxiedConnection(long secret, long id) {
        this.secret = secret;
        this.id = id;
    }

    @Override
    public void run() {
        Bootstrap bs1 = new Bootstrap();
        Bootstrap bs2 = new Bootstrap();
        EventLoopGroup worker = new NioEventLoopGroup();
        try{
            bs1.channel(NioSocketChannel.class)
                    .group(worker)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new PacketEncoder());
                            ch.pipeline().addLast(ProxiedConnection.this);
                        }
                    });
            mctp = bs1.connect(MinecraftTransport.config.host, MinecraftTransport.config.port).sync().channel();
            sendUpgradePacket(secret,id);
            mctp.pipeline().remove(PacketEncoder.class);
            bs2.channel(NioSocketChannel.class)
                    .group(worker)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ChannelMirror(mctp));
                        }
                    });
            minecraft = bs2.connect("127.0.0.1", MinecraftTransport.instance.getServer().getPort()).sync().channel();
            mctp.pipeline().remove(ProxiedConnection.class);
            for (ByteBuf buf : mctp.attr(MCTPVars.PACKET_STACK).get()) {
                minecraft.writeAndFlush(buf);
            }
            mctp.pipeline().addLast(new ChannelMirror(minecraft));
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private void sendUpgradePacket(long secret, long id) {
        int size = 17;
        ByteBuf packet = Unpooled.buffer(size,size);
        packet.writeByte(MCTPVars.CODE_UPGRADE);
        packet.writeLong(secret);
        packet.writeLong(id);
        mctp.writeAndFlush(packet);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.attr(MCTPVars.PACKET_STACK).set(new ArrayList<>());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.attr(MCTPVars.PACKET_STACK).get().add((ByteBuf)msg);
    }
}
