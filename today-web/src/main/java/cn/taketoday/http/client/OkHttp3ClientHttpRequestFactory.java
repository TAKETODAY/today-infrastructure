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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * {@link ClientHttpRequestFactory} implementation that uses
 * <a href="https://square.github.io/okhttp/">OkHttp</a> 3.x to create requests.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OkHttp3ClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

  private OkHttpClient client;

  private final boolean defaultClient;

  /**
   * Create a factory with a default {@link OkHttpClient} instance.
   */
  public OkHttp3ClientHttpRequestFactory() {
    this.client = new OkHttpClient();
    this.defaultClient = true;
  }

  /**
   * Create a factory with the given {@link OkHttpClient} instance.
   *
   * @param client the client to use
   */
  public OkHttp3ClientHttpRequestFactory(OkHttpClient client) {
    Assert.notNull(client, "OkHttpClient must not be null");
    this.client = client;
    this.defaultClient = false;
  }

  /**
   * Set the underlying read timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setReadTimeout(int readTimeout) {
    this.client = this.client.newBuilder()
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Set the underlying read timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setReadTimeout(Duration readTimeout) {
    this.client = this.client.newBuilder()
            .readTimeout(readTimeout)
            .build();
  }

  /**
   * Set the underlying write timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setWriteTimeout(int writeTimeout) {
    this.client = this.client.newBuilder()
            .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Set the underlying write timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setWriteTimeout(Duration writeTimeout) {
    this.client = this.client.newBuilder()
            .writeTimeout(writeTimeout)
            .build();
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setConnectTimeout(int connectTimeout) {
    this.client = this.client.newBuilder()
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   */
  public void setConnectTimeout(Duration connectTimeout) {
    this.client = this.client.newBuilder()
            .connectTimeout(connectTimeout)
            .build();
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
    return new OkHttp3ClientHttpRequest(this.client, uri, httpMethod);
  }

  @Override
  public void destroy() throws IOException {
    if (this.defaultClient) {
      // Clean up the client if we created it in the constructor
      Cache cache = this.client.cache();
      if (cache != null) {
        cache.close();
      }
      this.client.dispatcher().executorService().shutdown();
      this.client.connectionPool().evictAll();
    }
  }

}
