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

import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.http.HttpRequest;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.DataSize;
import infra.util.concurrent.Awaiter;
import infra.util.concurrent.SimpleSingleThreadAwaiter;
import infra.web.RequestContext;
import infra.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeadersFactory;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

/**
 * Configuration class for Netty-based HTTP request handling. This class provides
 * a comprehensive set of options to customize the behavior of HTTP requests and
 * responses, including cookie encoding/decoding, response body initialization,
 * character set handling, and more.
 *
 * <p>Instances of this class are immutable and are created using the {@link Builder}.
 * The builder allows flexible configuration of various properties, ensuring that
 * all required fields are properly set before constructing the final configuration.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * NettyRequestConfig config = NettyRequestConfig.forBuilder(true)
 *     .cookieEncoder(ServerCookieEncoder.LAX)
 *     .cookieDecoder(ServerCookieDecoder.LAX)
 *     .responseBodyInitialCapacity(256)
 *     .postRequestDecoderCharset(StandardCharsets.UTF_8)
 *     .writerCharset(StandardCharsets.UTF_8)
 *     .httpDataFactory(new DefaultHttpDataFactory(false))
 *     .sendErrorHandler(error -> {
 *       // Handle send error logic
 *     })
 *     .build();
 * }</pre>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Customizable cookie encoding and decoding strategies.</li>
 *   <li>Support for specifying initial capacity of response bodies.</li>
 *   <li>Flexible handling of character sets for POST request decoding and writers.</li>
 *   <li>Integration with Netty's {@link HttpHeadersFactory} and {@link HttpDataFactory}.</li>
 *   <li>Error handling for send operations via {@link SendErrorHandler}.</li>
 *   <li>Optional support for SSL/TLS through the {@code secure} flag.</li>
 * </ul>
 *
 * <p>This class is designed to be thread-safe and immutable once constructed, making
 * it suitable for use in concurrent environments.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see io.netty.buffer.ByteBufAllocator
 * @see ServerCookieEncoder
 * @see ServerCookieDecoder
 * @see HttpHeadersFactory
 * @see HttpDataFactory
 * @see SendErrorHandler
 * @since 2021/3/30 17:46
 */
public final class NettyRequestConfig {

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

  /**
   * @since 5.0
   */
  public final Charset writerCharset;

  /**
   * Response headers factory
   */
  public final HttpHeadersFactory httpHeadersFactory;

  public final HttpDataFactory httpDataFactory;

  public final SendErrorHandler sendErrorHandler;

  /**
   * SSL enabled status
   */
  public final boolean secure;

  /**
   * The maximum length of the http content.
   *
   * @since 5.0
   */
  public final long maxContentLength;

  /**
   * @since 5.0
   */
  public final Function<HttpRequest, Awaiter> awaiterFactory;

  private NettyRequestConfig(Builder builder) {
    Assert.notNull(builder.sendErrorHandler, "SendErrorHandler is required");
    Assert.notNull(builder.httpDataFactory, "HttpDataFactory is required");
    Assert.isTrue(builder.responseBodyInitialCapacity > 0, "responseBodyInitialCapacity is required");

    this.secure = builder.secure;
    this.cookieEncoder = builder.cookieEncoder;
    this.cookieDecoder = builder.cookieDecoder;
    this.awaiterFactory = builder.awaiterFactory;
    this.httpDataFactory = builder.httpDataFactory;
    this.maxContentLength = builder.maxContentLength;
    this.sendErrorHandler = builder.sendErrorHandler;
    this.httpHeadersFactory = builder.httpHeadersFactory;
    this.responseBodyFactory = builder.responseBodyFactory;
    this.trailerHeadersConsumer = builder.trailerHeadersConsumer;
    this.responseBodyInitialCapacity = builder.responseBodyInitialCapacity;
    this.postRequestDecoderCharset = builder.postRequestDecoderCharset == null
            ? Constant.DEFAULT_CHARSET : builder.postRequestDecoderCharset;
    this.writerCharset = builder.writerCharset == null ? Constant.DEFAULT_CHARSET : builder.writerCharset;
  }

