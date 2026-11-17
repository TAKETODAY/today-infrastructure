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

import java.util.function.Function;

import infra.util.Assert;
import infra.util.DataSize;
import infra.web.HttpContext;
import infra.web.multipart.MultipartParser;
import infra.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.HttpDataFactory;

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
 *     .multipartParser(new DefaultMultipartParser())
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
 *   <li>Integration with Netty's {@link HttpDataFactory}.</li>
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
 * @see HttpDataFactory
 * @see SendErrorHandler
 * @since 2021/3/30 17:46
 */
public final class NettyRequestConfig {

  /**
   * Server-side cookie encoder used to write {@code Set-Cookie} response headers.
   *
   * @see ServerCookieEncoder
   */
  public final ServerCookieEncoder cookieEncoder;

  /**
   * Server-side cookie decoder used to read the {@code Cookie} request header.
   *
   * @see ServerCookieDecoder
   */
  public final ServerCookieDecoder cookieDecoder;

  /**
   * response body initial size
   *
   * @see io.netty.buffer.ByteBufAllocator#ioBuffer(int)
   */
  public final int responseBodyInitialCapacity;

  /**
   * Optional factory for the initial response body buffer.
   * <p>When it returns {@code null}, the buffer is created from the channel allocator.
   *
   * @see #responseBodyInitialCapacity
   */
  public final @Nullable Function<HttpContext, ByteBuf> responseBodyFactory;

  /**
   * Handler invoked to render an error response.
   *
   * @see SendErrorHandler
   */
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
   * Parser used for {@code multipart/form-data} requests.
   *
   * @see MultipartParser
   * @since 5.0
   */
  public final MultipartParser multipartParser;

  /**
   * Capacity of the queue that buffers received request body chunks.
   *
   * @see BodyInputStream#BodyInputStream(int)
   * @since 5.0
   */
  public final int dataReceivedQueueCapacity;

  /**
   * Whether the request body is read on demand instead of being received
   * automatically by the channel.
   * <p>When {@code true}, channel auto-read is paused for the duration of the
   * exchange and body frames are requested as the application consumes them.
   *
   * @see io.netty.channel.ChannelConfig#setAutoRead(boolean)
   * @since 5.0
   */
  public final boolean manualBodyRead;

  private NettyRequestConfig(Builder builder) {
    Assert.notNull(builder.sendErrorHandler, "SendErrorHandler is required");
    Assert.notNull(builder.multipartParser, "MultipartParser is required");
    Assert.isTrue(builder.responseBodyInitialCapacity > 0, "responseBodyInitialCapacity is required");

    this.secure = builder.secure;
    this.manualBodyRead = builder.manualBodyRead;
    this.cookieEncoder = builder.cookieEncoder;
    this.cookieDecoder = builder.cookieDecoder;
    this.multipartParser = builder.multipartParser;
    this.maxContentLength = builder.maxContentLength;
    this.sendErrorHandler = builder.sendErrorHandler;
    this.responseBodyFactory = builder.responseBodyFactory;
    this.dataReceivedQueueCapacity = builder.dataReceivedQueueCapacity;
    this.responseBodyInitialCapacity = builder.responseBodyInitialCapacity;
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

  public static class Builder {

    /**
     * Cookie encoder for response headers; {@link ServerCookieEncoder#STRICT} by default.
     */
    private ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;

    /**
     * Cookie decoder for request headers; {@link ServerCookieDecoder#STRICT} by default.
     */
    private ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;

    /**
     * response body initial size
     *
     * @see io.netty.buffer.ByteBufAllocator#ioBuffer(int)
     */
    private int responseBodyInitialCapacity = 128;

    /**
     * Optional factory for the initial response body buffer.
     */
    private @Nullable Function<HttpContext, ByteBuf> responseBodyFactory;

    /**
     * Maximum accepted request content length; 1GB by default.
     */
    private long maxContentLength = DataSize.BYTES_PER_GB;

    /**
     * Handler invoked to render an error response.
     */
    private @Nullable SendErrorHandler sendErrorHandler;

    /**
     * Parser used for {@code multipart/form-data} requests.
     */
    private @Nullable MultipartParser multipartParser;

    /**
     * Capacity of the queue that buffers received request body chunks; 256 by default.
     */
    private int dataReceivedQueueCapacity = 256;

    /**
     * Whether the request body is read on demand; {@code false} by default.
     */
    private boolean manualBodyRead = false;

    /**
     * Whether this configuration is built for a secure connection.
     */
    private final boolean secure;

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
     * The provided factory function takes a {@link HttpContext} as input and returns
     * a {@link ByteBuf} representing the response body. This allows dynamic generation
     * of response bodies based on the request context.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.responseBodyFactory(http -> {
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
    public Builder responseBodyFactory(@Nullable Function<HttpContext, ByteBuf> factory) {
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
     * Sets the {@link infra.web.multipart.parsing.DefaultMultipartParser} to be used for parsing multipart requests.
     * <p>
     * This method allows configuring a custom multipart parser that will be used
     * to handle multipart/form-data requests. The provided parser will be responsible
     * for parsing the multipart content and extracting individual parts.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   MultipartParser customParser = new CustomMultipartParser();
     *   builder.multipartParser(customParser);
     * }</pre>
     *
     * @param multipartParser the {@link MultipartParser} instance to be used for
     * parsing multipart requests. Must not be {@code null}.
     * @return the current {@link Builder} instance, enabling method chaining
     * @throws IllegalArgumentException if the provided multipartParser is {@code null}
     * @since 5.0
     */
    public Builder multipartParser(MultipartParser multipartParser) {
      Assert.notNull(multipartParser, "MultipartParser is required");
      this.multipartParser = multipartParser;
      return this;
    }

    /**
     * Sets the capacity of the queue used for receiving data.
     * <p>
     * This method configures the maximum number of data items that can be queued
     * for processing before the queue starts blocking or rejecting new items.
     * Adjusting this value can help balance between memory usage and throughput
     * based on the application's requirements.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.dataReceivedQueueCapacity(1000); // Set queue capacity to 1000 items
     * }</pre>
     *
     * @param dataReceivedQueueCapacity the capacity of the data received queue.
     * Must be a non-negative integer.
     * @return the current {@link Builder} instance, enabling method chaining
     * @see BodyInputStream#BodyInputStream(int)
     * @since 5.0
     */
    public Builder dataReceivedQueueCapacity(int dataReceivedQueueCapacity) {
      Assert.isTrue(dataReceivedQueueCapacity > 0, "dataReceivedQueueCapacity must be great than 0");
      this.dataReceivedQueueCapacity = dataReceivedQueueCapacity;
      return this;
    }

    /**
     * Sets whether the request body is read on demand.
     * <p>When enabled, channel auto-read is paused while the exchange is active
     * and body frames are requested as the application consumes them. This is
     * not Netty's connection-level {@code AUTO_READ} channel option.
     * <p>
     * Example usage:
     * <pre>{@code
     *   Builder builder = ...;
     *   builder.manualBodyRead(true);
     * }</pre>
     *
     * @param manualBodyRead {@code true} to read the body on demand,
     * {@code false} to receive it automatically
     * @return the current {@link Builder} instance, enabling method chaining
     * @see io.netty.channel.ChannelConfig#setAutoRead(boolean)
     * @since 5.0
     */
    public Builder manualBodyRead(boolean manualBodyRead) {
      this.manualBodyRead = manualBodyRead;
      return this;
    }

    public NettyRequestConfig build() {
      return new NettyRequestConfig(this);
    }

  }

}
