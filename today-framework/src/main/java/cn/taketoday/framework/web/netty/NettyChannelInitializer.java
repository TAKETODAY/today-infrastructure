/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.framework.web.netty;

import cn.taketoday.lang.Assert;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

/**
 * Framework Channel Initializer
 *
 * @author TODAY 2019-07-02 21:34
 */
public class NettyChannelInitializer
        extends ChannelInitializer<SocketChannel> implements ChannelHandler {

  private final ReactiveChannelHandler reactiveDispatcher;

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
   * @see HttpObjectDecoder#maxChunkSize
   */
  private int maxChunkSize = 8192;

  /**
   * For {@link HttpObjectDecoder.HeaderParser#maxLength}
   *
   * HTTP header cannot larger than {@code maxHeaderSize} bytes.
   *
   * @see io.netty.handler.codec.TooLongFrameException
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  private int maxHeaderSize = 8192;

  /**
   * For {@link HttpObjectDecoder.LineParser#maxLength}
   *
   * An HTTP line cannot larger than {@code maxInitialLineLength} bytes.
   *
   * @see io.netty.handler.codec.TooLongFrameException
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  private int maxInitialLineLength = 4096;

  /**
   * For validate HTTP request headers
   *
   * @see HttpObjectDecoder#validateHeaders
   * @see io.netty.handler.codec.http.HttpHeaders
   * @see io.netty.handler.codec.DefaultHeaders.NameValidator
   */
  private boolean validateHeaders = true;

  public NettyChannelInitializer(ReactiveChannelHandler reactiveDispatcher) {
    Assert.notNull(reactiveDispatcher, "ReactiveDispatcher must not be null");
    this.reactiveDispatcher = reactiveDispatcher;
  }

  @Override
  protected void initChannel(final SocketChannel ch) {
    final HttpServerCodec serverCodec
            = new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders);

    ch.pipeline()
            .addLast("HttpServerCodec", serverCodec)
            .addLast("HttpObjectAggregator", new HttpObjectAggregator(maxContentLength, closeOnExpectationFailed))
            .addLast("HttpServerExpectContinueHandler", new HttpServerExpectContinueHandler())
            .addLast("ReactiveChannelHandler", reactiveDispatcher);
  }

  @Override
  public boolean isSharable() {
    return false;
  }

  //

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

  /**
   * The maximum length of the aggregated content.
   * If the length of the aggregated content exceeds this value,
   *
   * @see HttpObjectAggregator#maxContentLength
   */
  public int getMaxContentLength() {
    return maxContentLength;
  }

  /**
   * If a 100-continue response is detected but the content
   * length is too large then true means close the connection.
   * otherwise the connection will remain open and data will be
   * consumed and discarded until the next request is received.
   *
   * @see HttpObjectAggregator#closeOnExpectationFailed
   */
  public boolean isCloseOnExpectationFailed() {
    return closeOnExpectationFailed;
  }

  /**
   * Get Netty {@link cn.taketoday.web.handler} implementation
   * like {@link cn.taketoday.web.servlet.DispatcherServlet}
   */
  public ReactiveChannelHandler getReactiveDispatcher() {
    return reactiveDispatcher;
  }

  // HttpServerCodec

  /**
   * Set {@link HttpObjectDecoder#maxChunkSize}
   *
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  public void setMaxChunkSize(int maxChunkSize) {
    this.maxChunkSize = maxChunkSize;
  }

  /**
   * Set {@link HttpObjectDecoder.HeaderParser#maxLength}
   *
   * HTTP header cannot larger than {@code maxHeaderSize} bytes.
   *
   * @see io.netty.handler.codec.TooLongFrameException
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  public void setMaxHeaderSize(int maxHeaderSize) {
    this.maxHeaderSize = maxHeaderSize;
  }

  /**
   * Set validate HTTP request headers
   *
   * @see HttpObjectDecoder#validateHeaders
   * @see io.netty.handler.codec.http.HttpHeaders
   * @see io.netty.handler.codec.DefaultHeaders.NameValidator
   */
  public void setValidateHeaders(boolean validateHeaders) {
    this.validateHeaders = validateHeaders;
  }

  /**
   * Set {@link HttpObjectDecoder.LineParser#maxLength}
   *
   * An HTTP line cannot larger than {@code maxInitialLineLength} bytes.
   *
   * @see io.netty.handler.codec.TooLongFrameException
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  public void setMaxInitialLineLength(int maxInitialLineLength) {
    this.maxInitialLineLength = maxInitialLineLength;
  }

  /**
   * @see HttpObjectDecoder#validateHeaders
   * @see io.netty.handler.codec.http.HttpHeaders
   * @see io.netty.handler.codec.DefaultHeaders.NameValidator
   */
  public boolean isValidateHeaders() {
    return validateHeaders;
  }

  /**
   * @see HttpObjectDecoder#maxChunkSize
   */
  public int getMaxChunkSize() {
    return maxChunkSize;
  }

  /**
   * For {@link HttpObjectDecoder.HeaderParser#maxLength}
   *
   * HTTP header cannot larger than {@code maxHeaderSize} bytes.
   *
   * @see io.netty.handler.codec.TooLongFrameException
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  public int getMaxHeaderSize() {
    return maxHeaderSize;
  }

  /**
   * For {@link HttpObjectDecoder.LineParser#maxLength}
   *
   * An HTTP line cannot larger than {@code maxInitialLineLength} bytes.
   *
   * @see io.netty.handler.codec.TooLongFrameException
   * @see io.netty.handler.codec.http.HttpObjectDecoder
   */
  public int getMaxInitialLineLength() {
    return maxInitialLineLength;
  }
}
