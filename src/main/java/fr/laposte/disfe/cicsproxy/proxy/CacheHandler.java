package fr.laposte.disfe.cicsproxy.proxy;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;

/**
 * CommareaProxyCacheHandler
 * @author sylvain
 */
@Sharable
//@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheHandler extends ChannelDuplexHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(CacheHandler.class);
	/** Modes de foncitonnement */
	enum ProxyMode { IDLE, RECORD, REPLAY, RECORD_IF_EMPTY }
	/** Charset de travail */
	public static final Charset CHARSET = Charset.forName("UTF-8");
	/** Clé de stockage de la question dans le channel */
	final static AttributeKey<String> QUESTION = AttributeKey.valueOf("QUESTION"); 
	/** Liste des arguments passsés à l'application au démarrage */
	private final ApplicationArguments args;
	/** Bucket couchbase injecté par Spring Boot */
	private final Bucket bucket;
	
	
    /**
     * Lecture de la question
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    	
    	LOG.info(ProxyMode.valueOf(args.getNonOptionArgs().get(0)).toString());
    	
    	byte[] bytes = ByteBufUtil.getBytes((ByteBuf) msg);
    	LOG.info("Question reçue : " + new String(bytes, "IBM01147"));
    	
    	if (ProxyMode.valueOf(args.getNonOptionArgs().get(0)) == ProxyMode.IDLE) {
    		ctx.fireChannelRead(msg); // Appel du handler suivant		
    	}
    	else {
    		final String question = new String((new Base64()).encode(bytes), CHARSET);
	    	ctx.channel().attr(QUESTION).set(question);
	    	
	    	LOG.info("Recherche en cache de la clé : " + question + " ...");
	    	
	    	final JsonDocument doc = this.bucket.get(question);
	    	if (doc == null || ProxyMode.valueOf(args.getNonOptionArgs().get(0)) == ProxyMode.RECORD) {
	    		LOG.info("Appel de la CTG ...");
	        	ctx.fireChannelRead(msg); // Appel du handler suivant
	    	}
	    	else {
	    		LOG.info("Enregistrement disponible, utilisation du cache ...");
	    		bytes = new Base64().decode(doc.content().getString("response"));
	        	LOG.info("Contenu du cache : " + new String(bytes, "IBM01147"));
	        	
	        	LOG.info("Envoi de la réponse");
	    		ctx.writeAndFlush(Unpooled.copiedBuffer(bytes)); // Réponse vers le client
		    }
    	}
    }

    /**
     * Eriture de la réponse
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    	final byte[] bytes = ByteBufUtil.getBytes((ByteBuf) msg);
    	LOG.info("CtgHandler WRITE EBCDIC : " + new String(bytes, "IBM01147"));

     	LOG.info("Enregistrement de la réponse en cache ...");
    	final String question = ctx.channel().attr(QUESTION).get();
    	final String reponse = new String((new Base64()).encode(bytes), CHARSET);
    	this.bucket.insert(JsonDocument.create(question, 
    		JsonObject.create()
    			.put("question", question)
    			.put("response", reponse))
    	);
    	LOG.info("Enregistrement réussi !");
    	
    	LOG.info("Envoi de la réponse");
    	ctx.write(msg, promise);
    	
    }

}
