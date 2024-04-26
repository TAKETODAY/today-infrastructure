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
import cn.taketoday.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeadersFactory;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

/**
 * Netty HTTP request config
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/3/30 17:46
 */
public class NettyRequestConfig {

  @Nullable
  public final Consumer<? super HttpHeaders> trailerHeadersConsumer;

  public final ServerCookieEncoder cookieEncoder;

  public final ServerCookieDecoder cookieDecoder;

  /**
   * response body initial size
   *
   * @see io.netty.buffer.ByteBufAllocator#ioBuffer(int)
   */
  public final int responseBodyInitialCapacity;

  @Nullable
  public final Function<RequestContext, ByteBuf> responseBodyFactory;

  /**
   * @see InterfaceHttpPostRequestDecoder
   */
  public final Charset postRequestDecoderCharset;

  public final HttpHeadersFactory httpHeadersFactory;

  public final HttpDataFactory httpDataFactory;

  public final SendErrorHandler sendErrorHandler;

  public final boolean secure;

  private NettyRequestConfig(Builder builder) {
    Assert.notNull(builder.sendErrorHandler, "SendErrorHandler is required");
    Assert.notNull(builder.httpDataFactory, "HttpDataFactory is required");
    Assert.isTrue(builder.responseBodyInitialCapacity > 0, "responseBodyInitialCapacity is required");

    this.secure = builder.secure;
    this.cookieEncoder = builder.cookieEncoder;
    this.cookieDecoder = builder.cookieDecoder;
    this.httpDataFactory = builder.httpDataFactory;
    this.sendErrorHandler = builder.sendErrorHandler;
    this.httpHeadersFactory = builder.httpHeadersFactory;
    this.responseBodyFactory = builder.responseBodyFactory;
    this.trailerHeadersConsumer = builder.trailerHeadersConsumer;
    this.responseBodyInitialCapacity = builder.responseBodyInitialCapacity;
    this.postRequestDecoderCharset = builder.postRequestDecoderCharset == null
            ? Constant.DEFAULT_CHARSET : builder.postRequestDecoderCharset;
  }

  public static Builder forBuilder() {
    return new Builder();
  }

  public static class Builder {

    @Nullable
    private Consumer<? super HttpHeaders> trailerHeadersConsumer;

    private ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;

    private ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;

    /**
     * response body initial size
     *
     * @see io.netty.buffer.ByteBufAllocator#ioBuffer(int)
     */
    private int responseBodyInitialCapacity = 128;

    @Nullable
    private Function<RequestContext, ByteBuf> responseBodyFactory;

    @Nullable
    private Charset postRequestDecoderCharset = Constant.DEFAULT_CHARSET;

    private HttpHeadersFactory httpHeadersFactory = DefaultHttpHeadersFactory.headersFactory();

    private HttpDataFactory httpDataFactory;

    private SendErrorHandler sendErrorHandler;

    private boolean secure;

    public Builder secure(boolean secure) {
      this.secure = secure;
      return this;
    }

    public Builder sendErrorHandler(SendErrorHandler sendErrorHandler) {
      this.sendErrorHandler = sendErrorHandler;
      return this;
    }

    /**
     * Interface to enable creation of InterfaceHttpData objects
     */
    public Builder httpDataFactory(HttpDataFactory httpDataFactory) {
      this.httpDataFactory = httpDataFactory;
      return this;
    }

    public Builder trailerHeadersConsumer(@Nullable Consumer<? super HttpHeaders> consumer) {
      this.trailerHeadersConsumer = consumer;
      return this;
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie decoder to be used server side.
     *
     * Only name and value fields are expected, so old fields are not populated (path, domain, etc).
     *
     * Old <a href="https://tools.ietf.org/html/rfc2965">RFC2965</a> cookies are still supported,
     * old fields will simply be ignored.
     *
     * @see ServerCookieEncoder
     */
    public Builder cookieDecoder(@Nullable ServerCookieDecoder cookieDecoder) {
      this.cookieDecoder = cookieDecoder == null ? ServerCookieDecoder.STRICT : cookieDecoder;
      return this;
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie encoder to be used server side,
     * so some fields are sent (Version is typically ignored).
     *
     * As Netty's Cookie merges Expires and MaxAge into one single field, only Max-Age field is sent.
     *
     * Note that multiple cookies must be sent as separate "Set-Cookie" headers.
     *
     * @see ServerCookieDecoder
     */
    public Builder cookieEncoder(@Nullable ServerCookieEncoder cookieEncoder) {
      this.cookieEncoder = cookieEncoder == null ? ServerCookieEncoder.STRICT : cookieEncoder;
      return this;
    }

    public Builder responseBodyFactory(@Nullable Function<RequestContext, ByteBuf> factory) {
      this.responseBodyFactory = factory;
      return this;
    }

    /**
     * @param responseBodyInitialCapacity response body initial capacity
     * @see io.netty.buffer.ByteBufAllocator#buffer(int)
     */
    public Builder responseBodyInitialCapacity(int responseBodyInitialCapacity) {
      this.responseBodyInitialCapacity = responseBodyInitialCapacity;
      return this;
    }

    public Builder postRequestDecoderCharset(@Nullable Charset charset) {
      this.postRequestDecoderCharset = charset == null ? Constant.DEFAULT_CHARSET : charset;
      return this;
    }

    /**
     * A builder of {@link HttpHeadersFactory} instances, that itself implements {@link HttpHeadersFactory}.
     * The builder is immutable, and every {@code with-} method produce a new, modified instance.
     * <p>
     * The default builder you most likely want to start with is {@link DefaultHttpHeadersFactory#headersFactory()}.
     */
    public Builder headersFactory(@Nullable HttpHeadersFactory headersFactory) {
      this.httpHeadersFactory = headersFactory == null ? DefaultHttpHeadersFactory.headersFactory()
              : headersFactory;
      return this;
    }

    public NettyRequestConfig build() {
      return new NettyRequestConfig(this);
    }

  }

}
