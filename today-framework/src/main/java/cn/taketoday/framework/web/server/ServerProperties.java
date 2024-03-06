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
import cn.taketoday.session.config.CookieProperties;
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
  private Integer port;

  /**
   * Network address to which the server should bind.
   */
  @Nullable
  private InetAddress address;

  @NestedConfigurationProperty
  private final EncodingProperties encoding = new EncodingProperties();

  //  @NestedConfigurationProperty
  private final SessionProperties session = new SessionProperties();

  @NestedConfigurationProperty
  private final ErrorProperties error = new ErrorProperties();

  /**
   * Strategy for handling X-Forwarded-* headers.
   */
  @Nullable
  private ForwardHeadersStrategy forwardHeadersStrategy;

  /**
   * Value to use for the Server response header (if empty, no header is sent).
   */
  @Nullable
  private String serverHeader;

  /**
   * Maximum size of the HTTP message header.
   */
  private DataSize maxHttpRequestHeaderSize = DataSize.ofKilobytes(8);

  /**
   * Type of shutdown that the server will support.
   */
  @Nullable
  private Shutdown shutdown = Shutdown.IMMEDIATE;

  @Nullable
  @NestedConfigurationProperty
  private Ssl ssl;

  @Nullable
  @NestedConfigurationProperty
  private Compression compression;

  @Nullable
  @NestedConfigurationProperty
  private Http2 http2;

  private final Servlet servlet = new Servlet();

  private final Reactive reactive = new Reactive();

  private final Tomcat tomcat = new Tomcat();

  private final Jetty jetty = new Jetty();

  private final Netty netty = new Netty();

  private final ReactorNetty reactorNetty = new ReactorNetty();

  private final Undertow undertow = new Undertow();

  @Nullable
  public Integer getPort() {
    return this.port;
  }

  public void setPort(@Nullable Integer port) {
    this.port = port;
  }

  @Nullable
  public InetAddress getAddress() {
    return this.address;
  }

  public void setAddress(@Nullable InetAddress address) {
    this.address = address;
  }

  @Nullable
  public String getServerHeader() {
    return this.serverHeader;
  }

  public void setServerHeader(@Nullable String serverHeader) {
    this.serverHeader = serverHeader;
  }

  public DataSize getMaxHttpRequestHeaderSize() {
    return this.maxHttpRequestHeaderSize;
  }

  public void setMaxHttpRequestHeaderSize(DataSize maxHttpRequestHeaderSize) {
    this.maxHttpRequestHeaderSize = maxHttpRequestHeaderSize;
  }

  @Nullable
  public Shutdown getShutdown() {
    return this.shutdown;
  }

  public void setShutdown(@Nullable Shutdown shutdown) {
    this.shutdown = shutdown;
  }

  public ErrorProperties getError() {
    return this.error;
  }

  @Nullable
  public Ssl getSsl() {
    return this.ssl;
  }

  public void setSsl(@Nullable Ssl ssl) {
    this.ssl = ssl;
  }

  @Nullable
  public Compression getCompression() {
    return this.compression;
  }

  public void setCompression(@Nullable Compression compression) {
    this.compression = compression;
  }

  public void setHttp2(@Nullable Http2 http2) {
    this.http2 = http2;
  }

  @Nullable
  public Http2 getHttp2() {
    return this.http2;
  }

  public Servlet getServlet() {
    return this.servlet;
  }

  public Reactive getReactive() {
    return this.reactive;
  }

  public Tomcat getTomcat() {
    return this.tomcat;
  }

  public Jetty getJetty() {
    return this.jetty;
  }

  public Netty getNetty() {
    return netty;
  }

  public ReactorNetty getReactorNetty() {
    return this.reactorNetty;
  }

  public Undertow getUndertow() {
    return this.undertow;
  }

  @Nullable
  public ForwardHeadersStrategy getForwardHeadersStrategy() {
    return this.forwardHeadersStrategy;
  }

  public void setForwardHeadersStrategy(@Nullable ForwardHeadersStrategy forwardHeadersStrategy) {
    this.forwardHeadersStrategy = forwardHeadersStrategy;
  }

  public EncodingProperties getEncoding() {
    return this.encoding;
  }

  public SessionProperties getSession() {
    return this.session;
  }

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
    private String contextPath;

    /**
     * Display name of the application.
     */
    @Nullable
    private String applicationDisplayName = "application";

    /**
     * Whether to register the default Servlet with the container.
     */
    private boolean registerDefaultServlet = false;

    @Nullable
    public String getContextPath() {
      return this.contextPath;
    }

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

    @Nullable
    public String getApplicationDisplayName() {
      return this.applicationDisplayName;
    }

    public void setApplicationDisplayName(@Nullable String displayName) {
      this.applicationDisplayName = displayName;
    }

    public boolean isRegisterDefaultServlet() {
      return this.registerDefaultServlet;
    }

    public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
      this.registerDefaultServlet = registerDefaultServlet;
    }

    public Map<String, String> getContextParameters() {
      return this.contextParameters;
    }

  }

  /**
   * Reactive server properties.
   */
  public static class Reactive {

    private final Session session = new Session();

    public Session getSession() {
      return this.session;
    }

    public static class Session {

      /**
       * Session timeout. If a duration suffix is not specified, seconds will be
       * used.
       */
      @DurationUnit(ChronoUnit.SECONDS)
      private Duration timeout = Duration.ofMinutes(30);

      private final CookieProperties cookie = new CookieProperties();

      public Duration getTimeout() {
        return this.timeout;
      }

      public void setTimeout(Duration timeout) {
        this.timeout = timeout;
      }

      public CookieProperties getCookie() {
        return this.cookie;
      }

    }

  }

  /**
   * Tomcat properties.
   */
  public static class Tomcat {

    /**
     * Access log configuration.
     */
    private final Accesslog accesslog = new Accesslog();

    /**
     * Thread related configuration.
     */
    private final Threads threads = new Threads();

    /**
     * Tomcat base directory. If not specified, a temporary directory is used.
     */
    private File basedir;

    /**
     * Delay between the invocation of backgroundProcess methods. If a duration suffix
     * is not specified, seconds will be used.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration backgroundProcessorDelay = Duration.ofSeconds(10);

    /**
     * Maximum size of the form content in any HTTP post request.
     */
    private DataSize maxHttpFormPostSize = DataSize.ofMegabytes(2);

    /**
     * Maximum amount of request body to swallow.
     */
    private DataSize maxSwallowSize = DataSize.ofMegabytes(2);

    /**
     * Whether requests to the context root should be redirected by appending a / to
     * the path. When using SSL terminated at a proxy, this property should be set to
     * false.
     */
    private Boolean redirectContextRoot = true;

    /**
     * Whether HTTP 1.1 and later location headers generated by a call to sendRedirect
     * will use relative or absolute redirects.
     */
    private boolean useRelativeRedirects;

    /**
     * Character encoding to use to decode the URI.
     */
    private Charset uriEncoding = StandardCharsets.UTF_8;

    /**
     * Maximum number of connections that the server accepts and processes at any
     * given time. Once the limit has been reached, the operating system may still
     * accept connections based on the "acceptCount" property.
     */
    private int maxConnections = 8192;

    /**
     * Maximum queue length for incoming connection requests when all possible request
     * processing threads are in use.
     */
    private int acceptCount = 100;

    /**
     * Maximum number of idle processors that will be retained in the cache and reused
     * with a subsequent request. When set to -1 the cache will be unlimited with a
     * theoretical maximum size equal to the maximum number of connections.
     */
    private int processorCache = 200;

    /**
     * Time to wait for another HTTP request before the connection is closed. When not
     * set the connectionTimeout is used. When set to -1 there will be no timeout.
     */
    private Duration keepAliveTimeout;

    /**
     * Maximum number of HTTP requests that can be pipelined before the connection is
     * closed. When set to 0 or 1, keep-alive and pipelining are disabled. When set to
     * -1, an unlimited number of pipelined or keep-alive requests are allowed.
     */
    private int maxKeepAliveRequests = 100;

    /**
     * Comma-separated list of additional patterns that match jars to ignore for TLD
     * scanning. The special '?' and '*' characters can be used in the pattern to
     * match one and only one character and zero or more characters respectively.
     */
    private List<String> additionalTldSkipPatterns = new ArrayList<>();

    /**
     * Comma-separated list of additional unencoded characters that should be allowed
     * in URI paths. Only {@code "< > [ \ ] ^ ` { | }"} are allowed.
     */
    private List<Character> relaxedPathChars = new ArrayList<>();

    /**
     * Comma-separated list of additional unencoded characters that should be allowed
     * in URI query strings. Only {@code "< > [ \ ] ^ ` { | }"} are allowed.
     */
    private List<Character> relaxedQueryChars = new ArrayList<>();

    /**
     * Amount of time the connector will wait, after accepting a connection, for the
     * request URI line to be presented.
     */
    private Duration connectionTimeout;

    /**
     * Whether to reject requests with illegal header names or values.
     */
    private boolean rejectIllegalHeader = true;

    /**
     * Static resource configuration.
     */
    private final Resource resource = new Resource();

    /**
     * Modeler MBean Registry configuration.
     */
    private final Mbeanregistry mbeanregistry = new Mbeanregistry();

    /**
     * Remote Ip Valve configuration.
     */
    private final Remoteip remoteip = new Remoteip();

    /**
     * Maximum size of the HTTP response header.
     */
    private DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

    public DataSize getMaxHttpFormPostSize() {
      return this.maxHttpFormPostSize;
    }

    public void setMaxHttpFormPostSize(DataSize maxHttpFormPostSize) {
      this.maxHttpFormPostSize = maxHttpFormPostSize;
    }

    public Accesslog getAccesslog() {
      return this.accesslog;
    }

    public Threads getThreads() {
      return this.threads;
    }

    public Duration getBackgroundProcessorDelay() {
      return this.backgroundProcessorDelay;
    }

    public void setBackgroundProcessorDelay(Duration backgroundProcessorDelay) {
      this.backgroundProcessorDelay = backgroundProcessorDelay;
    }

    public File getBasedir() {
      return this.basedir;
    }

    public void setBasedir(File basedir) {
      this.basedir = basedir;
    }

    public Boolean getRedirectContextRoot() {
      return this.redirectContextRoot;
    }

    public void setRedirectContextRoot(Boolean redirectContextRoot) {
      this.redirectContextRoot = redirectContextRoot;
    }

    public boolean isUseRelativeRedirects() {
      return this.useRelativeRedirects;
    }

    public void setUseRelativeRedirects(boolean useRelativeRedirects) {
      this.useRelativeRedirects = useRelativeRedirects;
    }

    public Charset getUriEncoding() {
      return this.uriEncoding;
    }

    public void setUriEncoding(Charset uriEncoding) {
      this.uriEncoding = uriEncoding;
    }

    public int getMaxConnections() {
      return this.maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
    }

    public DataSize getMaxSwallowSize() {
      return this.maxSwallowSize;
    }

    public void setMaxSwallowSize(DataSize maxSwallowSize) {
      this.maxSwallowSize = maxSwallowSize;
    }

    public int getAcceptCount() {
      return this.acceptCount;
    }

    public void setAcceptCount(int acceptCount) {
      this.acceptCount = acceptCount;
    }

    public int getProcessorCache() {
      return this.processorCache;
    }

    public void setProcessorCache(int processorCache) {
      this.processorCache = processorCache;
    }

    public Duration getKeepAliveTimeout() {
      return this.keepAliveTimeout;
    }

    public void setKeepAliveTimeout(Duration keepAliveTimeout) {
      this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getMaxKeepAliveRequests() {
      return this.maxKeepAliveRequests;
    }

    public void setMaxKeepAliveRequests(int maxKeepAliveRequests) {
      this.maxKeepAliveRequests = maxKeepAliveRequests;
    }

    public List<String> getAdditionalTldSkipPatterns() {
      return this.additionalTldSkipPatterns;
    }

    public void setAdditionalTldSkipPatterns(List<String> additionalTldSkipPatterns) {
      this.additionalTldSkipPatterns = additionalTldSkipPatterns;
    }

    public List<Character> getRelaxedPathChars() {
      return this.relaxedPathChars;
    }

    public void setRelaxedPathChars(List<Character> relaxedPathChars) {
      this.relaxedPathChars = relaxedPathChars;
    }

    public List<Character> getRelaxedQueryChars() {
      return this.relaxedQueryChars;
    }

    public void setRelaxedQueryChars(List<Character> relaxedQueryChars) {
      this.relaxedQueryChars = relaxedQueryChars;
    }

    public Duration getConnectionTimeout() {
      return this.connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
    }

    public boolean isRejectIllegalHeader() {
      return this.rejectIllegalHeader;
    }

    public void setRejectIllegalHeader(boolean rejectIllegalHeader) {
      this.rejectIllegalHeader = rejectIllegalHeader;
    }

    public Resource getResource() {
      return this.resource;
    }

    public Mbeanregistry getMbeanregistry() {
      return this.mbeanregistry;
    }

    public Remoteip getRemoteip() {
      return this.remoteip;
    }

    public DataSize getMaxHttpResponseHeaderSize() {
      return maxHttpResponseHeaderSize;
    }

    public void setMaxHttpResponseHeaderSize(DataSize maxHttpResponseHeaderSize) {
      this.maxHttpResponseHeaderSize = maxHttpResponseHeaderSize;
    }

    /**
     * Tomcat access log properties.
     */
    public static class Accesslog {

      /**
       * Enable access log.
       */
      private boolean enabled = false;

      /**
       * Whether logging of the request will only be enabled if
       * "ServletRequest.getAttribute(conditionIf)" does not yield null.
       */
      private String conditionIf;

      /**
       * Whether logging of the request will only be enabled if
       * "ServletRequest.getAttribute(conditionUnless)" yield null.
       */
      private String conditionUnless;

      /**
       * Format pattern for access logs.
       */
      private String pattern = "common";

      /**
       * Directory in which log files are created. Can be absolute or relative to
       * the Tomcat base dir.
       */
      private String directory = "logs";

      /**
       * Log file name prefix.
       */
      protected String prefix = "access_log";

      /**
       * Log file name suffix.
       */
      private String suffix = ".log";

      /**
       * Character set used by the log file. Default to the system default character
       * set.
       */
      private String encoding;

      /**
       * Locale used to format timestamps in log entries and in log file name
       * suffix. Default to the default locale of the Java process.
       */
      private String locale;

      /**
       * Whether to check for log file existence so it can be recreated it if an
       * external process has renamed it.
       */
      private boolean checkExists = false;

      /**
       * Whether to enable access log rotation.
       */
      private boolean rotate = true;

      /**
       * Whether to defer inclusion of the date stamp in the file name until rotate
       * time.
       */
      private boolean renameOnRotate = false;

      /**
       * Number of days to retain the access log files before they are removed.
       */
      private int maxDays = -1;

      /**
       * Date format to place in the log file name.
       */
      private String fileDateFormat = ".yyyy-MM-dd";

      /**
       * Whether to use IPv6 canonical representation format as defined by RFC 5952.
       */
      private boolean ipv6Canonical = false;

      /**
       * Set request attributes for the IP address, Hostname, protocol, and port
       * used for the request.
       */
      private boolean requestAttributesEnabled = false;

      /**
       * Whether to buffer output such that it is flushed only periodically.
       */
      private boolean buffered = true;

      public boolean isEnabled() {
        return this.enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getConditionIf() {
        return this.conditionIf;
      }

      public void setConditionIf(String conditionIf) {
        this.conditionIf = conditionIf;
      }

      public String getConditionUnless() {
        return this.conditionUnless;
      }

      public void setConditionUnless(String conditionUnless) {
        this.conditionUnless = conditionUnless;
      }

      public String getPattern() {
        return this.pattern;
      }

      public void setPattern(String pattern) {
        this.pattern = pattern;
      }

      public String getDirectory() {
        return this.directory;
      }

      public void setDirectory(String directory) {
        this.directory = directory;
      }

      public String getPrefix() {
        return this.prefix;
      }

      public void setPrefix(String prefix) {
        this.prefix = prefix;
      }

      public String getSuffix() {
        return this.suffix;
      }

      public void setSuffix(String suffix) {
        this.suffix = suffix;
      }

      public String getEncoding() {
        return this.encoding;
      }

      public void setEncoding(String encoding) {
        this.encoding = encoding;
      }

      public String getLocale() {
        return this.locale;
      }

      public void setLocale(String locale) {
        this.locale = locale;
      }

      public boolean isCheckExists() {
        return this.checkExists;
      }

      public void setCheckExists(boolean checkExists) {
        this.checkExists = checkExists;
      }

      public boolean isRotate() {
        return this.rotate;
      }

      public void setRotate(boolean rotate) {
        this.rotate = rotate;
      }

      public boolean isRenameOnRotate() {
        return this.renameOnRotate;
      }

      public void setRenameOnRotate(boolean renameOnRotate) {
        this.renameOnRotate = renameOnRotate;
      }

      public int getMaxDays() {
        return this.maxDays;
      }

      public void setMaxDays(int maxDays) {
        this.maxDays = maxDays;
      }

      public String getFileDateFormat() {
        return this.fileDateFormat;
      }

      public void setFileDateFormat(String fileDateFormat) {
        this.fileDateFormat = fileDateFormat;
      }

      public boolean isIpv6Canonical() {
        return this.ipv6Canonical;
      }

      public void setIpv6Canonical(boolean ipv6Canonical) {
        this.ipv6Canonical = ipv6Canonical;
      }

      public boolean isRequestAttributesEnabled() {
        return this.requestAttributesEnabled;
      }

      public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
      }

      public boolean isBuffered() {
        return this.buffered;
      }

      public void setBuffered(boolean buffered) {
        this.buffered = buffered;
      }

    }

    /**
     * Tomcat thread properties.
     */
    public static class Threads {

      /**
       * Maximum amount of worker threads.
       */
      private int max = 200;

      /**
       * Minimum amount of worker threads.
       */
      private int minSpare = 10;

      /**
       * Maximum capacity of the thread pool's backing queue.
       */
      private int maxQueueCapacity = 2147483647;

      public int getMax() {
        return this.max;
      }

      public void setMax(int max) {
        this.max = max;
      }

      public int getMinSpare() {
        return this.minSpare;
      }

      public void setMinSpare(int minSpare) {
        this.minSpare = minSpare;
      }

      public int getMaxQueueCapacity() {
        return this.maxQueueCapacity;
      }

      public void setMaxQueueCapacity(int maxQueueCapacity) {
        this.maxQueueCapacity = maxQueueCapacity;
      }

    }

    /**
     * Tomcat static resource properties.
     */
    public static class Resource {

      /**
       * Whether static resource caching is permitted for this web application.
       */
      private boolean allowCaching = true;

      /**
       * Time-to-live of the static resource cache.
       */
      private Duration cacheTtl;

      public boolean isAllowCaching() {
        return this.allowCaching;
      }

      public void setAllowCaching(boolean allowCaching) {
        this.allowCaching = allowCaching;
      }

      public Duration getCacheTtl() {
        return this.cacheTtl;
      }

      public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
      }

    }

    public static class Mbeanregistry {

      /**
       * Whether Tomcat's MBean Registry should be enabled.
       */
      private boolean enabled;

      public boolean isEnabled() {
        return this.enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

    }

    public static class Remoteip {

      /**
       * Regular expression that matches proxies that are to be trusted.
       */
      private String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
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
      private String protocolHeader;

      /**
       * Value of the protocol header indicating whether the incoming request uses
       * SSL.
       */
      private String protocolHeaderHttpsValue = "https";

      /**
       * Name of the HTTP header from which the remote host is extracted.
       */
      private String hostHeader = "X-Forwarded-Host";

      /**
       * Name of the HTTP header used to override the original port value.
       */
      private String portHeader = "X-Forwarded-Port";

      /**
       * Name of the HTTP header from which the remote IP is extracted. For
       * instance, 'X-FORWARDED-FOR'.
       */
      private String remoteIpHeader;

      /**
       * Regular expression defining proxies that are trusted when they appear in
       * the "remote-ip-header" header.
       */
      private String trustedProxies;

      public String getInternalProxies() {
        return this.internalProxies;
      }

      public void setInternalProxies(String internalProxies) {
        this.internalProxies = internalProxies;
      }

      public String getProtocolHeader() {
        return this.protocolHeader;
      }

      public void setProtocolHeader(String protocolHeader) {
        this.protocolHeader = protocolHeader;
      }

      public String getProtocolHeaderHttpsValue() {
        return this.protocolHeaderHttpsValue;
      }

      public String getHostHeader() {
        return this.hostHeader;
      }

      public void setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
      }

      public void setProtocolHeaderHttpsValue(String protocolHeaderHttpsValue) {
        this.protocolHeaderHttpsValue = protocolHeaderHttpsValue;
      }

      public String getPortHeader() {
        return this.portHeader;
      }

      public void setPortHeader(String portHeader) {
        this.portHeader = portHeader;
      }

      public String getRemoteIpHeader() {
        return this.remoteIpHeader;
      }

      public void setRemoteIpHeader(String remoteIpHeader) {
        this.remoteIpHeader = remoteIpHeader;
      }

      public String getTrustedProxies() {
        return this.trustedProxies;
      }

      public void setTrustedProxies(String trustedProxies) {
        this.trustedProxies = trustedProxies;
      }

    }

  }

  /**
   * Jetty properties.
   */
  public static class Jetty {

    /**
     * Access log configuration.
     */
    private final Accesslog accesslog = new Accesslog();

    /**
     * Thread related configuration.
     */
    private final Threads threads = new Threads();

    /**
     * Maximum size of the form content in any HTTP post request.
     */
    private DataSize maxHttpFormPostSize = DataSize.ofBytes(200000);

    /**
     * Time that the connection can be idle before it is closed.
     */
    private Duration connectionIdleTimeout;

    /**
     * Maximum size of the HTTP response header.
     */
    private DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

    /**
     * Maximum number of connections that the server accepts and processes at any
     * given time.
     */
    private int maxConnections = -1;

    public Accesslog getAccesslog() {
      return this.accesslog;
    }

    public Threads getThreads() {
      return this.threads;
    }

    public DataSize getMaxHttpFormPostSize() {
      return this.maxHttpFormPostSize;
    }

    public void setMaxHttpFormPostSize(DataSize maxHttpFormPostSize) {
      this.maxHttpFormPostSize = maxHttpFormPostSize;
    }

    public Duration getConnectionIdleTimeout() {
      return this.connectionIdleTimeout;
    }

    public void setConnectionIdleTimeout(Duration connectionIdleTimeout) {
      this.connectionIdleTimeout = connectionIdleTimeout;
    }

    public void setMaxHttpResponseHeaderSize(DataSize maxHttpResponseHeaderSize) {
      this.maxHttpResponseHeaderSize = maxHttpResponseHeaderSize;
    }

    public DataSize getMaxHttpResponseHeaderSize() {
      return maxHttpResponseHeaderSize;
    }

    public int getMaxConnections() {
      return this.maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
    }

    /**
     * Jetty access log properties.
     */
    public static class Accesslog {

      /**
       * Enable access log.
       */
      private boolean enabled = false;

      /**
       * Log format.
       */
      private FORMAT format = FORMAT.NCSA;

      /**
       * Custom log format, see org.eclipse.jetty.server.CustomRequestLog. If
       * defined, overrides the "format" configuration key.
       */
      private String customFormat;

      /**
       * Log filename. If not specified, logs redirect to "System.err".
       */
      private String filename;

      /**
       * Date format to place in log file name.
       */
      private String fileDateFormat;

      /**
       * Number of days before rotated log files are deleted.
       */
      private int retentionPeriod = 31; // no days

      /**
       * Append to log.
       */
      private boolean append;

      /**
       * Request paths that should not be logged.
       */
      private List<String> ignorePaths;

      public boolean isEnabled() {
        return this.enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public FORMAT getFormat() {
        return this.format;
      }

      public void setFormat(FORMAT format) {
        this.format = format;
      }

      @Nullable
      public String getCustomFormat() {
        return this.customFormat;
      }

      public void setCustomFormat(String customFormat) {
        this.customFormat = customFormat;
      }

      @Nullable
      public String getFilename() {
        return this.filename;
      }

      public void setFilename(String filename) {
        this.filename = filename;
      }

      @Nullable
      public String getFileDateFormat() {
        return this.fileDateFormat;
      }

      public void setFileDateFormat(String fileDateFormat) {
        this.fileDateFormat = fileDateFormat;
      }

      public int getRetentionPeriod() {
        return this.retentionPeriod;
      }

      public void setRetentionPeriod(int retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
      }

      public boolean isAppend() {
        return this.append;
      }

      public void setAppend(boolean append) {
        this.append = append;
      }

      public List<String> getIgnorePaths() {
        return this.ignorePaths;
      }

      public void setIgnorePaths(List<String> ignorePaths) {
        this.ignorePaths = ignorePaths;
      }

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
      private Integer acceptors = -1;

      /**
       * Number of selector threads to use. When the value is -1, the default, the
       * number of selectors is derived from the operating environment.
       */
      private Integer selectors = -1;

      /**
       * Maximum number of threads.
       */
      private Integer max = 200;

      /**
       * Minimum number of threads.
       */
      private Integer min = 8;

      /**
       * Maximum capacity of the thread pool's backing queue. A default is computed
       * based on the threading configuration.
       */
      private Integer maxQueueCapacity;

      /**
       * Maximum thread idle time.
       */
      @Nullable
      private Duration idleTimeout = Duration.ofMillis(60000);

      public Integer getAcceptors() {
        return this.acceptors;
      }

      public void setAcceptors(Integer acceptors) {
        this.acceptors = acceptors;
      }

      public Integer getSelectors() {
        return this.selectors;
      }

      public void setSelectors(Integer selectors) {
        this.selectors = selectors;
      }

      public void setMin(Integer min) {
        this.min = min;
      }

      public Integer getMin() {
        return this.min;
      }

      public void setMax(Integer max) {
        this.max = max;
      }

      public Integer getMax() {
        return this.max;
      }

      public Integer getMaxQueueCapacity() {
        return this.maxQueueCapacity;
      }

      public void setMaxQueueCapacity(Integer maxQueueCapacity) {
        this.maxQueueCapacity = maxQueueCapacity;
      }

      public void setIdleTimeout(@Nullable Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
      }

      @Nullable
      public Duration getIdleTimeout() {
        return this.idleTimeout;
      }

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
    private Integer workerThreads;

    /**
     * the number of threads that will be used by
     * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
     *
     * For parent {@link EventLoopGroup}
     *
     * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
     */
    @Nullable
    private Integer acceptorThreads;

    /**
     * The SOMAXCONN value of the current machine. If failed to get the value,  {@code 200} is used as a
     * default value for Windows and {@code 128} for others.
     */
    @Nullable
    private Integer maxConnection;

    @Nullable
    private Class<? extends ServerSocketChannel> socketChannel;

    @Nullable
    private LogLevel loggingLevel;

    /**
     * the maximum length of the aggregated content.
     * If the length of the aggregated content exceeds this value,
     *
     * @see HttpObjectAggregator#maxContentLength
     */
    private DataSize maxContentLength = DataSize.ofMegabytes(100);

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
     * The maximum chunk size.
     * <p>
     * HTTP requests and responses can be quite large, in which case
     * it's better to process the data as a stream of chunks. This
     * sets the limit, in bytes, at which Netty will send a chunk
     * down the pipeline.
     */
    private DataSize maxChunkSize = DataSize.ofBytes(8192);

    /**
     * The maximum line length of header lines.
     * <p>
     * This limits how much memory Netty will use when parsing
     * HTTP header key-value pairs. You would typically set this
     * to the same value as {@link #setMaxInitialLineLength(int)}.
     */
    private int maxHeaderSize = 8192;

    /**
     * The maximum length of the first line of the HTTP header.
     * <p>
     * This limits how much memory Netty will use when parsed the
     * initial HTTP header line. You would typically set this to
     * the same value as {@link #setMaxHeaderSize(int)}.
     */
    private int maxInitialLineLength = 4096;

    /**
     * Whether header validation should be enabled or not.
     * <p>
     * You usually want header validation enabled (which is the default)
     * in order to prevent request-/response-splitting attacks.
     */
    private boolean validateHeaders = true;

    private final Shutdown shutdown = new Shutdown();

    private final NettySSL ssl = new NettySSL();

    public void setLoggingLevel(@Nullable LogLevel loggingLevel) {
      this.loggingLevel = loggingLevel;
    }

    public void setSocketChannel(@Nullable Class<? extends ServerSocketChannel> socketChannel) {
      this.socketChannel = socketChannel;
    }

    public void setAcceptorThreads(@Nullable Integer acceptorThreads) {
      this.acceptorThreads = acceptorThreads;
    }

    public void setWorkerThreads(@Nullable Integer workerThreads) {
      this.workerThreads = workerThreads;
    }

    public void setMaxConnection(@Nullable Integer maxConnection) {
      this.maxConnection = maxConnection;
    }

    @Nullable
    public Integer getMaxConnection() {
      return maxConnection;
    }

    @Nullable
    public Class<? extends ServerSocketChannel> getSocketChannel() {
      return socketChannel;
    }

    @Nullable
    public Integer getAcceptorThreads() {
      return acceptorThreads;
    }

    @Nullable
    public Integer getWorkerThreads() {
      return workerThreads;
    }

    @Nullable
    public LogLevel getLoggingLevel() {
      return loggingLevel;
    }

    /**
     * Set the maximum chunk size.
     * <p>
     * HTTP requests and responses can be quite large, in which case
     * it's better to process the data as a stream of chunks. This
     * sets the limit, in bytes, at which Netty will send a chunk
     * down the pipeline.
     *
     * @param maxChunkSize The maximum chunk size.
     */
    public void setMaxChunkSize(DataSize maxChunkSize) {
      this.maxChunkSize = maxChunkSize;
    }

    /**
     * Set the maximum line length of header lines.
     * <p>
     * This limits how much memory Netty will use when parsing
     * HTTP header key-value pairs. You would typically set this
     * to the same value as {@link #setMaxInitialLineLength(int)}.
     *
     * @param maxHeaderSize The maximum length, in bytes.
     */
    public void setMaxHeaderSize(int maxHeaderSize) {
      this.maxHeaderSize = maxHeaderSize;
    }

    /**
     * Set whether header validation should be enabled or not.
     * <p>
     * You usually want header validation enabled (which is the default)
     * in order to prevent request-/response-splitting attacks.
     *
     * @param validateHeaders set to {@code false} to disable header validation.
     */
    public void setValidateHeaders(boolean validateHeaders) {
      this.validateHeaders = validateHeaders;
    }

    /**
     * Set the maximum length of the first line of the HTTP header.
     * <p>
     * This limits how much memory Netty will use when parsed the
     * initial HTTP header line. You would typically set this to
     * the same value as {@link #setMaxHeaderSize(int)}.
     *
     * @param maxInitialLineLength The maximum length, in bytes.
     */
    public void setMaxInitialLineLength(int maxInitialLineLength) {
      this.maxInitialLineLength = maxInitialLineLength;
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
     * Set the maximum length of the aggregated content.
     * If the length of the aggregated content exceeds this value,
     *
     * @param maxContentLength the maximum length of the aggregated content.
     * If the length of the aggregated content exceeds this value,
     * @see HttpObjectAggregator#maxContentLength
     */
    public void setMaxContentLength(DataSize maxContentLength) {
      this.maxContentLength = maxContentLength;
    }

    public DataSize getMaxContentLength() {
      return maxContentLength;
    }

    public boolean isCloseOnExpectationFailed() {
      return closeOnExpectationFailed;
    }

    public DataSize getMaxChunkSize() {
      return maxChunkSize;
    }

    public int getMaxHeaderSize() {
      return maxHeaderSize;
    }

    public int getMaxInitialLineLength() {
      return maxInitialLineLength;
    }

    public boolean isValidateHeaders() {
      return validateHeaders;
    }

    public Shutdown getShutdown() {
      return shutdown;
    }

    public NettySSL getSsl() {
      return ssl;
    }

    public static class NettySSL {

      private boolean enabled = false;

      private String privateKey;

      @Nullable
      private String keyPassword;

      private String publicKey;

      /**
       * Return whether to enable SSL support.
       *
       * @return whether to enable SSL support
       */
      public boolean isEnabled() {
        return this.enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
      }

      public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
      }

      public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
      }

      /**
       * Private key password
       *
       * @return Private key password
       */
      @Nullable
      public String getKeyPassword() {
        return keyPassword;
      }

      /**
       * Private key resource location
       *
       * @return privateKey
       */
      public String getPrivateKey() {
        return privateKey;
      }

      /**
       * Public key resource location
       *
       * @return Public key resource location
       */
      public String getPublicKey() {
        return publicKey;
      }
    }

    public static class Shutdown {

      /**
       * Graceful shutdown ensures that no tasks are submitted for
       * 'the quiet period' (usually a couple seconds) before it shuts
       * itself down. If a task is submitted during the quiet period,
       * it is guaranteed to be accepted and the quiet period will start over.
       */
      private long quietPeriod = 1;

      /**
       * The maximum amount of time to wait until the executor is
       * shutdown() regardless if a task was submitted during the quiet period
       */
      private long timeout = 10;

      /**
       * The unit of quietPeriod and timeout
       */
      private TimeUnit unit = TimeUnit.SECONDS;

      public void setQuietPeriod(long quietPeriod) {
        this.quietPeriod = quietPeriod;
      }

      public void setTimeout(long timeout) {
        this.timeout = timeout;
      }

      public void setUnit(TimeUnit unit) {
        this.unit = unit;
      }

      public long getQuietPeriod() {
        return quietPeriod;
      }

      public long getTimeout() {
        return timeout;
      }

      public TimeUnit getUnit() {
        return unit;
      }
    }

  }

  /**
   * ReactorNetty properties.
   */
  public static class ReactorNetty {

    /**
     * Connection timeout of the Netty channel.
     */
    private Duration connectionTimeout;

    /**
     * Maximum content length of an H2C upgrade request.
     */
    private DataSize h2cMaxContentLength = DataSize.ofBytes(0);

    /**
     * Initial buffer size for HTTP request decoding.
     */
    private DataSize initialBufferSize = DataSize.ofBytes(128);

    /**
     * Maximum chunk size that can be decoded for an HTTP request.
     */
    private DataSize maxChunkSize = DataSize.ofKilobytes(8);

    /**
     * Maximum length that can be decoded for an HTTP request's initial line.
     */
    private DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

    /**
     * Maximum number of requests that can be made per connection. By default, a
     * connection serves unlimited number of requests.
     */
    private Integer maxKeepAliveRequests;

    /**
     * Whether to validate headers when decoding requests.
     */
    private boolean validateHeaders = true;

    /**
     * Idle timeout of the Netty channel. When not specified, an infinite timeout is
     * used.
     */
    private Duration idleTimeout;

    public Duration getConnectionTimeout() {
      return this.connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
    }

    public DataSize getH2cMaxContentLength() {
      return this.h2cMaxContentLength;
    }

    public void setH2cMaxContentLength(DataSize h2cMaxContentLength) {
      this.h2cMaxContentLength = h2cMaxContentLength;
    }

    public DataSize getInitialBufferSize() {
      return this.initialBufferSize;
    }

    public void setInitialBufferSize(DataSize initialBufferSize) {
      this.initialBufferSize = initialBufferSize;
    }

    public DataSize getMaxChunkSize() {
      return this.maxChunkSize;
    }

    public void setMaxChunkSize(DataSize maxChunkSize) {
      this.maxChunkSize = maxChunkSize;
    }

    public DataSize getMaxInitialLineLength() {
      return this.maxInitialLineLength;
    }

    public void setMaxInitialLineLength(DataSize maxInitialLineLength) {
      this.maxInitialLineLength = maxInitialLineLength;
    }

    public Integer getMaxKeepAliveRequests() {
      return this.maxKeepAliveRequests;
    }

    public void setMaxKeepAliveRequests(Integer maxKeepAliveRequests) {
      this.maxKeepAliveRequests = maxKeepAliveRequests;
    }

    public boolean isValidateHeaders() {
      return this.validateHeaders;
    }

    public void setValidateHeaders(boolean validateHeaders) {
      this.validateHeaders = validateHeaders;
    }

    public Duration getIdleTimeout() {
      return this.idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
      this.idleTimeout = idleTimeout;
    }

  }

  /**
   * Undertow properties.
   */
  public static class Undertow {

    /**
     * Maximum size of the HTTP post content. When the value is -1, the default, the
     * size is unlimited.
     */
    private DataSize maxHttpPostSize = DataSize.ofBytes(-1);

    /**
     * Size of each buffer. The default is derived from the maximum amount of memory
     * that is available to the JVM.
     */
    private DataSize bufferSize;

    /**
     * Whether to allocate buffers outside the Java heap. The default is derived from
     * the maximum amount of memory that is available to the JVM.
     */
    private Boolean directBuffers;

    /**
     * Whether servlet filters should be initialized on startup.
     */
    private boolean eagerFilterInit = true;

    /**
     * Maximum number of query or path parameters that are allowed. This limit exists
     * to prevent hash collision based DOS attacks.
     */
    private int maxParameters = UndertowOptions.DEFAULT_MAX_PARAMETERS;

    /**
     * Maximum number of headers that are allowed. This limit exists to prevent hash
     * collision based DOS attacks.
     */
    private int maxHeaders = UndertowOptions.DEFAULT_MAX_HEADERS;

    /**
     * Maximum number of cookies that are allowed. This limit exists to prevent hash
     * collision based DOS attacks.
     */
    private int maxCookies = 200;

    /**
     * Whether encoded slash characters (%2F) should be decoded. Decoding can cause
     * security problems if a front-end proxy does not perform the same decoding. Only
     * enable this if you have a legacy application that requires it. When set,
     * server.undertow.allow-encoded-slash has no effect.
     */
    private Boolean decodeSlash;

    /**
     * Whether the URL should be decoded. When disabled, percent-encoded characters in
     * the URL will be left as-is.
     */
    private boolean decodeUrl = true;

    /**
     * Charset used to decode URLs.
     */
    private Charset urlCharset = StandardCharsets.UTF_8;

    /**
     * Whether the 'Connection: keep-alive' header should be added to all responses,
     * even if not required by the HTTP specification.
     */
    private boolean alwaysSetKeepAlive = true;

    /**
     * Amount of time a connection can sit idle without processing a request, before
     * it is closed by the server.
     */
    private Duration noRequestTimeout;

    /**
     * Whether to preserve the path of a request when it is forwarded.
     */
    private boolean preservePathOnForward = false;

    private final Accesslog accesslog = new Accesslog();

    /**
     * Thread related configuration.
     */
    private final Threads threads = new Threads();

    private final Options options = new Options();

    public DataSize getMaxHttpPostSize() {
      return this.maxHttpPostSize;
    }

    public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
      this.maxHttpPostSize = maxHttpPostSize;
    }

    public DataSize getBufferSize() {
      return this.bufferSize;
    }

    public void setBufferSize(DataSize bufferSize) {
      this.bufferSize = bufferSize;
    }

    public Boolean getDirectBuffers() {
      return this.directBuffers;
    }

    public void setDirectBuffers(Boolean directBuffers) {
      this.directBuffers = directBuffers;
    }

    public boolean isEagerFilterInit() {
      return this.eagerFilterInit;
    }

    public void setEagerFilterInit(boolean eagerFilterInit) {
      this.eagerFilterInit = eagerFilterInit;
    }

    public int getMaxParameters() {
      return this.maxParameters;
    }

    public void setMaxParameters(Integer maxParameters) {
      this.maxParameters = maxParameters;
    }

    public int getMaxHeaders() {
      return this.maxHeaders;
    }

    public void setMaxHeaders(int maxHeaders) {
      this.maxHeaders = maxHeaders;
    }

    public Integer getMaxCookies() {
      return this.maxCookies;
    }

    public void setMaxCookies(Integer maxCookies) {
      this.maxCookies = maxCookies;
    }

    public Boolean getDecodeSlash() {
      return this.decodeSlash;
    }

    public void setDecodeSlash(Boolean decodeSlash) {
      this.decodeSlash = decodeSlash;
    }

    public boolean isDecodeUrl() {
      return this.decodeUrl;
    }

    public void setDecodeUrl(Boolean decodeUrl) {
      this.decodeUrl = decodeUrl;
    }

    public Charset getUrlCharset() {
      return this.urlCharset;
    }

    public void setUrlCharset(Charset urlCharset) {
      this.urlCharset = urlCharset;
    }

    public boolean isAlwaysSetKeepAlive() {
      return this.alwaysSetKeepAlive;
    }

    public void setAlwaysSetKeepAlive(boolean alwaysSetKeepAlive) {
      this.alwaysSetKeepAlive = alwaysSetKeepAlive;
    }

    public Duration getNoRequestTimeout() {
      return this.noRequestTimeout;
    }

    public void setNoRequestTimeout(Duration noRequestTimeout) {
      this.noRequestTimeout = noRequestTimeout;
    }

    public boolean isPreservePathOnForward() {
      return this.preservePathOnForward;
    }

    public void setPreservePathOnForward(boolean preservePathOnForward) {
      this.preservePathOnForward = preservePathOnForward;
    }

    public Accesslog getAccesslog() {
      return this.accesslog;
    }

    public Threads getThreads() {
      return this.threads;
    }

    public Options getOptions() {
      return this.options;
    }

    /**
     * Undertow access log properties.
     */
    public static class Accesslog {

      /**
       * Whether to enable the access log.
       */
      private boolean enabled = false;

      /**
       * Format pattern for access logs.
       */
      private String pattern = "common";

      /**
       * Log file name prefix.
       */
      protected String prefix = "access_log.";

      /**
       * Log file name suffix.
       */
      private String suffix = "log";

      /**
       * Undertow access log directory.
       */
      private File dir = new File("logs");

      /**
       * Whether to enable access log rotation.
       */
      private boolean rotate = true;

      public boolean isEnabled() {
        return this.enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getPattern() {
        return this.pattern;
      }

      public void setPattern(String pattern) {
        this.pattern = pattern;
      }

      public String getPrefix() {
        return this.prefix;
      }

      public void setPrefix(String prefix) {
        this.prefix = prefix;
      }

      public String getSuffix() {
        return this.suffix;
      }

      public void setSuffix(String suffix) {
        this.suffix = suffix;
      }

      public File getDir() {
        return this.dir;
      }

      public void setDir(File dir) {
        this.dir = dir;
      }

      public boolean isRotate() {
        return this.rotate;
      }

      public void setRotate(boolean rotate) {
        this.rotate = rotate;
      }

    }

    /**
     * Undertow thread properties.
     */
    public static class Threads {

      /**
       * Number of I/O threads to create for the worker. The default is derived from
       * the number of available processors.
       */
      private Integer io;

      /**
       * Number of worker threads. The default is 8 times the number of I/O threads.
       */
      private Integer worker;

      public Integer getIo() {
        return this.io;
      }

      public void setIo(Integer io) {
        this.io = io;
      }

      public Integer getWorker() {
        return this.worker;
      }

      public void setWorker(Integer worker) {
        this.worker = worker;
      }

    }

    public static class Options {

      private final Map<String, String> socket = new LinkedHashMap<>();

      private final Map<String, String> server = new LinkedHashMap<>();

      public Map<String, String> getServer() {
        return this.server;
      }

      public Map<String, String> getSocket() {
        return this.socket;
      }

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
