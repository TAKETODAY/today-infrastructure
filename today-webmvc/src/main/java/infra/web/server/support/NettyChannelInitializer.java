/*
 * Copyright 2017 - 2026 the original author or authors.
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * HTTP netty channel initializer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:34
 */
sealed class NettyChannelInitializer extends ChannelInitializer<Channel> implements ChannelHandler permits SSLNettyChannelInitializer {

  private final ChannelHandler channelHandler;

  private final @Nullable ChannelConfigurer channelConfigurer;

  /**
   * A configuration object for specifying the behavior
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  private final HttpDecoderConfig httpDecoderConfig;

  protected NettyChannelInitializer(ChannelHandler channelHandler,
          @Nullable ChannelConfigurer channelConfigurer, HttpDecoderConfig httpDecoderConfig) {
    this.channelHandler = channelHandler;
    this.channelConfigurer = channelConfigurer;
    this.httpDecoderConfig = httpDecoderConfig;
  }

  @Override
  protected final void initChannel(final Channel ch) {
    preInitChannel(channelConfigurer, ch);
    ch.pipeline()
            .addLast("HttpServerCodec", new HttpServerCodec(httpDecoderConfig))
            .addLast("NettyChannelHandler", channelHandler)
            .remove(this);

    postInitChannel(channelConfigurer, ch);
  }

  protected void preInitChannel(@Nullable ChannelConfigurer configurer, Channel ch) {
    if (configurer != null) {
      configurer.initChannel(ch);
    }
  }

  protected void postInitChannel(@Nullable ChannelConfigurer configurer, Channel ch) {
    if (configurer != null) {
      configurer.postInitChannel(ch);
    }
  }

  @Override
  public boolean isSharable() {
    return true;
  }

}
