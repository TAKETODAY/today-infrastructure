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
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpRequestFactory} implementation based on the Java
 * {@link HttpClient}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JdkClientHttpRequestFactory implements ClientHttpRequestFactory {

  private final HttpClient httpClient;

  private final Executor executor;

  @Nullable
  private Duration readTimeout;

  /**
   * Create a new instance of the {@code JdkClientHttpRequestFactory}
   * with a default {@link HttpClient}.
   */
  public JdkClientHttpRequestFactory() {
    this(HttpClient.newHttpClient());
  }

  /**
   * Create a new instance of the {@code JdkClientHttpRequestFactory} based on
   * the given {@link HttpClient}.
   *
   * @param httpClient the client to base on
   */
  public JdkClientHttpRequestFactory(HttpClient httpClient) {
    Assert.notNull(httpClient, "HttpClient is required");
    this.httpClient = httpClient;
    this.executor = httpClient.executor().orElseGet(SimpleAsyncTaskExecutor::new);
  }

  /**
   * Create a new instance of the {@code JdkClientHttpRequestFactory} based on
   * the given {@link HttpClient} and {@link Executor}.
   *
   * @param httpClient the client to base on
   * @param executor the executor to use for blocking write operations
   */
  public JdkClientHttpRequestFactory(HttpClient httpClient, Executor executor) {
    Assert.notNull(httpClient, "HttpClient is required");
    Assert.notNull(executor, "Executor must not be null");
    this.httpClient = httpClient;
    this.executor = executor;
  }

  /**
   * Set the underlying {@code HttpClient}'s read timeout (in milliseconds).
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Default is the system's default timeout.
   *
   * @see java.net.http.HttpRequest.Builder#timeout
   */
  public void setReadTimeout(int readTimeout) {
    this.readTimeout = Duration.ofMillis(readTimeout);
  }

  /**
   * Set the underlying {@code HttpClient}'s read timeout as a
   * {@code Duration}.
   * <p>Default is the system's default timeout.
   *
   * @see java.net.http.HttpRequest.Builder#timeout
   */
  public void setReadTimeout(Duration readTimeout) {
    Assert.notNull(readTimeout, "ReadTimeout is required");
    this.readTimeout = readTimeout;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    return new JdkClientHttpRequest(this.httpClient, uri, httpMethod, this.executor, this.readTimeout);
  }

}
