/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.framework.web.netty;

import cn.taketoday.lang.Assert;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

/**
 * HTTP netty channel initializer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:34
 */
public class NettyChannelInitializer extends ChannelInitializer<Channel> implements ChannelHandler {

  private final NettyChannelHandler nettyChannelHandler;

  /**
   * the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   *
   * @see HttpObjectAggregator#maxContentLength
   */
  private int maxContentLength = 1024 * 1024 * 64;

  /**
   * If a 100-continue response is detected but the content
   * length is too large then true means close the connection.
   * otherwise the connection will remain open and data will be
   * consumed and discarded until the next request is received.
   *
   * @see HttpObjectAggregator#closeOnExpectationFailed
   */
  private boolean closeOnExpectationFailed = false;

  /**
   * A configuration object for specifying the behaviour
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  private HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
          .setMaxInitialLineLength(4096)
          .setMaxHeaderSize(8192)
          .setMaxChunkSize(8192)
          .setValidateHeaders(true);

  public NettyChannelInitializer(NettyChannelHandler channelHandler) {
    Assert.notNull(channelHandler, "NettyChannelHandler is required");
    this.nettyChannelHandler = channelHandler;
  }

  @Override
  protected void initChannel(final Channel ch) {

    ch.pipeline()
            .addLast("HttpServerCodec", new HttpServerCodec(httpDecoderConfig))
            .addLast("HttpObjectAggregator", new HttpObjectAggregator(maxContentLength, closeOnExpectationFailed))
            .addLast("HttpServerExpectContinueHandler", new HttpServerExpectContinueHandler())
            .addLast("NettyChannelHandler", nettyChannelHandler);
  }

  @Override
  public boolean isSharable() {
    return true;
  }

  //

  /**
   * Set a configuration object for specifying the behaviour
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  public void setHttpDecoderConfig(HttpDecoderConfig httpDecoderConfig) {
    Assert.notNull(httpDecoderConfig, "HttpDecoderConfig is required");
    this.httpDecoderConfig = httpDecoderConfig;
  }

  /**
   * Set the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   *
   * @param maxContentLength the maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   * @see HttpObjectAggregator#maxContentLength
   */
  public void setMaxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
  }

  /**
   * Set If a 100-continue response is detected but the content
   * length is too large then true means close the connection.
   * otherwise the connection will remain open and data will be
   * consumed and discarded until the next request is received.
   *
   * @see HttpObjectAggregator#closeOnExpectationFailed
   */
  public void setCloseOnExpectationFailed(boolean closeOnExpectationFailed) {
    this.closeOnExpectationFailed = closeOnExpectationFailed;
  }

}
