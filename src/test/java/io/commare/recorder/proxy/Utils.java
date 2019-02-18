package io.commare.recorder.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;


public class Utils {

    public static ByteBuf encodeCics(String message) {
        return Unpooled.copiedBuffer(message.getBytes(Charset.forName("IBM01147")));
    }

    public static String decodeCics(final ByteBuf buf) {
        return new String(ByteBufUtil.getBytes(buf), Charset.forName("IBM01147"));
    }

    private Utils() {
    }
}
