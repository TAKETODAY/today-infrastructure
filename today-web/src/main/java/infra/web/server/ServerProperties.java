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

package infra.web.server;

import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.core.ApplicationTemp;
import infra.core.ssl.SslBundles;
import infra.util.DataSize;
import infra.web.multipart.MultipartParser;
import infra.web.multipart.parsing.DefaultMultipartParser;
import infra.web.server.error.ErrorProperties;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.logging.LogLevel;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for a web server (e.g. port
 * and path settings).
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Ivan Sopov
 * @author Marcos Barbero
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 * @author Aurélien Leboulanger
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @author Andrew McGhie
 * @author Rafiullah Hamedy
 * @author Dirk Deyne
 * @author HaiTao Zhang
 * @author Victor Mandujano
 * @author Chris Bono
 * @author Parviz Rozikov
 * @author Florian Storz
 * @author Michael Weidmann
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

  /**
   * Server HTTP port.
   */
  public @Nullable Integer port;

  /**
   * Network address to which the server should bind.
   */
  public @Nullable InetAddress address;

  @NestedConfigurationProperty
  public final EncodingProperties encoding = new EncodingProperties();

  @NestedConfigurationProperty
  public final ErrorProperties error = new ErrorProperties();

  @NestedConfigurationProperty
  public final Multipart multipart = new Multipart();

  /**
   * Strategy for handling X-Forwarded-* headers.
   */
  public @Nullable ForwardHeadersStrategy forwardHeadersStrategy;

  /**
   * Type of shutdown that the server will support.
   */
  public @Nullable Shutdown shutdown = Shutdown.GRACEFUL;

  @NestedConfigurationProperty
  public @Nullable Ssl ssl;

  @NestedConfigurationProperty
  public @Nullable Compression compression;

  @NestedConfigurationProperty
  public @Nullable Http2 http2;

  @NestedConfigurationProperty
  public final Netty netty = new Netty();

  @NestedConfigurationProperty
  public final ReactorNetty reactorNetty = new ReactorNetty();

  public void applyTo(ConfigurableWebServerFactory factory,
          @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    if (sslBundles != null) {
      factory.setSslBundles(sslBundles);
    }

    if (applicationTemp != null) {
      factory.setApplicationTemp(applicationTemp);
    }

    if (ssl != null) {
      factory.setSsl(ssl);
    }
    if (port != null) {
      factory.setPort(port);
    }

    factory.setHttp2(http2);

    if (address != null) {
      factory.setAddress(address);
    }

    if (shutdown != null) {
      factory.setShutdown(shutdown);
    }

    factory.setCompression(compression);
  }

  /**
   * Properties to be used in configuring a {@link MultipartParser}.
   *
   * @see DefaultMultipartParser
   * @since 5.0
   */
  public static class Multipart {

    /**
     * directory path where to store disk attributes and file uploads.
     * If mixedMode is disabled and this property is not empty will be
     * using disk mode
     */
    public @Nullable String tempBaseDir;

    /**
     * Subdirectory name where temporary files will be stored.
     * This property is used together with baseDir to create the full path
     * for storing uploaded files and temporary data.
     *
     * @see ApplicationTemp
     */
    public @Nullable String tempSubDir;

    /**
     * true if temporary files should be deleted with the JVM, false otherwise.
     */
    public boolean deleteOnExit; // false is a good default cause true leaks

    /**
     * Maximum number of fields allowed in a single multipart request.
     * This limits the number of individual form fields that can be submitted
     * to prevent excessive memory consumption from malformed or malicious requests.
     */
    public int maxFields = 128;

    /**
     * Default character set used for encoding multipart data.
     * <p>
     * This charset is used when decoding multipart form data when no specific
     * charset is specified in the request. UTF-8 is recommended as it provides
     * comprehensive Unicode support.
     */
    public Charset defaultCharset = StandardCharsets.UTF_8;

    /**
     * Threshold for field size in multipart requests.
     * Fields larger than this threshold will be stored on disk rather than in memory.
     */
    public DataSize fieldSizeThreshold = DataSize.ofKilobytes(16); // 16kB

    /**
     * Buffer size used for parsing multipart data.
     * <p>
     * This setting determines the size of the internal buffer used when parsing
     * multipart form data. Larger values may improve parsing performance for
     * larger files but will consume more memory per request.
     */
    public DataSize parsingBufferSize = DataSize.ofKilobytes(4); // 4KB

    /**
     * Maximum size of the HTTP message header for multipart requests.
     * <p>
     * This setting controls the maximum amount of bytes that can be used
     * for parsing HTTP headers in multipart form data. Headers exceeding
     * this limit will cause the request to be rejected.
     */
    public DataSize maxHeaderSize = DataSize.ofBytes(512); // 512 B
  }

  /**
   * Netty properties.
   */
  public static class Netty {

    /**
     * the number of threads that will be used by
     * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
     *
     * For child {@link EventLoopGroup}
     *
     * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
     */
    public @Nullable Integer workerThreads;

    /**
     * the number of threads that will be used by
     * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
     *
     * For parent {@link EventLoopGroup}
     *
     * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
     */
    public @Nullable Integer acceptorThreads;

    /**
     * The worker thread pool name
     *
     * @since 5.0
     */
    public @Nullable String workerPoolName;

    /**
     * The acceptor thread pool name
     *
     * @since 5.0
     */
    public @Nullable String acceptorPoolName;

    /**
     * The SOMAXCONN value of the current machine. If failed to get the value, {@code 200} is used as a
     * default value for Windows and {@code 128} for others.
     */
    public @Nullable Integer maxConnection;

    /**
     * The ServerSocketChannel class to be used by the Netty server.
     * <p>
     * This allows customization of the underlying socket channel implementation
     * that Netty will use for accepting incoming connections.
     */
    public @Nullable Class<? extends ServerSocketChannel> socketChannel;

    /**
     * Set netty LoggingHandler logging Level. If that loggingLevel
     * is null will not register logging handler
     */
    public @Nullable LogLevel loggingLevel;

    /**
     * The maximum length of the request body content.
     * <p>
     * This setting controls the maximum amount of data that can be received
     * in a single HTTP request body. Requests exceeding this limit will
     * typically result in an error being returned to the client.
     */
    public DataSize maxContentLength = DataSize.ofMegabytes(100);

    /**
     * The maximum chunk size.
     * <p>
     * HTTP requests and responses can be quite large, in which case
     * it's better to process the data as a stream of chunks. This
     * sets the limit, in bytes, at which Netty will send a chunk
     * down the pipeline.
     */
    public DataSize maxChunkSize = DataSize.ofBytes(8192);

    /**
     * Initial buffer size for HTTP request decoding.
     * <p>
     * This setting determines the initial capacity of the buffer used when
     * decoding incoming HTTP requests. A larger buffer may improve performance
     * for requests with large headers or bodies, while a smaller buffer may
     * reduce memory usage for applications handling many concurrent requests.
     */
    public DataSize initialBufferSize = DataSize.ofBytes(128);

    /**
     * The maximum line length of header lines.
     * <p>
     * This limits how much memory Netty will use when parsing
     * HTTP header key-value pairs.
     */
    public DataSize maxHeaderSize = DataSize.ofBytes(8192);

    /**
     * The maximum length of the first line of the HTTP header.
     * <p>
     * This limits how much memory Netty will use when parsed the
     * initial HTTP header line.
     */
    public int maxInitialLineLength = 4096;

    /**
     * Whether header validation should be enabled or not.
     * <p>
     * You usually want header validation enabled (which is the default)
     * in order to prevent request-/response-splitting attacks.
     */
    public boolean validateHeaders = true;

    /**
     * Set whether {@code Transfer-Encoding: Chunked} should be supported.
     * if {@code false}, then a {@code Transfer-Encoding: Chunked} header will produce an error,
     * instead of a stream of chunks.
     */
    public boolean chunkedSupported = true;

    /**
     * Set whether chunks can be split into multiple messages, if their
     * chunk size exceeds the size of the input buffer. If set to {@code false}
     * to only allow sending whole chunks down the pipeline.
     */
    public boolean allowPartialChunks = true;

    /**
     * Set whether more than one {@code Content-Length} header is allowed.
     * You usually want to disallow this (which is the default) as multiple
     * {@code Content-Length} headers can indicate a request- or response-splitting attack.
     * if set to {@code true} to allow multiple content length headers.
     */
    public boolean allowDuplicateContentLengths = false;

    /**
     * The capacity of the queue used to store received data chunks.
     * <p>
     * This setting controls how many data chunks can be queued for processing
     * before backpressure is applied to the data source. A larger queue can
     * help smooth out bursts of data but will consume more memory.
     */
    public int dataReceivedQueueCapacity = 256;

    /**
     * shutdown details
     */
    @NestedConfigurationProperty
    public final Shutdown shutdown = new Shutdown();

    public static class Shutdown {

      /**
       * Graceful shutdown ensures that no tasks are submitted for
       * 'the quiet period' (usually a couple seconds) before it shuts
       * itself down. If a task is submitted during the quiet period,
       * it is guaranteed to be accepted and the quiet period will start over.
       */
      public long quietPeriod = 1;

      /**
       * The maximum amount of time to wait until the executor is
       * shutdown() regardless if a task was submitted during the quiet period
       */
      public long timeout = 10;

      /**
       * The unit of quietPeriod and timeout
       */
      public TimeUnit unit = TimeUnit.SECONDS;

    }

  }

  /**
   * ReactorNetty properties.
   */
  public static class ReactorNetty {

    /**
     * Connection timeout of the Netty channel.
     */
    public @Nullable Duration connectionTimeout;

    /**
     * Maximum content length of an H2C upgrade request.
     */
    public DataSize h2cMaxContentLength = DataSize.ofBytes(0);

    /**
     * Initial buffer size for HTTP request decoding.
     */
    public DataSize initialBufferSize = DataSize.ofBytes(128);

    /**
     * Maximum chunk size that can be decoded for an HTTP request.
     */
    public DataSize maxChunkSize = DataSize.ofKilobytes(8);

    /**
     * Maximum size of the HTTP message header.
     */
    public DataSize maxHeaderSize = DataSize.ofKilobytes(8);

    /**
     * Maximum length that can be decoded for an HTTP request's initial line.
     */
    public DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

    /**
     * Maximum number of requests that can be made per connection. By default, a
     * connection serves unlimited number of requests.
     */
    public @Nullable Integer maxKeepAliveRequests;

    /**
     * Whether to validate headers when decoding requests.
     */
    public boolean validateHeaders = true;

    /**
     * Idle timeout of the Netty channel. When not specified, an infinite timeout is
     * used.
     */
    public @Nullable Duration idleTimeout;

  }

  /**
   * Strategies for supporting forward headers.
   */
  public enum ForwardHeadersStrategy {

    /**
     * Use the underlying container's native support for forwarded headers.
     */
    NATIVE,

    /**
     * Use Infra support for handling forwarded headers.
     */
    FRAMEWORK,

    /**
     * Ignore X-Forwarded-* headers.
     */
    NONE

  }

}
