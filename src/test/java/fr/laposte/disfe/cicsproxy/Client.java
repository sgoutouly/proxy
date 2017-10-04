package fr.laposte.disfe.cicsproxy;

import java.nio.charset.Charset;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Client {

	public static void main(String[] args) throws InterruptedException {
		new Client();
	}
	
	public Client() {
		
		final int port  = 8080;
		final String host = "localhost";
		final String message = "hello";
		
		NioEventLoopGroup workGroup = new NioEventLoopGroup(8);

	    try {
	    	Bootstrap bootstrap = new Bootstrap()
	        	.group(workGroup)
	        	.channel(NioSocketChannel.class)
	        	.handler(new ChannelInitializer<SocketChannel>() {
		            @Override
		            protected void initChannel(SocketChannel socketChannel) throws Exception {
		                socketChannel
		                .pipeline()
		                .addLast(new StringDecoder(Charset.forName("IBM01147")), 
		                	new StringEncoder(Charset.forName("IBM01147")), 
		                	new LineBasedFrameDecoder(1024), 
		                	new ChannelInboundHandlerAdapter(){
		                		@Override
			                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			                        System.out.println("Réception d'une réponse : " + msg.toString());
			                        ctx.close().await(); // nécessaire pour arrêter le client
			                    }
		                });
		            }
	        	});

	        System.out.println("Connexion vers : " + host + ":" + port + " ...");
	        ChannelFuture future = bootstrap.connect(host, port).sync();
	        System.out.println("... Connexion réussie !");
	        
	        System.out.println("Envoi d'un message : " + message);
	        future.channel().writeAndFlush(message);
	        future.channel().closeFuture().sync();
	        
	    }
	    catch (Exception e){
	        e.printStackTrace();
	    }
	    finally {
	    	System.out.println("Arrêt du client ...");
	        workGroup.shutdownGracefully();
	    }
		
	}

}