  /**
   * Creates a new {@link Builder} instance for constructing a {@link NettyRequestConfig}.
   * The {@code secure} parameter determines whether the configuration will be set up
   * for secure communication.
   *
   * <p>Example usage:
   * <pre>{@code
   *   NettyRequestConfig.Builder builder = NettyRequestConfig.forBuilder(true);
   *   NettyRequestConfig config = builder
   *       .cookieEncoder(ServerCookieEncoder.LAX)
   *       .responseBodyInitialCapacity(256)
   *       .build();
   * }</pre>
   *
   * @param secure a boolean flag indicating whether the configuration should be
   * initialized for secure communication. If {@code true}, the
   * configuration will be set up for secure connections; otherwise,
   * it will be configured for non-secure connections.
   * @return a new {@link Builder} instance initialized with the specified
   * secure flag, ready for further configuration.
   */
  public static Builder forBuilder(boolean secure) {
    return new Builder(secure);
  }

  @SuppressWarnings("NullAway.Init")
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

    @Nullable
    private Charset writerCharset = Constant.DEFAULT_CHARSET;

    private long maxContentLength = DataSize.BYTES_PER_GB;

    private HttpHeadersFactory httpHeadersFactory = DefaultHttpHeadersFactory.headersFactory();

    private HttpDataFactory httpDataFactory;

    private SendErrorHandler sendErrorHandler;

    private final boolean secure;

    private Function<HttpRequest, Awaiter> awaiterFactory = ctx -> new SimpleSingleThreadAwaiter();

    Builder(boolean secure) {
      this.secure = secure;
    }

    /**
     * Sets the error handler for sending HTTP error responses.
     * <p>
     * This method allows you to define a custom error handling mechanism
     * by providing an implementation of the {@link SendErrorHandler} interface.
     * The handler will be invoked when an error occurs during the processing
     * of HTTP requests.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.sendErrorHandler((request, message) -> {
     *     // Custom error handling logic
     *     System.err.println("Error occurred: " + message);
     *
     *   });
     * }</pre>
     *
     * @param sendErrorHandler the custom error handler to be used for sending
     * error responses. If null, no custom error handling
     * will be applied.
     * @return the current {@link Builder} instance, enabling method chaining
     * @see SendErrorHandler
     */
    public Builder sendErrorHandler(SendErrorHandler sendErrorHandler) {
      this.sendErrorHandler = sendErrorHandler;
      return this;
    }

    /**
     * Sets the {@link HttpDataFactory} to be used for creating HTTP data objects.
     * <p>
     * The {@code HttpDataFactory} is responsible for creating instances of HTTP data
     * such as request bodies, response bodies, or other data structures required
     * during HTTP processing. By providing a custom factory, you can control how
     * these objects are created and managed.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   HttpDataFactory customFactory = new DefaultHttpDataFactory();
     *   builder.httpDataFactory(customFactory);
     * }</pre>
     *
     * @param httpDataFactory the {@link HttpDataFactory} instance to be used for
     * creating HTTP data objects. If {@code null}, no custom
     * factory will be applied, and the default behavior will be used.
     * @return the current {@link Builder} instance, enabling method chaining
     */
    public Builder httpDataFactory(HttpDataFactory httpDataFactory) {
      this.httpDataFactory = httpDataFactory;
      return this;
    }

