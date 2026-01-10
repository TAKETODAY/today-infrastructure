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

package infra.http.client.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import infra.core.ssl.SslBundle;
import infra.lang.Assert;

/**
 * Builder that can be used to create an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpClient
 * @since 5.0
 */
public final class HttpComponentsHttpClientBuilder {

  private final Consumer<HttpClientBuilder> customizer;

  private final Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer;

  private final Consumer<SocketConfig.Builder> socketConfigCustomizer;

  private final Consumer<ConnectionConfig.Builder> connectionConfigCustomizer;

  private final Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer;

  private final TlsSocketStrategyFactory tlsSocketStrategyFactory;

  public HttpComponentsHttpClientBuilder() {
    this(Empty.consumer(), Empty.consumer(), Empty.consumer(), Empty.consumer(), Empty.consumer(),
            HttpComponentsSslBundleTlsStrategy::get);
  }

  private HttpComponentsHttpClientBuilder(Consumer<HttpClientBuilder> customizer,
          Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer,
          Consumer<SocketConfig.Builder> socketConfigCustomizer,
          Consumer<ConnectionConfig.Builder> connectionConfigCustomizer,
          Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer,
          TlsSocketStrategyFactory tlsSocketStrategyFactory) {
    this.customizer = customizer;
    this.connectionManagerCustomizer = connectionManagerCustomizer;
    this.socketConfigCustomizer = socketConfigCustomizer;
    this.connectionConfigCustomizer = connectionConfigCustomizer;
    this.defaultRequestConfigCustomizer = defaultRequestConfigCustomizer;
    this.tlsSocketStrategyFactory = tlsSocketStrategyFactory;
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
   * customization to the underlying {@link HttpClientBuilder}.
   *
   * @param customizer the customizer to apply
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsHttpClientBuilder withCustomizer(Consumer<HttpClientBuilder> customizer) {
    Assert.notNull(customizer, "'customizer' is required");
    return new HttpComponentsHttpClientBuilder(this.customizer.andThen(customizer),
            this.connectionManagerCustomizer, this.socketConfigCustomizer, this.connectionConfigCustomizer,
            this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
   * customization to the underlying {@link PoolingHttpClientConnectionManagerBuilder}.
   *
   * @param connectionManagerCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsHttpClientBuilder withConnectionManagerCustomizer(
          Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer) {
    Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' is required");
    return new HttpComponentsHttpClientBuilder(this.customizer,
            this.connectionManagerCustomizer.andThen(connectionManagerCustomizer), this.socketConfigCustomizer,
            this.connectionConfigCustomizer, this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
   * customization to the underlying
   * {@link org.apache.hc.core5.http.io.SocketConfig.Builder}.
   *
   * @param socketConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsHttpClientBuilder withSocketConfigCustomizer(
          Consumer<SocketConfig.Builder> socketConfigCustomizer) {
    Assert.notNull(socketConfigCustomizer, "'socketConfigCustomizer' is required");
    return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.socketConfigCustomizer.andThen(socketConfigCustomizer), this.connectionConfigCustomizer,
            this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
   * customization to the underlying
   * {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
   *
   * @param connectionConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsHttpClientBuilder withConnectionConfigCustomizer(Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
    Assert.notNull(connectionConfigCustomizer, "'connectionConfigCustomizer' is required");
    return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.socketConfigCustomizer, this.connectionConfigCustomizer.andThen(connectionConfigCustomizer),
            this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} with a replacement
   * {@link TlsSocketStrategy} factory.
   *
   * @param tlsSocketStrategyFactory the new factory used to create a
   * {@link TlsSocketStrategy}. The function will be provided with a {@link SslBundle}
   * or {@code null} if no bundle is selected. Only non {@code null} results will be
   * applied.
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsHttpClientBuilder withTlsSocketStrategyFactory(TlsSocketStrategyFactory tlsSocketStrategyFactory) {
    Assert.notNull(tlsSocketStrategyFactory, "'tlsSocketStrategyFactory' is required");
    return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.socketConfigCustomizer, this.connectionConfigCustomizer, this.defaultRequestConfigCustomizer,
            tlsSocketStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
   * customization to the underlying
   * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
   * requests.
   *
   * @param defaultRequestConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsHttpClientBuilder withDefaultRequestConfigCustomizer(Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
    Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' is required");
    return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.socketConfigCustomizer, this.connectionConfigCustomizer,
            this.defaultRequestConfigCustomizer.andThen(defaultRequestConfigCustomizer),
            this.tlsSocketStrategyFactory);
  }

  /**
   * Build a new {@link HttpClient} instance with the given settings applied.
   *
   * @param settings the settings to apply
   * @return a new {@link HttpClient} instance
   */
  public CloseableHttpClient build(@Nullable HttpClientSettings settings) {
    settings = settings != null ? settings : HttpClientSettings.defaults();
    HttpClientBuilder builder = HttpClientBuilder.create()
            .useSystemProperties()
            .setRedirectStrategy(HttpComponentsRedirectStrategy.get(settings.redirects()))
            .setConnectionManager(createConnectionManager(settings))
            .setDefaultRequestConfig(createDefaultRequestConfig());
    customizer.accept(builder);
    return builder.build();
  }

  private PoolingHttpClientConnectionManager createConnectionManager(HttpClientSettings settings) {
    PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create().useSystemProperties();
    builder.setDefaultSocketConfig(createSocketConfig());
    builder.setDefaultConnectionConfig(createConnectionConfig(settings, connectionConfigCustomizer));

    TlsSocketStrategy tlsSocketStrategy = tlsSocketStrategyFactory.getTlsSocketStrategy(settings.sslBundle());
    builder.setTlsSocketStrategy(tlsSocketStrategy);

    connectionManagerCustomizer.accept(builder);
    return builder.build();
  }

  private SocketConfig createSocketConfig() {
    SocketConfig.Builder builder = SocketConfig.custom();
    this.socketConfigCustomizer.accept(builder);
    return builder.build();
  }

  static ConnectionConfig createConnectionConfig(HttpClientSettings settings, Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
    ConnectionConfig.Builder builder = ConnectionConfig.custom();
    if (settings.connectTimeout() != null) {
      builder.setConnectTimeout(settings.connectTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    if (settings.readTimeout() != null) {
      builder.setSocketTimeout((int) settings.readTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    connectionConfigCustomizer.accept(builder);
    return builder.build();
  }

  private RequestConfig createDefaultRequestConfig() {
    RequestConfig.Builder builder = RequestConfig.custom();
    defaultRequestConfigCustomizer.accept(builder);
    return builder.build();
  }

  /**
   * Factory that can be used to optionally create a {@link TlsSocketStrategy} given an
   * {@link SslBundle}.
   */
  public interface TlsSocketStrategyFactory {

    /**
     * Return the {@link TlsSocketStrategy} to use for the given bundle.
     *
     * @param sslBundle the SSL bundle or {@code null}
     * @return the {@link TlsSocketStrategy} to use or {@code null}
     */
    @Nullable
    TlsSocketStrategy getTlsSocketStrategy(@Nullable SslBundle sslBundle);

  }

}
