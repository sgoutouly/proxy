
package fr.laposte.disfe.cicsproxy.proxy;

import java.nio.charset.Charset;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.logging.LogLevel;
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
	private enum ProxyMode { IDLE, RECORD, RECORD_IF_EMPTY, MOCK }
	
	public static final Charset CHARSET = Charset.forName("UTF-8");
	public static final Charset CICS_CHARSET = Charset.forName("IBM01147");
	
	/** Bucket couchbase injecté par Spring Boot */
	private final Bucket bucket;
	
	@PostConstruct
	public void start() {

		LOG.info("CICS Proxying *:" + LOCAL_PORT + " ...");

        TcpServer.newServer(LOCAL_PORT)
        	.enableWireLogging("proxy-server", LogLevel.DEBUG)
        	.start(serverConn -> {
        		return serverConn.getInput()
        			.replayable()
        			.flatMap(question -> { 
	    				return this.bucket.async()
	    					.get(toBase64(question))
	    					.switchIfEmpty(forward(question)
	    						.map(reponse -> saveExchange(question, reponse))
	    						.flatMap(remoteResponse -> serverConn.writeAndFlushOnEach(Observable.just(remoteResponse)))
	    						.cast(JsonDocument.class)) // Ceci permet de faire passer ce bloc pour un JsonDocument
	    					.flatMap(doc -> {
	    						LOG.info(" => Utilisation du cache ...");
	        					final byte[] reponse = new Base64().decode(doc.content().getString("reponse"));
	        					return serverConn.writeAndFlushOnEach(Observable.just(Unpooled.copiedBuffer(reponse)));
	    					});
	    				});
        		
        	})
        	.awaitShutdown();
	}
	
	/**
	 * Appel le serveur distant
	 * @param serverConn
	 * @param q
	 * @return Observable<Void>
	 */
	Observable<ByteBuf> forward(ByteBuf q) {
		LOG.info(" => Appel du serveur distant ..");
		return TcpClient.newClient(this.remoteHost, this.remotePort)
			.createConnectionRequest()
			.flatMap(clientConn -> clientConn.writeAndFlushOnEach(Observable.just(q))
				.cast(ByteBuf.class) 
				.mergeWith(clientConn.getInput()));
	}
	
	/**
	 * @param q
	 * @param r
	 * @return
	 */
	public ByteBuf saveExchange(ByteBuf q, ByteBuf r) {
		try {
	     	LOG.info("Enregistrement de la réponse en cache ...");
	    	
	     	final String question = toBase64(q);
	    	this.bucket.upsert(JsonDocument.create(question, 
	    		JsonObject.create()
	    			.put("questionDecoded", decodeCics(q))
	    			.put("question", question)
	    			.put("reponseDecoded", decodeCics(r))
	    			.put("reponse", toBase64(r)))
	    	);
	    	LOG.info("Enregistrement réussi !");
	    	
	    	return r;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private String decodeCics(final ByteBuf buf) {
		return new String(ByteBufUtil.getBytes(buf), CICS_CHARSET);
	}
	
	private String toBase64(final ByteBuf buf) {
		return new String(new Base64().encode(ByteBufUtil.getBytes(buf)), CHARSET);
	}
	
	private static void safeDump(byte[] b) {
		LOG.info(new String(b, CICS_CHARSET));
	}
	
}