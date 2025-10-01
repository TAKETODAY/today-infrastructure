/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.support;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

/**
 * HTTP netty channel initializer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:34
 */
sealed class NettyChannelInitializer extends ChannelInitializer<Channel> implements ChannelHandler permits SSLNettyChannelInitializer {

  private final ChannelHandlerFactory channelHandlerFactory;

  @Nullable
  private final ChannelConfigurer channelConfigurer;

  /**
   * A configuration object for specifying the behaviour
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  private final HttpDecoderConfig httpDecoderConfig;

  protected NettyChannelInitializer(ChannelHandlerFactory channelHandlerFactory,
          @Nullable ChannelConfigurer channelConfigurer, HttpDecoderConfig httpDecoderConfig) {
    this.channelHandlerFactory = channelHandlerFactory;
    this.channelConfigurer = channelConfigurer;
    this.httpDecoderConfig = httpDecoderConfig;
  }

  @Override
  protected final void initChannel(final Channel ch) {
    preInitChannel(channelConfigurer, ch);
    ch.pipeline()
            .addLast("HttpServerCodec", new HttpServerCodec(httpDecoderConfig))
            .addLast("HttpServerExpectContinueHandler", new HttpServerExpectContinueHandler())
            .addLast("NettyChannelHandler", channelHandlerFactory.createChannelHandler(ch))
            .remove(this);
  }

  protected void preInitChannel(@Nullable ChannelConfigurer configurer, Channel ch) {
    if (configurer != null) {
      configurer.initChannel(ch);
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }

}
