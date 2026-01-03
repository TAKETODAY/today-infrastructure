/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

import infra.core.ssl.SslBundle;
import infra.lang.Assert;

import static infra.http.client.config.HttpComponentsHttpClientBuilder.createConnectionConfig;

/**
 * Builder that can be used to create a
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link HttpAsyncClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 5.0
 */
public final class HttpComponentsHttpAsyncClientBuilder {

  private final Consumer<HttpAsyncClientBuilder> customizer;

  private final Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer;

  private final Consumer<ConnectionConfig.Builder> connectionConfigCustomizer;

  private final Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer;

  private final Function<@Nullable SslBundle, @Nullable TlsStrategy> tlsStrategyFactory;

  public HttpComponentsHttpAsyncClientBuilder() {
    this(Empty.consumer(), Empty.consumer(), Empty.consumer(), Empty.consumer(),
            HttpComponentsSslBundleTlsStrategy::get);
  }

  private HttpComponentsHttpAsyncClientBuilder(Consumer<HttpAsyncClientBuilder> customizer,
          Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer,
          Consumer<ConnectionConfig.Builder> connectionConfigCustomizer,
          Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer,
          Function<@Nullable SslBundle, @Nullable TlsStrategy> tlsStrategyFactory) {
    this.customizer = customizer;
    this.connectionManagerCustomizer = connectionManagerCustomizer;
    this.connectionConfigCustomizer = connectionConfigCustomizer;
    this.defaultRequestConfigCustomizer = defaultRequestConfigCustomizer;
    this.tlsStrategyFactory = tlsStrategyFactory;
  }

  /**
   * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
   * customization to the underlying {@link HttpAsyncClientBuilder}.
   *
   * @param customizer the customizer to apply
   * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
   */
  public HttpComponentsHttpAsyncClientBuilder withCustomizer(Consumer<HttpAsyncClientBuilder> customizer) {
    Assert.notNull(customizer, "'customizer' is required");
    return new HttpComponentsHttpAsyncClientBuilder(this.customizer.andThen(customizer),
            this.connectionManagerCustomizer, this.connectionConfigCustomizer, this.defaultRequestConfigCustomizer,
            this.tlsStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
   * customization to the underlying {@link PoolingAsyncClientConnectionManagerBuilder}.
   *
   * @param connectionManagerCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
   */
  public HttpComponentsHttpAsyncClientBuilder withConnectionManagerCustomizer(
          Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer) {
    Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' is required");
    return new HttpComponentsHttpAsyncClientBuilder(this.customizer,
            this.connectionManagerCustomizer.andThen(connectionManagerCustomizer), this.connectionConfigCustomizer,
            this.defaultRequestConfigCustomizer, this.tlsStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
   * customization to the underlying
   * {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
   *
   * @param connectionConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
   */
  public HttpComponentsHttpAsyncClientBuilder withConnectionConfigCustomizer(
          Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
    Assert.notNull(connectionConfigCustomizer, "'connectionConfigCustomizer' is required");
    return new HttpComponentsHttpAsyncClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.connectionConfigCustomizer.andThen(connectionConfigCustomizer),
            this.defaultRequestConfigCustomizer, this.tlsStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpAsyncClientBuilder} with a replacement
   * {@link TlsStrategy} factory.
   *
   * @param tlsStrategyFactory the new factory used to create a {@link TlsStrategy} for
   * a given {@link SslBundle}
   * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
   */
  public HttpComponentsHttpAsyncClientBuilder withTlsStrategyFactory(
          Function<@Nullable SslBundle, @Nullable TlsStrategy> tlsStrategyFactory) {
    Assert.notNull(tlsStrategyFactory, "'tlsStrategyFactory' is required");
    return new HttpComponentsHttpAsyncClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.connectionConfigCustomizer, this.defaultRequestConfigCustomizer, tlsStrategyFactory);
  }

  /**
   * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
   * customization to the underlying
   * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
   * requests.
   *
   * @param defaultRequestConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
   */
  public HttpComponentsHttpAsyncClientBuilder withDefaultRequestConfigCustomizer(
          Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
    Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' is required");
    return new HttpComponentsHttpAsyncClientBuilder(this.customizer, this.connectionManagerCustomizer,
            this.connectionConfigCustomizer,
            this.defaultRequestConfigCustomizer.andThen(defaultRequestConfigCustomizer), this.tlsStrategyFactory);
  }

  /**
   * Build a new {@link HttpAsyncClient} instance with the given settings applied.
   *
   * @param settings the settings to apply
   * @return a new {@link CloseableHttpAsyncClient} instance
   */
  public CloseableHttpAsyncClient build(@Nullable HttpClientSettings settings) {
    settings = (settings != null) ? settings : HttpClientSettings.defaults();
    HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create()
            .useSystemProperties()
            .setRedirectStrategy(HttpComponentsRedirectStrategy.get(settings.redirects()))
            .setConnectionManager(createConnectionManager(settings))
            .setDefaultRequestConfig(createDefaultRequestConfig());
    this.customizer.accept(builder);
    return builder.build();
  }

  private PoolingAsyncClientConnectionManager createConnectionManager(HttpClientSettings settings) {
    var builder = PoolingAsyncClientConnectionManagerBuilder.create()
            .useSystemProperties();

    if (settings.sslBundle() != null) {
      TlsStrategy strategy = tlsStrategyFactory.apply(settings.sslBundle());
      if (strategy != null) {
        builder.setTlsStrategy(strategy);
      }
    }

    builder.setDefaultConnectionConfig(createConnectionConfig(settings, connectionConfigCustomizer));
    this.connectionManagerCustomizer.accept(builder);
    return builder.build();
  }

  private RequestConfig createDefaultRequestConfig() {
    RequestConfig.Builder builder = RequestConfig.custom();
    this.defaultRequestConfigCustomizer.accept(builder);
    return builder.build();
  }

}
