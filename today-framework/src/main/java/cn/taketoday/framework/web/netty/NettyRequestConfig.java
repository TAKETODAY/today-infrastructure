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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
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

  /**
   * Should Netty validate HTTP response Header values to ensure they aren't malicious.
   */
  private boolean validateHeaders = false;
  private boolean singleFieldHeaders = true;

  private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

  @Nullable
  private Consumer<? super HttpHeaders> trailerHeadersConsumer;

  private ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;
  private ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;

  /**
   * response body initial size
   *
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  private int bodyInitialSize = 512;

  @Nullable
  private Function<RequestContext, ByteBuf> responseBodyFactory;

  /**
   * {@code contextPath} just like {@code HttpServletRequest.getContextPath()}
   *
   * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
   */
  private String contextPath = "";

  private Charset postRequestDecoderCharset = Constant.DEFAULT_CHARSET;

  private HttpDataFactory httpDataFactory;

  private final SendErrorHandler sendErrorHandler;

  public NettyRequestConfig(HttpDataFactory httpDataFactory, SendErrorHandler sendErrorHandler) {
    Assert.notNull(sendErrorHandler, "SendErrorHandler is required");
    setHttpDataFactory(httpDataFactory);
    this.sendErrorHandler = sendErrorHandler;
  }

  /**
   * Should Netty validate Header values to ensure they aren't malicious.
   */
  public void setValidateHeaders(boolean validateHeaders) {
    this.validateHeaders = validateHeaders;
  }

  public void setSingleFieldHeaders(boolean singleFieldHeaders) {
    this.singleFieldHeaders = singleFieldHeaders;
  }

  public boolean isSingleFieldHeaders() {
    return singleFieldHeaders;
  }

  public boolean isValidateHeaders() {
    return validateHeaders;
  }

  public void setTrailerHeadersConsumer(@Nullable Consumer<? super HttpHeaders> consumer) {
    this.trailerHeadersConsumer = consumer;
  }

  @Nullable
  public Consumer<? super HttpHeaders> getTrailerHeadersConsumer() {
    return trailerHeadersConsumer;
  }

  public void setHttpVersion(HttpVersion httpVersion) {
    Assert.notNull(httpVersion, "HttpVersion is required");
    this.httpVersion = httpVersion;
  }

  public HttpVersion getHttpVersion() {
    return httpVersion;
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
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  public int getBodyInitialSize() {
    return bodyInitialSize;
  }

  /**
   * @param bodyInitialSize response body initial capacity
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  public void setBodyInitialSize(int bodyInitialSize) {
    this.bodyInitialSize = bodyInitialSize;
  }

  public void setContextPath(@Nullable String contextPath) {
    this.contextPath = contextPath == null ? "" : contextPath;
  }

  /**
   * {@code contextPath} just like {@code HttpServletRequest.getContextPath()}
   *
   * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
   */
  public String getContextPath() {
    return contextPath;
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

  public SendErrorHandler getSendErrorHandler() {
    return sendErrorHandler;
  }

}
