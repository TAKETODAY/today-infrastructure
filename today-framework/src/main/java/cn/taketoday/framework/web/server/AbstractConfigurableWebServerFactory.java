/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.server;

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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

  @Nullable
  private InetAddress address;

  private Set<ErrorPage> errorPages = new LinkedHashSet<>();

  @Nullable
  private Ssl ssl;

  private SslBundles sslBundles;

  @Nullable
  private Http2 http2;

  @Nullable
  private Compression compression;

  @Nullable
  private String serverHeader;

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
  @Nullable
  public InetAddress getAddress() {
    return this.address;
  }

  @Override
  public void setAddress(@Nullable InetAddress address) {
    this.address = address;
  }

  /**
   * Returns a mutable set of {@link ErrorPage ErrorPages} that will be used when
   * handling exceptions.
   *
   * @return the error pages
   */
  public Set<ErrorPage> getErrorPages() {
    return this.errorPages;
  }

  @Override
  public void setErrorPages(Set<? extends ErrorPage> errorPages) {
    Assert.notNull(errorPages, "ErrorPages must not be null");
    this.errorPages = new LinkedHashSet<>(errorPages);
  }

  @Override
  public void addErrorPages(ErrorPage... errorPages) {
    Assert.notNull(errorPages, "ErrorPages must not be null");
    this.errorPages.addAll(Arrays.asList(errorPages));
  }

  @Nullable
  public Ssl getSsl() {
    return this.ssl;
  }

  @Override
  public void setSsl(@Nullable Ssl ssl) {
    this.ssl = ssl;
  }

  @Override
  public void setSslBundles(SslBundles sslBundles) {
    this.sslBundles = sslBundles;
  }

  @Nullable
  public Http2 getHttp2() {
    return this.http2;
  }

  @Override
  public void setHttp2(@Nullable Http2 http2) {
    this.http2 = http2;
  }

  @Nullable
  public Compression getCompression() {
    return this.compression;
  }

  @Override
  public void setCompression(@Nullable Compression compression) {
    this.compression = compression;
  }

  @Nullable
  public String getServerHeader() {
    return this.serverHeader;
  }

  @Override
  public void setServerHeader(@Nullable String serverHeader) {
    this.serverHeader = serverHeader;
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

  /**
   * Return the {@link SslBundle} that should be used with this server.
   *
   * @return the SSL bundle
   */
  protected final SslBundle getSslBundle() {
    return WebServerSslBundle.get(this.ssl, this.sslBundles);
  }

  /**
   * Return the absolute temp dir for given web server.
   *
   * @param prefix server name
   * @return the temp dir for given server.
   */
  protected final File createTempDir(String prefix) {
    File tempDir = applicationTemp.getDir(prefix + "-" + getPort());
    tempDir.deleteOnExit();
    return tempDir;
  }

}
