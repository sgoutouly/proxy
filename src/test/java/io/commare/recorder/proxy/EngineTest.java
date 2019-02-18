package io.commare.recorder.proxy;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class EngineTest {

    @Test
    public void shouldForwardToServer() {
        final String commarea = "commarea";
        final ByteBuf b = Utils.encodeCics(commarea);

        String response = new Engine(2000, "localhost", 3000, null)
            .forward(b)
            .take(1) // nedd that to free the buffer and the thread
            .map(Utils::decodeCics)
            .toBlocking()
            .singleOrDefault(null);

        Assert.assertEquals(commarea, response);
    }


    private TcpServer<ByteBuf, ByteBuf> server;

    @Before
    public void setUp() {
        this.server = TcpServer.newServer(3000)
            .start(serverConn -> serverConn.writeAndFlushOnEach(serverConn.getInput()));
    }

    @After
    public void tearDown() {
        server.shutdown();
        server.awaitShutdown();
    }

}