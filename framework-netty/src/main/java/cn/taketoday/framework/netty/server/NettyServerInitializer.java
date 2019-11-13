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
package cn.taketoday.framework.netty.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2019-07-02 21:34
 */
@Slf4j
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> implements ChannelHandler {

    private final HttpServerHandler httpServerHandler;

    public NettyServerInitializer() {
        this.httpServerHandler = new HttpServerHandler();
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        log.info("{}", ch);

        final ChannelPipeline pipeline = ch.pipeline();
        try {

            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpServerExpectContinueHandler());

            pipeline.addLast(new MergeRequestHandler());
            pipeline.addLast(httpServerHandler);
        }
        catch (Exception e) {
            log.error("Add channel pipeline error", e);
        }
    }

}
