
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
	
	/** Modes de foncitonnement */
	private enum ProxyMode { IDLE, RECORD, REPLAY, RECORD_IF_EMPTY }
	/** Charset de travail */
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	/** Liste des arguments passsés à l'application au démarrage */
	private final ApplicationArguments args;
	/** Bucket couchbase injecté par Spring Boot */
	private final Bucket bucket;
	
	@PostConstruct
	public void start() {

		LOG.info("CICS Proxying *:" + LOCAL_PORT + " ...");

        /* Start a TCP client pointing to remote server. */
        final TcpClient<ByteBuf, ByteBuf> targetClient = TcpClient.newClient(remoteHost, remotePort);
        
        /* Create a new connection request, each subscription to which creates a new connection.*/
        ConnectionRequest<ByteBuf, ByteBuf> connReq = targetClient.createConnectionRequest();

        /* Starts a new HTTP server on an ephemeral port which acts as a proxy to the target server started above.*/
        TcpServer.newServer(LOCAL_PORT)
        	.enableWireLogging("proxy-server", LogLevel.DEBUG)
        	.start(serverConn -> {
        		
        		final Observable<ByteBuf> stream = serverConn.getInput().replayable();
        		
        		return stream.flatMap(buf -> { 
    				byte[] bytes = ByteBufUtil.getBytes(buf);
    				String question64 =  new String(new Base64().encode(bytes), CHARSET);
    				JsonDocument doc = this.bucket.get(question64);
    				if (doc == null) {
    					LOG.info(" => Appel du serveur distant ..");
    					Observable<ByteBuf> resp = connReq.flatMap(clientConn -> clientConn
    							.writeAndFlushOnEach(stream)
    						  	.cast(ByteBuf.class) 
    						  	.mergeWith(clientConn.getInput()))
    							.map(r -> save(buf, r));
    					
        				return serverConn.writeAndFlushOnEach(resp);	
    				}
    				else {
    					LOG.info(" => Utilisation du cache ...");
    					String reponse64 = doc.content().getString("response");
    					byte[] reponseB = new Base64().decode(reponse64);
    					ByteBuf reponseBB = Unpooled.copiedBuffer(reponseB);
    					return serverConn.writeAndFlushOnEach(Observable.just(reponseBB));
    				}
    				
        		});
        	})
        .awaitShutdown();

	}
	
	public ByteBuf save(ByteBuf q, ByteBuf r) {
		try {
	     	LOG.info("Enregistrement de la réponse en cache ...");
	    	final String question = new String(new Base64().encode(ByteBufUtil.getBytes(q)), "UTF-8");
	    	final String reponse = new String((new Base64()).encode(ByteBufUtil.getBytes(r)), "UTF-8");
	    	this.bucket.insert(JsonDocument.create(question, 
	    		JsonObject.create()
	    			.put("question", question)
	    			.put("response", reponse))
	    	);
	    	LOG.info("Enregistrement réussi !");
	    	
	    	return r;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private static void safeDump(byte[] b) {
		try {
			LOG.info(new String(b, "IBM01147"));
		} 
		catch (UnsupportedEncodingException e) {}
	}
	
}