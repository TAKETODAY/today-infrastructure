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
import java.net.SocketAddress;

import infra.core.ApplicationTemp;
import infra.core.ssl.SslBundles;

/**
 * A configurable {@link WebServerFactory}.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConfigurableWebServerFactory extends WebServerFactory {

  /**
   * Sets the port that the web server should listen on. If not specified port '8080'
   * will be used. Use port -1 to disable auto-start (i.e. start the web application
   * context but not have it listen to any port).
   *
   * @param port the port to set
   */
  void setPort(int port);

  /**
   * Sets the specific network address that the server should bind to.
   *
   * @param address the address to set (defaults to {@code null})
   */
  void setAddress(@Nullable InetAddress address);

  /**
   * Sets the bind address for the web server.
   *
   * @param bindAddress the bind address
   * @since 5.0
   */
  void setBindAddress(@Nullable SocketAddress bindAddress);

  /**
   * Sets the SSL configuration that will be applied to the server's default connector.
   *
   * @param ssl the SSL configuration
   */
  void setSsl(@Nullable Ssl ssl);

  /**
   * Sets the SSL bundles that can be used to configure SSL connections.
   *
   * @param sslBundles the SSL bundles
   */
  void setSslBundles(@Nullable SslBundles sslBundles);

  /**
   * Sets the HTTP/2 configuration that will be applied to the server.
   *
   * @param http2 the HTTP/2 configuration
   */
  void setHttp2(@Nullable Http2 http2);

  /**
   * Sets the compression configuration that will be applied to the server's default
   * connector.
   *
   * @param compression the compression configuration
   */
  void setCompression(@Nullable Compression compression);

  /**
   * Sets the shutdown configuration that will be applied to the server.
   *
   * @param shutdown the shutdown configuration
   */
  void setShutdown(Shutdown shutdown);

  /**
   * Sets the application temp. Provides access to an
   * application specific temporary directory
   *
   * @param applicationTemp the app temp dir
   */
  void setApplicationTemp(ApplicationTemp applicationTemp);

}
