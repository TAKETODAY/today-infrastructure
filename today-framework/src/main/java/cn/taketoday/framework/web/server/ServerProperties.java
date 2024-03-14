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

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.NestedConfigurationProperty;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.framework.web.error.ErrorProperties;
import cn.taketoday.framework.web.servlet.server.ConfigurableServletWebServerFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.config.SessionProperties;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.StringUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.undertow.UndertowOptions;

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

  //  @NestedConfigurationProperty
  public final SessionProperties session = new SessionProperties();

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

  public final Servlet servlet = new Servlet();

  public final Tomcat tomcat = new Tomcat();

  public final Jetty jetty = new Jetty();

  public final Netty netty = new Netty();

  public final ReactorNetty reactorNetty = new ReactorNetty();

  public final Undertow undertow = new Undertow();

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

  public void applyTo(ConfigurableServletWebServerFactory factory,
          @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    if (sslBundles != null) {
      factory.setSslBundles(sslBundles);
    }

    if (applicationTemp != null) {
      factory.setApplicationTemp(applicationTemp);
    }

    applyTo(factory);

    if (servlet.contextPath != null) {
      factory.setContextPath(servlet.contextPath);
    }

    factory.setSession(session);
    factory.setInitParameters(servlet.contextParameters);
    factory.setRegisterDefaultServlet(servlet.registerDefaultServlet);

    if (servlet.applicationDisplayName != null) {
      factory.setDisplayName(servlet.applicationDisplayName);
    }

    if (encoding.getMapping() != null) {
      factory.setLocaleCharsetMappings(encoding.getMapping());
    }
  }

  /**
   * Servlet server properties.
   */
  public static class Servlet {

    /**
     * Servlet context init parameters.
     */
    private final Map<String, String> contextParameters = new HashMap<>();

    /**
     * Context path of the application.
     */
    @Nullable
    public String contextPath;

    /**
     * Display name of the application.
     */
    @Nullable
    public String applicationDisplayName = "application";

    /**
     * Whether to register the default Servlet with the container.
     */
    public boolean registerDefaultServlet = false;

    public void setContextPath(@Nullable String contextPath) {
      this.contextPath = cleanContextPath(contextPath);
    }

    @Nullable
    private String cleanContextPath(@Nullable String contextPath) {
      String candidate = null;
      if (StringUtils.isNotEmpty(contextPath)) {
        candidate = contextPath.strip();
      }
      if (StringUtils.hasText(candidate) && candidate.endsWith("/")) {
        return candidate.substring(0, candidate.length() - 1);
      }
      return candidate;
    }

  }

  /**
   * Tomcat properties.
   */
  public static class Tomcat {

    /**
     * Access log configuration.
     */
    public final Accesslog accesslog = new Accesslog();

    /**
     * Thread related configuration.
     */
    public final Threads threads = new Threads();

    /**
     * Tomcat base directory. If not specified, a temporary directory is used.
     */
    public File basedir;

    /**
     * Delay between the invocation of backgroundProcess methods. If a duration suffix
     * is not specified, seconds will be used.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    public Duration backgroundProcessorDelay = Duration.ofSeconds(10);

    /**
     * Maximum size of the form content in any HTTP post request.
     */
    public DataSize maxHttpFormPostSize = DataSize.ofMegabytes(2);

    /**
     * Maximum amount of request body to swallow.
     */
    public DataSize maxSwallowSize = DataSize.ofMegabytes(2);

    /**
     * Whether requests to the context root should be redirected by appending a / to
     * the path. When using SSL terminated at a proxy, this property should be set to
     * false.
     */
    @Nullable
    public Boolean redirectContextRoot = true;

    /**
     * Whether HTTP 1.1 and later location headers generated by a call to sendRedirect
     * will use relative or absolute redirects.
     */
    public boolean useRelativeRedirects;

    /**
     * Character encoding to use to decode the URI.
     */
    public Charset uriEncoding = StandardCharsets.UTF_8;

    /**
     * Maximum number of connections that the server accepts and processes at any
     * given time. Once the limit has been reached, the operating system may still
     * accept connections based on the "acceptCount" property.
     */
    public int maxConnections = 8192;

    /**
     * Maximum queue length for incoming connection requests when all possible request
     * processing threads are in use.
     */
    public int acceptCount = 100;

    /**
     * Maximum number of idle processors that will be retained in the cache and reused
     * with a subsequent request. When set to -1 the cache will be unlimited with a
     * theoretical maximum size equal to the maximum number of connections.
     */
    public int processorCache = 200;

    /**
     * Time to wait for another HTTP request before the connection is closed. When not
     * set the connectionTimeout is used. When set to -1 there will be no timeout.
     */
    public Duration keepAliveTimeout;

    /**
     * Maximum number of HTTP requests that can be pipelined before the connection is
     * closed. When set to 0 or 1, keep-alive and pipelining are disabled. When set to
     * -1, an unlimited number of pipelined or keep-alive requests are allowed.
     */
    public int maxKeepAliveRequests = 100;

    /**
     * Comma-separated list of additional patterns that match jars to ignore for TLD
     * scanning. The special '?' and '*' characters can be used in the pattern to
     * match one and only one character and zero or more characters respectively.
     */
    public List<String> additionalTldSkipPatterns = new ArrayList<>();

    /**
     * Comma-separated list of additional unencoded characters that should be allowed
     * in URI paths. Only {@code "< > [ \ ] ^ ` { | }"} are allowed.
     */
    public List<Character> relaxedPathChars = new ArrayList<>();

    /**
     * Comma-separated list of additional unencoded characters that should be allowed
     * in URI query strings. Only {@code "< > [ \ ] ^ ` { | }"} are allowed.
     */
    public List<Character> relaxedQueryChars = new ArrayList<>();

    /**
     * Amount of time the connector will wait, after accepting a connection, for the
     * request URI line to be presented.
     */
    public Duration connectionTimeout;

    /**
     * Whether to reject requests with illegal header names or values.
     */
    public boolean rejectIllegalHeader = true;

    /**
     * Static resource configuration.
     */
    public final Resource resource = new Resource();

    /**
     * Modeler MBean Registry configuration.
     */
    public final Mbeanregistry mbeanregistry = new Mbeanregistry();

    /**
     * Remote Ip Valve configuration.
     */
    public final Remoteip remoteip = new Remoteip();

    /**
     * Maximum size of the HTTP response header.
     */
    public DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

    /**
     * Tomcat access log properties.
     */
    public static class Accesslog {

      /**
       * Enable access log.
       */
      public boolean enabled = false;

      /**
       * Whether logging of the request will only be enabled if
       * "ServletRequest.getAttribute(conditionIf)" does not yield null.
       */
      public String conditionIf;

      /**
       * Whether logging of the request will only be enabled if
       * "ServletRequest.getAttribute(conditionUnless)" yield null.
       */
      public String conditionUnless;

      /**
       * Format pattern for access logs.
       */
      public String pattern = "common";

      /**
       * Directory in which log files are created. Can be absolute or relative to
       * the Tomcat base dir.
       */
      public String directory = "logs";

      /**
       * Log file name prefix.
       */
      public String prefix = "access_log";

      /**
       * Log file name suffix.
       */
      public String suffix = ".log";

      /**
       * Character set used by the log file. Default to the system default character
       * set.
       */
      public String encoding;

      /**
       * Locale used to format timestamps in log entries and in log file name
       * suffix. Default to the default locale of the Java process.
       */
      public String locale;

      /**
       * Whether to check for log file existence so it can be recreated it if an
       * external process has renamed it.
       */
      public boolean checkExists = false;

      /**
       * Whether to enable access log rotation.
       */
      public boolean rotate = true;

      /**
       * Whether to defer inclusion of the date stamp in the file name until rotate
       * time.
       */
      public boolean renameOnRotate = false;

      /**
       * Number of days to retain the access log files before they are removed.
       */
      public int maxDays = -1;

      /**
       * Date format to place in the log file name.
       */
      public String fileDateFormat = ".yyyy-MM-dd";

      /**
       * Whether to use IPv6 canonical representation format as defined by RFC 5952.
       */
      public boolean ipv6Canonical = false;

      /**
       * Set request attributes for the IP address, Hostname, protocol, and port
       * used for the request.
       */
      public boolean requestAttributesEnabled = false;

      /**
       * Whether to buffer output such that it is flushed only periodically.
       */
      public boolean buffered = true;

    }

    /**
     * Tomcat thread properties.
     */
    public static class Threads {

      /**
       * Maximum amount of worker threads.
       */
      public int max = 200;

      /**
       * Minimum amount of worker threads.
       */
      public int minSpare = 10;

      /**
       * Maximum capacity of the thread pool's backing queue.
       */
      public int maxQueueCapacity = 2147483647;

    }

    /**
     * Tomcat static resource properties.
     */
    public static class Resource {

      /**
       * Whether static resource caching is permitted for this web application.
       */
      public boolean allowCaching = true;

      /**
       * Time-to-live of the static resource cache.
       */
      @Nullable
      public Duration cacheTtl;

    }

    public static class Mbeanregistry {

      /**
       * Whether Tomcat's MBean Registry should be enabled.
       */
      public boolean enabled;

    }

    public static class Remoteip {

      /**
       * Regular expression that matches proxies that are to be trusted.
       */
      public String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
              + "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
              + "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
              + "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
              + "100\\.6[4-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
              + "100\\.[7-9]{1}\\d{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
              + "100\\.1[0-1]{1}\\d{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
              + "100\\.12[0-7]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
              + "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
              + "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
              + "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
              + "0:0:0:0:0:0:0:1|::1";

      /**
       * Header that holds the incoming protocol, usually named "X-Forwarded-Proto".
       */
      public String protocolHeader;

      /**
       * Value of the protocol header indicating whether the incoming request uses
       * SSL.
       */
      public String protocolHeaderHttpsValue = "https";

      /**
       * Name of the HTTP header from which the remote host is extracted.
       */
      public String hostHeader = "X-Forwarded-Host";

      /**
       * Name of the HTTP header used to override the original port value.
       */
      public String portHeader = "X-Forwarded-Port";

      /**
       * Name of the HTTP header from which the remote IP is extracted. For
       * instance, 'X-FORWARDED-FOR'.
       */
      public String remoteIpHeader;

      /**
       * Regular expression defining proxies that are trusted when they appear in
       * the "remote-ip-header" header.
       */
      public String trustedProxies;

    }

  }

  /**
   * Jetty properties.
   */
  public static class Jetty {

    /**
     * Access log configuration.
     */
    public final Accesslog accesslog = new Accesslog();

    /**
     * Thread related configuration.
     */
    public final Threads threads = new Threads();

    /**
     * Maximum size of the form content in any HTTP post request.
     */
    public DataSize maxHttpFormPostSize = DataSize.ofBytes(200000);

    /**
     * Time that the connection can be idle before it is closed.
     */
    public Duration connectionIdleTimeout;

    /**
     * Maximum size of the HTTP response header.
     */
    public DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

    /**
     * Maximum number of connections that the server accepts and processes at any
     * given time.
     */
    public int maxConnections = -1;

    /**
     * Jetty access log properties.
     */
    public static class Accesslog {

      /**
       * Enable access log.
       */
      public boolean enabled = false;

      /**
       * Log format.
       */
      public FORMAT format = FORMAT.NCSA;

      /**
       * Custom log format, see org.eclipse.jetty.server.CustomRequestLog. If
       * defined, overrides the "format" configuration key.
       */
      @Nullable
      public String customFormat;

      /**
       * Log filename. If not specified, logs redirect to "System.err".
       */
      @Nullable
      public String filename;

      /**
       * Date format to place in log file name.
       */
      @Nullable
      public String fileDateFormat;

      /**
       * Number of days before rotated log files are deleted.
       */
      public int retentionPeriod = 31; // no days

      /**
       * Append to log.
       */
      public boolean append;

      /**
       * Request paths that should not be logged.
       */
      @Nullable
      public List<String> ignorePaths;

      /**
       * Log format for Jetty access logs.
       */
      public enum FORMAT {

        /**
         * NCSA format, as defined in CustomRequestLog#NCSA_FORMAT.
         */
        NCSA,

        /**
         * Extended NCSA format, as defined in
         * CustomRequestLog#EXTENDED_NCSA_FORMAT.
         */
        EXTENDED_NCSA

      }

    }

    /**
     * Jetty thread properties.
     */
    public static class Threads {

      /**
       * Number of acceptor threads to use. When the value is -1, the default, the
       * number of acceptors is derived from the operating environment.
       */
      public Integer acceptors = -1;

      /**
       * Number of selector threads to use. When the value is -1, the default, the
       * number of selectors is derived from the operating environment.
       */
      public Integer selectors = -1;

      /**
       * Maximum number of threads.
       */
      public Integer max = 200;

      /**
       * Minimum number of threads.
       */
      public Integer min = 8;

      /**
       * Maximum capacity of the thread pool's backing queue. A default is computed
       * based on the threading configuration.
       */
      public Integer maxQueueCapacity;

      /**
       * Maximum thread idle time.
       */
      @Nullable
      public Duration idleTimeout = Duration.ofMillis(60000);

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

    public static class NettySSL {
      /**
       * Whether to enable SSL support.
       */
      public boolean enabled = false;

      /**
       * Private key resource location
       */
      public String privateKey;

      /**
       * Private key password
       */
      @Nullable
      public String keyPassword;

      /**
       * Public key resource location
       */
      public String publicKey;

    }

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

  /**
   * Undertow properties.
   */
  public static class Undertow {

    /**
     * Maximum size of the HTTP post content. When the value is -1, the default, the
     * size is unlimited.
     */
    public DataSize maxHttpPostSize = DataSize.ofBytes(-1);

    /**
     * Size of each buffer. The default is derived from the maximum amount of memory
     * that is available to the JVM.
     */
    public DataSize bufferSize;

    /**
     * Whether to allocate buffers outside the Java heap. The default is derived from
     * the maximum amount of memory that is available to the JVM.
     */
    public Boolean directBuffers;

    /**
     * Whether servlet filters should be initialized on startup.
     */
    public boolean eagerFilterInit = true;

    /**
     * Maximum number of query or path parameters that are allowed. This limit exists
     * to prevent hash collision based DOS attacks.
     */
    public int maxParameters = UndertowOptions.DEFAULT_MAX_PARAMETERS;

    /**
     * Maximum number of headers that are allowed. This limit exists to prevent hash
     * collision based DOS attacks.
     */
    public int maxHeaders = UndertowOptions.DEFAULT_MAX_HEADERS;

    /**
     * Maximum number of cookies that are allowed. This limit exists to prevent hash
     * collision based DOS attacks.
     */
    public int maxCookies = 200;

    /**
     * Whether encoded slash characters (%2F) should be decoded. Decoding can cause
     * security problems if a front-end proxy does not perform the same decoding. Only
     * enable this if you have a legacy application that requires it. When set,
     * server.undertow.allow-encoded-slash has no effect.
     */
    public Boolean decodeSlash;

    /**
     * Whether the URL should be decoded. When disabled, percent-encoded characters in
     * the URL will be left as-is.
     */
    public boolean decodeUrl = true;

    /**
     * Charset used to decode URLs.
     */
    public Charset urlCharset = StandardCharsets.UTF_8;

    /**
     * Whether the 'Connection: keep-alive' header should be added to all responses,
     * even if not required by the HTTP specification.
     */
    public boolean alwaysSetKeepAlive = true;

    /**
     * Amount of time a connection can sit idle without processing a request, before
     * it is closed by the server.
     */
    public Duration noRequestTimeout;

    /**
     * Whether to preserve the path of a request when it is forwarded.
     */
    public boolean preservePathOnForward = false;

    public final Accesslog accesslog = new Accesslog();

    /**
     * Thread related configuration.
     */
    public final Threads threads = new Threads();

    public final Options options = new Options();

    /**
     * Undertow access log properties.
     */
    public static class Accesslog {

      /**
       * Whether to enable the access log.
       */
      public boolean enabled = false;

      /**
       * Format pattern for access logs.
       */
      public String pattern = "common";

      /**
       * Log file name prefix.
       */
      public String prefix = "access_log.";

      /**
       * Log file name suffix.
       */
      public String suffix = "log";

      /**
       * Undertow access log directory.
       */
      public File dir = new File("logs");

      /**
       * Whether to enable access log rotation.
       */
      public boolean rotate = true;

    }

    /**
     * Undertow thread properties.
     */
    public static class Threads {

      /**
       * Number of I/O threads to create for the worker. The default is derived from
       * the number of available processors.
       */
      public Integer io;

      /**
       * Number of worker threads. The default is 8 times the number of I/O threads.
       */
      public Integer worker;

    }

    public static class Options {

      public final Map<String, String> socket = new LinkedHashMap<>();

      public final Map<String, String> server = new LinkedHashMap<>();

    }

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
