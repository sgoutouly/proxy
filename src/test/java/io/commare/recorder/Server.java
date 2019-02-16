package io.commare.recorder;

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

public class Server {

	private static final Charset CICS_CHARSET = Charset.forName("IBM01147");
	private static final Logger LOG = LoggerFactory.getLogger(Server.class);
	
	public static void main(String[] args) {
		new Server();
	}
	
	Server() {
		int port = 3000;

		NioEventLoopGroup workGroup = new NioEventLoopGroup(8);
		NioEventLoopGroup bossGroup = new NioEventLoopGroup();

		try {
			final ServerBootstrap bootstrap = new ServerBootstrap()
				.group(bossGroup, workGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 100)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel socketChannel) throws Exception {
						socketChannel.pipeline().addLast(new StringEncoder(CICS_CHARSET), new StringDecoder(CICS_CHARSET),
							new LineBasedFrameDecoder(1024),	new ChannelInboundHandlerAdapter() {
								
								@Override
								public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
									LOG.info("Connexion acceptée !");
								}
								@Override
								public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
									LOG.info("Réception d'un message :" + msg.toString());
									LOG.info("Envoi d'une réponse...");
									ctx.channel().writeAndFlush("GATE   BJKDJKBDKJBDJKBC djnjkbdkjbdjkbdjbd from server ");
								}
								
							});
					}
				});

			final ChannelFuture future = bootstrap.bind(port).sync();

			LOG.info("Server start at port : " + port);
			future.channel().closeFuture().sync();
		} 
		catch (Exception e) {
			LOG.error("error", e);
		} 
		finally {
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}

}
