/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.framework.reactive.server;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.framework.reactive.ReactiveDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * @author TODAY <br>
 *         2019-07-02 21:34
 */
@Singleton
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> implements ChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(NettyServerInitializer.class);

    private final ReactiveDispatcher reactiveDispatcher;

    public NettyServerInitializer(ReactiveDispatcher reactiveDispatcher) {
        this.reactiveDispatcher = reactiveDispatcher;
    }

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        log.info("initChannel {}", ch);

        try {
            ch.pipeline()
                    .addLast("HttpServerCodec", new HttpServerCodec())
                    .addLast("HttpObjectAggregator", new HttpObjectAggregator(1024 * 1024 * 64))
//                    .addLast("HttpServerExpectContinueHandler", new HttpServerExpectContinueHandler())
                    .addLast("ReactiveDispatcher", reactiveDispatcher);
        }
        catch (Exception e) {
            log.error("Add channel pipeline error", e);
            throw e;
        }
    }

}
