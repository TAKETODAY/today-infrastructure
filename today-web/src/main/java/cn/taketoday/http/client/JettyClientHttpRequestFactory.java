/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import org.eclipse.jetty.client.api.Request;

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

  private Duration timeOut = Duration.ofSeconds(1);

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
   * Sets the maximum time to wait until all headers have been received.
   * The default value is 1 second.
   */
  public void setTimeOut(Duration timeOut) {
    Assert.notNull(timeOut, "TimeOut must not be null");
    Assert.isTrue(!timeOut.isNegative(), "TimeOut must not be negative");
    this.timeOut = timeOut;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    startHttpClient();
  }

  private void startHttpClient() throws Exception {
    if (!this.httpClient.isStarted()) {
      this.httpClient.start();
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
    try {
      startHttpClient();
    }
    catch (Exception ex) {
      throw new IOException("Could not start HttpClient: " + ex.getMessage(), ex);
    }

    Request request = this.httpClient.newRequest(uri).method(httpMethod.name());
    return new JettyClientHttpRequest(request, this.timeOut);
  }
}
