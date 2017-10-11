
package fr.laposte.disfe.cicsproxy.proxy;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.ConnectionRequest;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import lombok.RequiredArgsConstructor;
import rx.Observable;

/**
 * 
 * @author sylvain
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public final class CommareaProxy {

	

	private static final Logger LOG = LoggerFactory.getLogger(CommareaProxy.class);
	
	@Value("${proxy.local.port}")
	private int LOCAL_PORT ; 
	@Value("${proxy.remote.host}")
    private String remoteHost;
	@Value("${proxy.remote.port}")
    private int remotePort;
	
	/** Modes de fonctionnement */
	private enum ProxyMode { IDLE, RECORD, REPLAY, RECORD_IF_EMPTY }
	
	/** Charset de travail */
	public static final Charset CHARSET = Charset.forName("UTF-8");
	public static final Charset CICS_CHARSET = Charset.forName("IBM01147");
	
	/** Bucket couchbase injecté par Spring Boot */
	private final Bucket bucket;
	
	@PostConstruct
	public void start() {

		LOG.info("CICS Proxying *:" + LOCAL_PORT + " ...");

        /* Starts a new HTTP server on an ephemeral port which acts as a proxy to the target server started above.*/
        TcpServer.newServer(LOCAL_PORT)
        	.enableWireLogging("proxy-server", LogLevel.DEBUG)
        	.start(serverConn -> {
        		
        		final Observable<ByteBuf> stream = serverConn.getInput().replayable();
        		
        		return stream.flatMap(buf -> { 
    				String question64 =  new String(new Base64().encode(ByteBufUtil.getBytes(buf)), CHARSET);
    				JsonDocument doc = this.bucket.get(question64);
    				if (doc == null) {
    					LOG.info(" => Appel du serveur distant ..");
    					Observable<ByteBuf> resp = TcpClient.newClient(remoteHost, remotePort)
    							.createConnectionRequest()
    							.flatMap(clientConn -> clientConn.writeAndFlushOnEach(stream)
    								.cast(ByteBuf.class) 
    								.mergeWith(clientConn.getInput()))
    								.map(r -> save(buf, r));
    					
        				return serverConn.writeAndFlushOnEach(resp);	
    				}
    				else {
    					LOG.info(" => Utilisation du cache ...");
    					byte[] reponse = new Base64().decode(doc.content().getString("response"));
    					ByteBuf reponseBB = Unpooled.copiedBuffer(reponse);
    					return serverConn.writeAndFlushOnEach(Observable.just(reponseBB));
    				}
    				
        		});
        	})
        .awaitShutdown();

	}
	
	public ByteBuf save(ByteBuf q, ByteBuf r) {
		try {
	     	LOG.info("Enregistrement de la réponse en cache ...");
	    	
	     	final String question = new String(new Base64().encode(ByteBufUtil.getBytes(q)), CHARSET);
	    	this.bucket.insert(JsonDocument.create(question, 
	    		JsonObject.create()
	    			.put("questionDecoded", new String(ByteBufUtil.getBytes(q), CICS_CHARSET))
	    			.put("question", question)
	    			.put("reponseDecoded", new String(ByteBufUtil.getBytes(r), CICS_CHARSET))
	    			.put("reponse", new String((new Base64()).encode(ByteBufUtil.getBytes(r)), CHARSET)))
	    	);
	    	LOG.info("Enregistrement réussi !");
	    	
	    	return r;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private static void safeDump(byte[] b) {
		LOG.info(new String(b, CICS_CHARSET));
	}
	
}