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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import infra.core.ApplicationTemp;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Abstract base class for {@link ConfigurableWebServerFactory} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ivan Sopov
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractConfigurableWebServerFactory implements ConfigurableWebServerFactory {

  private int port = 8080;

  private @Nullable InetAddress address;

  private @Nullable Ssl ssl;

  private @Nullable SslBundles sslBundles;

  private @Nullable Http2 http2;

  private @Nullable Compression compression;

  private Shutdown shutdown = Shutdown.IMMEDIATE;

  private ApplicationTemp applicationTemp = ApplicationTemp.instance;

  /**
   * Create a new {@link AbstractConfigurableWebServerFactory} instance.
   */
  public AbstractConfigurableWebServerFactory() {
  }

  /**
   * Create a new {@link AbstractConfigurableWebServerFactory} instance with the
   * specified port.
   *
   * @param port the port number for the web server
   */
  public AbstractConfigurableWebServerFactory(int port) {
    this.port = port;
  }

  /**
   * The port that the web server listens on.
   *
   * @return the port
   */
  public int getPort() {
    return this.port;
  }

  @Override
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Return the address that the web server binds to.
   *
   * @return the address
   */
  public @Nullable InetAddress getAddress() {
    return this.address;
  }

  @Override
  public void setAddress(@Nullable InetAddress address) {
    this.address = address;
  }

  public @Nullable Ssl getSsl() {
    return this.ssl;
  }

  @Override
  public void setSsl(@Nullable Ssl ssl) {
    this.ssl = ssl;
  }

  /**
   * Return the configured {@link SslBundles}.
   *
   * @return the {@link SslBundles} or {@code null}
   */
  public @Nullable SslBundles getSslBundles() {
    return this.sslBundles;
  }

  @Override
  public void setSslBundles(@Nullable SslBundles sslBundles) {
    this.sslBundles = sslBundles;
  }

  public @Nullable Http2 getHttp2() {
    return this.http2;
  }

  @Override
  public void setHttp2(@Nullable Http2 http2) {
    this.http2 = http2;
  }

  public @Nullable Compression getCompression() {
    return this.compression;
  }

  @Override
  public void setCompression(@Nullable Compression compression) {
    this.compression = compression;
  }

  @Override
  public void setShutdown(Shutdown shutdown) {
    this.shutdown = shutdown;
  }

  /**
   * Returns the shutdown configuration that will be applied to the server.
   *
   * @return the shutdown configuration
   */
  public Shutdown getShutdown() {
    return this.shutdown;
  }

  @Override
  public void setApplicationTemp(ApplicationTemp applicationTemp) {
    Assert.notNull(applicationTemp, "ApplicationTemp is required");
    this.applicationTemp = applicationTemp;
  }

  public ApplicationTemp getApplicationTemp() {
    return applicationTemp;
  }

  protected final boolean isHttp2Enabled() {
    return Http2.isEnabled(getHttp2());
  }

  /**
   * Return the {@link SslBundle} that should be used with this server.
   *
   * @return the SSL bundle
   */
  protected final SslBundle getSslBundle() {
    return WebServerSslBundle.get(this.ssl, this.sslBundles);
  }

  protected final Map<String, SslBundle> getServerNameSslBundles() {
    if (ssl != null && !ssl.serverNameBundles.isEmpty()) {
      Assert.state(this.sslBundles != null, "sslBundles is required");
      HashMap<String, SslBundle> ret = new HashMap<>();
      for (var pair : ssl.serverNameBundles) {
        ret.put(pair.getServerName(), sslBundles.getBundle(pair.getBundle()));
      }
      return ret;
    }
    return Collections.emptyMap();
  }

  /**
   * @since 5.0
   */
  protected final void addBundleUpdateHandler(Ssl ssl, BiConsumer<@Nullable String, SslBundle> updateHandler) {
    if (sslBundles != null) {
      addBundleUpdateHandler(sslBundles, null, ssl.bundle, updateHandler);
      for (var pair : ssl.serverNameBundles) {
        addBundleUpdateHandler(sslBundles, pair.getServerName(), pair.getBundle(), updateHandler);
      }
    }
  }

  private void addBundleUpdateHandler(SslBundles sslBundles, @Nullable String serverName,
          @Nullable String bundleName, BiConsumer<@Nullable String, SslBundle> updateHandler) {
    if (StringUtils.hasText(bundleName)) {
      sslBundles.addBundleUpdateHandler(bundleName, sslBundle ->
              updateHandler.accept(serverName, sslBundle));
    }
  }

}
