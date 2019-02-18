package io.commare.recorder.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Base64;


public class EncoderTest {

    @Test
    public void shouldDecodeCics() {
        String commarea = "commarea";
        ByteBuf b = Utils.encodeCics(commarea);
        Assert.assertEquals(commarea, new Engine(2000, "localhost", 3000, null).decodeCics(b));
    }

    @Test
    public void shouldConvertBufToBase64() {
        byte[] data = "commarea".getBytes(Charset.forName("IBM01147"));
        ByteBuf b = Unpooled.copiedBuffer(data);
        Assert.assertEquals(Base64.getEncoder().encodeToString(data), new Engine(2000, "localhost", 3000, null).toBase64(b));
    }

}
