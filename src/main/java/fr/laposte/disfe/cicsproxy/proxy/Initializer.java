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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * 
 * @author sylvain
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Initializer extends ChannelInitializer<SocketChannel> {
	
	/** Injecté par Spring */
    private final CacheHandler commareaProxyCacheHandler;
    /** Injecté par Spring */
    private final FrontendHandler commareaProxyFrontendHandler;

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
        		commareaProxyCacheHandler,
        		commareaProxyFrontendHandler);
    }
}