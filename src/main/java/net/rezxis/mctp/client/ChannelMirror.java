package net.rezxis.mctp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ChannelMirror extends ChannelInboundHandlerAdapter {

    private final Channel dest;

    public ChannelMirror(Channel dest) {
        this.dest = dest;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!dest.isActive())
            return;
        dest.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!dest.isActive()) {
            ctx.channel().close();
            return;
        }
        dest.writeAndFlush(msg);
    }
}