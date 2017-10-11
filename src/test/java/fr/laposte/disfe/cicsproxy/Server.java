package fr.laposte.disfe.cicsproxy;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Hello world!
 *
 */
public class Server {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	public static void main(String[] args) {
		new Server();
	}
	
	public Server() {
		int port = 3000;

		NioEventLoopGroup workGroup = new NioEventLoopGroup(8);
		NioEventLoopGroup bossGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap()
				.group(bossGroup, workGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 100)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel socketChannel) throws Exception {
						socketChannel.pipeline().addLast(
							new StringEncoder(Charset.forName("IBM01147")), 
							new StringDecoder(Charset.forName("IBM01147")), 
							new LineBasedFrameDecoder(1024),
							new ChannelInboundHandlerAdapter() {
								
								@Override
								public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
									log.info("Connexion acceptée !");
								}
								@Override
								public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
									log.info("Réception d'un message :" + msg.toString());
									log.info("Envoi d'une réponse...");
									ctx.channel().writeAndFlush("GATE   BJKDJKBDKJBDJKBC djnjkbdkjbdjkbdjbd from server ");
								}
								
							});
					}
				});
			ChannelFuture future = bootstrap.bind(port).sync();
			log.info("Server start at port : " + port);
			future.channel().closeFuture().sync();
		} 
		catch (Exception e) {
			log.info("error");
		} 
		finally {
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}

}