    /**
     * Sets a consumer to handle trailer headers in the HTTP response.
     * <p>
     * Trailer headers are additional headers sent after the main body of an HTTP response.
     * This method allows you to define a custom consumer that processes these headers
     * when they are received. The consumer can be set to {@code null} if no processing
     * is required.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.trailerHeadersConsumer(trailerHeaders -> {
     *     // Process trailer headers
     *     System.out.println("Trailer headers: " + trailerHeaders);
     *   });
     * }</pre>
     *
     * @param consumer the consumer to process trailer headers. If {@code null}, no
     * processing will be applied to the trailer headers.
     * @return the current {@link Builder} instance, enabling method chaining
     */
    public Builder trailerHeadersConsumer(@Nullable Consumer<? super HttpHeaders> consumer) {
      this.trailerHeadersConsumer = consumer;
      return this;
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc6265">RFC6265</a> compliant cookie decoder to be used server side.
     * <p>
     * Only name and value fields are expected, so old fields are not populated (path, domain, etc).
     * <p>
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
     * <p>
     * As Netty's Cookie merges Expires and MaxAge into one single field, only Max-Age field is sent.
     * <p>
     * Note that multiple cookies must be sent as separate "Set-Cookie" headers.
     *
     * @see ServerCookieDecoder
     */
    public Builder cookieEncoder(@Nullable ServerCookieEncoder cookieEncoder) {
      this.cookieEncoder = cookieEncoder == null ? ServerCookieEncoder.STRICT : cookieEncoder;
      return this;
    }

    /**
     * Sets a factory function to generate the response body for HTTP responses.
     * <p>
     * The provided factory function takes a {@link RequestContext} as input and returns
     * a {@link ByteBuf} representing the response body. This allows dynamic generation
     * of response bodies based on the request context.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.responseBodyFactory(requestContext -> {
     *     ByteBuf buffer = Unpooled.buffer();
     *     buffer.writeBytes("Custom Response Body".getBytes(StandardCharsets.UTF_8));
     *     return buffer;
     *   });
     * }</pre>
     *
     * @param factory the factory function to generate the response body. If {@code null},
     * no custom response body generation will be applied.
     * @return the current {@link Builder} instance, enabling method chaining
     */
    public Builder responseBodyFactory(@Nullable Function<RequestContext, ByteBuf> factory) {
      this.responseBodyFactory = factory;
      return this;
    }

    /**
     * Sets the initial capacity for the response body buffer.
     * <p>
     * This method allows you to configure the initial size of the buffer used
     * to store the response body. Setting an appropriate initial capacity can
     * help optimize memory usage and performance, especially when the expected
     * size of the response body is known in advance.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.responseBodyInitialCapacity(1024); // Set initial capacity to 1KB
     * }</pre>
     *
     * @param responseBodyInitialCapacity the initial capacity (in bytes) for the
     * response body buffer. Must be a positive integer.
     * @return the current {@link Builder} instance, enabling method chaining
     */
    public Builder responseBodyInitialCapacity(int responseBodyInitialCapacity) {
      this.responseBodyInitialCapacity = responseBodyInitialCapacity;
      return this;
    }

    /**
     * Sets the character set to be used for decoding POST request data.
     * If the provided charset is {@code null}, a default charset defined by
     * {@link Constant#DEFAULT_CHARSET} will be used instead.
     * <p>
     * This method allows customization of the character encoding for handling
     * POST request payloads, ensuring proper interpretation of incoming data.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.postRequestDecoderCharset(StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param charset the character set to be used for decoding POST request data,
     * or {@code null} to use the default charset
     * @return the current {@link Builder} instance, enabling method chaining
     */
    public Builder postRequestDecoderCharset(@Nullable Charset charset) {
      this.postRequestDecoderCharset = charset == null ? Constant.DEFAULT_CHARSET : charset;
      return this;
    }

    /**
     * Sets the character set to be used for writing HTTP response data.
     * If the provided charset is {@code null}, a default charset defined by
     * {@link Constant#DEFAULT_CHARSET} will be used instead.
     * <p>
     * This method allows customization of the character encoding for handling
     * HTTP response data, ensuring proper encoding of outgoing data.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.writerCharset(StandardCharsets.UTF_8);
     *
     *   // If no specific charset is required, pass null to use the default:
     *   builder.writerCharset(null);
     * }</pre>
     *
     * @param charset the character set to be used for writing HTTP response data,
     * or {@code null} to use the default charset
     * @return the current {@link Builder} instance, enabling method chaining
     */
    public Builder writerCharset(@Nullable Charset charset) {
      this.writerCharset = charset == null ? Constant.DEFAULT_CHARSET : charset;
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

    /**
     * Set the maximum length of the http content.
     *
     * @param maxContentLength the maximum length of the http content.
     * If the length of the http content exceeds this value,
     * @since 5.0
     */
    public Builder maxContentLength(long maxContentLength) {
      this.maxContentLength = maxContentLength;
      return this;
    }

    /**
     * Set {@link Awaiter} factory.
     *
     * @since 5.0
     */
    public Builder awaiterFactory(Function<HttpRequest, Awaiter> awaiterFactory) {
      Assert.notNull(awaiterFactory, "awaiterFactory is required");
      this.awaiterFactory = awaiterFactory;
      return this;
    }

    public NettyRequestConfig build() {
      return new NettyRequestConfig(this);
    }

  }

}
