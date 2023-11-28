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
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpRequestFactory} implementation that uses standard JDK facilities.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpURLConnection
 * @see HttpComponentsClientHttpRequestFactory
 * @since 4.0
 */
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory {

  private static final int DEFAULT_CHUNK_SIZE = 4096;

  @Nullable
  private Proxy proxy;

  private int chunkSize = DEFAULT_CHUNK_SIZE;

  private int connectTimeout = -1;

  private int readTimeout = -1;

  /**
   * Set the {@link Proxy} to use for this request factory.
   */
  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  /**
   * Set the number of bytes to write in each chunk when not buffering request
   * bodies locally.
   * <p>Note that this parameter is only used when bufferRequestBody is {@code false},
   * and the {@link cn.taketoday.http.HttpHeaders#getContentLength() Content-Length}
   * is not known in advance.
   */
  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  /**
   * Set the underlying URLConnection's connect timeout (in milliseconds).
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Default is the system's default timeout.
   *
   * @see URLConnection#setConnectTimeout(int)
   */
  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  /**
   * Set the underlying URLConnection's connect timeout as {@code Duration}.
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Default is the system's default timeout.
   *
   * @see URLConnection#setConnectTimeout(int)
   */
  public void setConnectTimeout(Duration connectTimeout) {
    Assert.notNull(connectTimeout, "ConnectTimeout is required");
    this.connectTimeout = (int) connectTimeout.toMillis();
  }

  /**
   * Set the underlying URLConnection's read timeout (in milliseconds).
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Default is the system's default timeout.
   *
   * @see URLConnection#setReadTimeout(int)
   */
  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

  /**
   * Set the underlying URLConnection's read timeout (in milliseconds).
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Default is the system's default timeout.
   *
   * @see URLConnection#setReadTimeout(int)
   */
  public void setReadTimeout(Duration readTimeout) {
    Assert.notNull(readTimeout, "ReadTimeout is required");
    this.readTimeout = (int) readTimeout.toMillis();
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
    prepareConnection(connection, httpMethod);

    return new SimpleClientHttpRequest(connection, this.chunkSize);
  }

  /**
   * Opens and returns a connection to the given URL.
   * <p>The default implementation uses the given {@linkplain #setProxy(java.net.Proxy) proxy} -
   * if any - to open a connection.
   *
   * @param url the URL to open a connection to
   * @param proxy the proxy to use, may be {@code null}
   * @return the opened connection
   * @throws IOException in case of I/O errors
   */
  protected HttpURLConnection openConnection(URL url, @Nullable Proxy proxy) throws IOException {
    URLConnection urlConnection = (proxy != null ? url.openConnection(proxy) : url.openConnection());
    if (!(urlConnection instanceof HttpURLConnection httpUrlConnection)) {
      throw new IllegalStateException(
              "HttpURLConnection required for [" + url + "] but got: " + urlConnection);
    }
    return httpUrlConnection;
  }

  /**
   * Template method for preparing the given {@link HttpURLConnection}.
   * <p>The default implementation prepares the connection for input and output, and sets the HTTP method.
   *
   * @param connection the connection to prepare
   * @param httpMethod the HTTP request method ({@code GET}, {@code POST}, etc.)
   * @throws IOException in case of I/O errors
   */
  protected void prepareConnection(HttpURLConnection connection, HttpMethod httpMethod) throws IOException {
    if (this.connectTimeout >= 0) {
      connection.setConnectTimeout(this.connectTimeout);
    }
    if (this.readTimeout >= 0) {
      connection.setReadTimeout(this.readTimeout);
    }

    boolean mayWrite =
            HttpMethod.POST == httpMethod
                    || HttpMethod.PUT == httpMethod
                    || HttpMethod.PATCH == httpMethod
                    || HttpMethod.DELETE == httpMethod;

    connection.setDoInput(true);
    connection.setInstanceFollowRedirects(HttpMethod.GET == httpMethod);
    connection.setDoOutput(mayWrite);
    connection.setRequestMethod(httpMethod.name());
  }

}
