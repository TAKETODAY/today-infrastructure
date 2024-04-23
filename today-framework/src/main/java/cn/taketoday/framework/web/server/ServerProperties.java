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

package cn.taketoday.framework.web.server;

import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.NestedConfigurationProperty;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.framework.web.error.ErrorProperties;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.DataSize;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
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
  @Nullable
  public Integer port;

  /**
   * Network address to which the server should bind.
   */
  @Nullable
  public InetAddress address;

  @NestedConfigurationProperty
  public final EncodingProperties encoding = new EncodingProperties();

  @NestedConfigurationProperty
  public final ErrorProperties error = new ErrorProperties();

  /**
   * Strategy for handling X-Forwarded-* headers.
   */
  @Nullable
  public ForwardHeadersStrategy forwardHeadersStrategy;

  /**
   * Value to use for the Server response header (if empty, no header is sent).
   */
  @Nullable
  public String serverHeader;

  /**
   * Maximum size of the HTTP message header.
   */
  public DataSize maxHttpRequestHeaderSize = DataSize.ofKilobytes(8);

  /**
   * Type of shutdown that the server will support.
   */
  @Nullable
  public Shutdown shutdown = Shutdown.IMMEDIATE;

  @Nullable
  @NestedConfigurationProperty
  public Ssl ssl;

  @Nullable
  @NestedConfigurationProperty
  public Compression compression;

  @Nullable
  @NestedConfigurationProperty
  public Http2 http2;

  public final Netty netty = new Netty();

  public final ReactorNetty reactorNetty = new ReactorNetty();

  public void applyTo(ConfigurableWebServerFactory factory,
          @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    if (sslBundles != null) {
      factory.setSslBundles(sslBundles);
    }

    if (applicationTemp != null) {
      factory.setApplicationTemp(applicationTemp);
    }
    applyTo(factory);
  }

  public void applyTo(ConfigurableWebServerFactory factory) {
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

    if (serverHeader != null) {
      factory.setServerHeader(serverHeader);
    }
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
    @Nullable
    public Integer workerThreads;

    /**
     * the number of threads that will be used by
     * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
     *
     * For parent {@link EventLoopGroup}
     *
     * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
     */
    @Nullable
    public Integer acceptorThreads;

    /**
     * The SOMAXCONN value of the current machine. If failed to get the value,  {@code 200} is used as a
     * default value for Windows and {@code 128} for others.
     */
    @Nullable
    public Integer maxConnection;

    @Nullable
    public Class<? extends ServerSocketChannel> socketChannel;

    @Nullable
    public LogLevel loggingLevel;

    /**
     * the maximum length of the aggregated content.
     * If the length of the aggregated content exceeds this value,
     *
     * @see HttpObjectAggregator#maxContentLength
     */
    public DataSize maxContentLength = DataSize.ofMegabytes(100);

    /**
     * If a 100-continue response is detected but the content
     * length is too large then true means close the connection.
     * otherwise the connection will remain open and data will be
     * consumed and discarded until the next request is received.
     *
     * @see HttpObjectAggregator#closeOnExpectationFailed
     */
    public boolean closeOnExpectationFailed = false;

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
     * The maximum line length of header lines.
     * <p>
     * This limits how much memory Netty will use when parsing
     * HTTP header key-value pairs.
     */
    public int maxHeaderSize = 8192;

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

    public final Shutdown shutdown = new Shutdown();

    public final NettySSL ssl = new NettySSL();

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
    public Duration connectionTimeout;

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
     * Maximum length that can be decoded for an HTTP request's initial line.
     */
    public DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

    /**
     * Maximum number of requests that can be made per connection. By default, a
     * connection serves unlimited number of requests.
     */
    public Integer maxKeepAliveRequests;

    /**
     * Whether to validate headers when decoding requests.
     */
    public boolean validateHeaders = true;

    /**
     * Idle timeout of the Netty channel. When not specified, an infinite timeout is
     * used.
     */
    public Duration idleTimeout;

  }

  public static class NettySSL {
    /**
     * Whether to enable SSL support.
     */
    public boolean enabled = false;

    /**
     * Private key resource location
     */
    @Nullable
    public String privateKey;

    /**
     * Private key password
     */
    @Nullable
    public String keyPassword;

    /**
     * Public key resource location
     */
    @Nullable
    public String publicKey;

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
