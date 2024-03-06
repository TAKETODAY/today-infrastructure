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

import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeadersFactory;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.HttpDataFactory;

/**
 * To help build a {@link NettyRequestContext}
 *
 * <p>
 * User can use this class to customize {@link NettyRequestContext}
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/3/30 17:46
 */
public class NettyRequestConfig {

  @Nullable
  private Consumer<? super HttpHeaders> trailerHeadersConsumer;

  private ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;

  private ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;

  /**
   * response body initial size
   *
   * @see io.netty.buffer.ByteBufAllocator#ioBuffer(int)
   */
  private int bodyInitialSize = 512;

  @Nullable
  private Function<RequestContext, ByteBuf> responseBodyFactory;

  private Charset postRequestDecoderCharset = Constant.DEFAULT_CHARSET;

  private HttpDataFactory httpDataFactory;

  private HttpHeadersFactory httpHeadersFactory = DefaultHttpHeadersFactory.headersFactory();

  public final SendErrorHandler sendErrorHandler;

  public final boolean secure;

  public NettyRequestConfig(HttpDataFactory httpDataFactory, SendErrorHandler sendErrorHandler, boolean secure) {
    Assert.notNull(sendErrorHandler, "SendErrorHandler is required");
    setHttpDataFactory(httpDataFactory);
    this.secure = secure;
    this.sendErrorHandler = sendErrorHandler;
  }

  public void setTrailerHeadersConsumer(@Nullable Consumer<? super HttpHeaders> consumer) {
    this.trailerHeadersConsumer = consumer;
  }

  @Nullable
  public Consumer<? super HttpHeaders> getTrailerHeadersConsumer() {
    return trailerHeadersConsumer;
  }

  public void setCookieDecoder(@Nullable ServerCookieDecoder cookieDecoder) {
    this.cookieDecoder = cookieDecoder == null ? ServerCookieDecoder.STRICT : cookieDecoder;
  }

  public void setCookieEncoder(@Nullable ServerCookieEncoder cookieEncoder) {
    this.cookieEncoder = cookieEncoder == null ? ServerCookieEncoder.STRICT : cookieEncoder;
  }

  public ServerCookieDecoder getCookieDecoder() {
    return cookieDecoder;
  }

  public ServerCookieEncoder getCookieEncoder() {
    return cookieEncoder;
  }

  public void setResponseBodyFactory(@Nullable Function<RequestContext, ByteBuf> factory) {
    this.responseBodyFactory = factory;
  }

  @Nullable
  public Function<RequestContext, ByteBuf> getResponseBodyFactory() {
    return responseBodyFactory;
  }

  /**
   * @return response body initial capacity
   * @see io.netty.buffer.ByteBufAllocator#buffer(int)
   */
  public int getBodyInitialSize() {
    return bodyInitialSize;
  }

  /**
   * @param bodyInitialSize response body initial capacity
   * @see io.netty.buffer.ByteBufAllocator#buffer(int)
   */
  public void setBodyInitialSize(int bodyInitialSize) {
    this.bodyInitialSize = bodyInitialSize;
  }

  public void setPostRequestDecoderCharset(@Nullable Charset charset) {
    this.postRequestDecoderCharset =
            charset == null ? Constant.DEFAULT_CHARSET : charset;
  }

  public Charset getPostRequestDecoderCharset() {
    return postRequestDecoderCharset;
  }

  public HttpDataFactory getHttpDataFactory() {
    return httpDataFactory;
  }

  public void setHttpDataFactory(HttpDataFactory httpDataFactory) {
    Assert.notNull(httpDataFactory, "HttpDataFactory is required");
    this.httpDataFactory = httpDataFactory;
  }

  public void setHttpHeadersFactory(@Nullable HttpHeadersFactory headersFactory) {
    this.httpHeadersFactory = headersFactory == null ? DefaultHttpHeadersFactory.headersFactory()
            : headersFactory;
  }

  public HttpHeadersFactory getHttpHeadersFactory() {
    return httpHeadersFactory;
  }

}
