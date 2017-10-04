/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package fr.laposte.disfe.cicsproxy.proxy;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * 
 * @author sylvain
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public final class CommareaProxy {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Value("${proxy.local.port}")
	private int LOCAL_PORT ; 
	
	/** Injecté par Spring */
	private final Initializer commareaProxyInitializer;

	
	@PostConstruct
	public void start() throws InterruptedException {

		log.info("CICS Proxying *:" + LOCAL_PORT + " ...");

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				// .handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(this.commareaProxyInitializer)
				.childOption(ChannelOption.AUTO_READ, false)
				.bind(LOCAL_PORT)
					.sync()
					.channel()
						.closeFuture()
						.sync();
		} 
		finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}

}