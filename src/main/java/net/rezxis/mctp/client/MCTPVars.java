package net.rezxis.mctp.client;

import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;

import java.util.ArrayList;

public class MCTPVars {

    public static final int CODE_INIT = 0x00;
    public static final int CODE_READY = 0x01;
    public static final int CODE_NEW = 0x02;
    public static final int CODE_UPGRADE = 0x03;

    public static final AttributeKey<ArrayList<ByteBuf>> PACKET_STACK = AttributeKey.newInstance("PACKET_STACK");
}
