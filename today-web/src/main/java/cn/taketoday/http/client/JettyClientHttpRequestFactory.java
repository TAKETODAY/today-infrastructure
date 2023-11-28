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

package cn.taketoday.http.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;

/**
 * {@link ClientHttpRequestFactory} implementation based on Jetty's {@link HttpClient}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.eclipse.org/jetty/documentation/jetty-11/programming-guide/index.html#pg-client-http">Jetty HttpClient</a>
 * @since 4.0
 */
public class JettyClientHttpRequestFactory implements ClientHttpRequestFactory, InitializingBean, DisposableBean {

  private final HttpClient httpClient;

  private final boolean defaultClient;

  private long readTimeout = 10 * 1000;

  /**
   * Default constructor that creates a new instance of {@link HttpClient}.
   */
  public JettyClientHttpRequestFactory() {
    this(new HttpClient(), true);
  }

  /**
   * Constructor that takes a customized {@code HttpClient} instance.
   *
   * @param httpClient the
   */
  public JettyClientHttpRequestFactory(HttpClient httpClient) {
    this(httpClient, false);
  }

  private JettyClientHttpRequestFactory(HttpClient httpClient, boolean defaultClient) {
    this.httpClient = httpClient;
    this.defaultClient = defaultClient;
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   * <p>Default is 5 seconds.
   */
  public void setConnectTimeout(int connectTimeout) {
    Assert.isTrue(connectTimeout >= 0, "Timeout must be a non-negative value");
    this.httpClient.setConnectTimeout(connectTimeout);
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   * <p>Default is 5 seconds.
   */
  public void setConnectTimeout(Duration connectTimeout) {
    Assert.notNull(connectTimeout, "ConnectTimeout is required");
    this.httpClient.setConnectTimeout(connectTimeout.toMillis());
  }

  /**
   * Set the underlying read timeout in milliseconds.
   * <p>Default is 10 seconds.
   */
  public void setReadTimeout(long readTimeout) {
    Assert.isTrue(readTimeout > 0, "Timeout must be a positive value");
    this.readTimeout = readTimeout;
  }

  /**
   * Set the underlying read timeout as {@code Duration}.
   * <p>Default is 10 seconds.
   */
  public void setReadTimeout(Duration readTimeout) {
    Assert.notNull(readTimeout, "ReadTimeout is required");
    this.readTimeout = readTimeout.toMillis();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    startHttpClient();
  }

  private void startHttpClient() throws IOException {
    if (!this.httpClient.isStarted()) {
      try {
        this.httpClient.start();
      }
      catch (Exception ex) {
        throw new IOException("Could not start HttpClient: " + ex.getMessage(), ex);
      }
    }
  }

  @Override
  public void destroy() throws Exception {
    if (this.defaultClient) {
      if (!this.httpClient.isStopped()) {
        this.httpClient.stop();
      }
    }
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    startHttpClient();

    Request request = this.httpClient.newRequest(uri).method(httpMethod.name());
    return new JettyClientHttpRequest(request, this.readTimeout);
  }
}
