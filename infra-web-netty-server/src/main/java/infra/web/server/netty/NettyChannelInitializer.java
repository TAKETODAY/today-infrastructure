/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.server.netty;

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
