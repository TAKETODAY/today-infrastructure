/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server;

import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.core.ApplicationTemp;
import infra.core.ssl.SslBundles;
import infra.util.DataSize;
import infra.util.StringUtils;

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

  /**
   * Whether to use virtual threads for the service executor.
   * <p>
   * When set to {@code true}, the server will use virtual threads instead of
   * platform threads for handling requests. This can significantly improve
   * scalability for I/O-intensive applications running on Java 21+.
   * <p>
   * Defaults to {@code false}, meaning platform threads are used by default.
   *
   * @since 5.0
   */
  public boolean useVirtualThreadServiceExecutor = false;

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
   * Properties to be used in configuring a {@link infra.web.multipart.MultipartParser}.
   *
   * @see infra.web.multipart.parsing.DefaultMultipartParser
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

    public Path computeTempRepository(@Nullable ApplicationTemp applicationTemp) {
      if (StringUtils.hasText(tempBaseDir)) {
        if (StringUtils.hasText(tempSubDir)) {
          return Path.of(tempBaseDir, tempSubDir);
        }
        else {
          return Path.of(tempBaseDir);
        }
      }
      else {
        if (applicationTemp == null) {
          applicationTemp = ApplicationTemp.instance;
        }
        return applicationTemp.getDir(Objects.requireNonNullElse(tempSubDir, "multipart"));
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
