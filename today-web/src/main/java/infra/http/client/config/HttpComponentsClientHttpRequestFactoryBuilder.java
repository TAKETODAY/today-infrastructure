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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import infra.core.ssl.SslBundle;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.config.HttpComponentsHttpClientBuilder.TlsSocketStrategyFactory;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#httpComponents()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class HttpComponentsClientHttpRequestFactoryBuilder extends AbstractClientHttpRequestFactoryBuilder<HttpComponentsClientHttpRequestFactory> {

  private final HttpComponentsHttpClientBuilder httpClientBuilder;

  HttpComponentsClientHttpRequestFactoryBuilder() {
    this(null, new HttpComponentsHttpClientBuilder());
  }

  private HttpComponentsClientHttpRequestFactoryBuilder(@Nullable List<Consumer<HttpComponentsClientHttpRequestFactory>> customizers,
          HttpComponentsHttpClientBuilder httpClientBuilder) {
    super(customizers);
    this.httpClientBuilder = httpClientBuilder;
  }

  @Override
  public HttpComponentsClientHttpRequestFactoryBuilder withCustomizer(Consumer<HttpComponentsClientHttpRequestFactory> customizer) {
    return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
  }

  @Override
  public HttpComponentsClientHttpRequestFactoryBuilder withCustomizers(Collection<Consumer<HttpComponentsClientHttpRequestFactory>> customizers) {
    return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizers),
            this.httpClientBuilder);
  }

  /**
   * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
   * additional customization to the underlying {@link HttpClientBuilder}.
   *
   * @param httpClientCustomizer the customizer to apply
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
   */
  public HttpComponentsClientHttpRequestFactoryBuilder withHttpClientCustomizer(Consumer<HttpClientBuilder> httpClientCustomizer) {
    Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' is required");
    return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withCustomizer(httpClientCustomizer));
  }

  /**
   * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
   * additional customization to the underlying
   * {@link PoolingHttpClientConnectionManagerBuilder}.
   *
   * @param connectionManagerCustomizer the customizer to apply
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
   */
  public HttpComponentsClientHttpRequestFactoryBuilder withConnectionManagerCustomizer(Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer) {
    Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' is required");
    return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withConnectionManagerCustomizer(connectionManagerCustomizer));
  }

  /**
   * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
   * additional customization to the underlying
   * {@link org.apache.hc.core5.http.io.SocketConfig.Builder}.
   *
   * @param socketConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
   */
  public HttpComponentsClientHttpRequestFactoryBuilder withSocketConfigCustomizer(Consumer<SocketConfig.Builder> socketConfigCustomizer) {
    Assert.notNull(socketConfigCustomizer, "'socketConfigCustomizer' is required");
    return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withSocketConfigCustomizer(socketConfigCustomizer));
  }

  /**
   * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
   * customization to the underlying
   * {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
   *
   * @param connectionConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsHttpClientBuilder} instance
   */
  public HttpComponentsClientHttpRequestFactoryBuilder withConnectionConfigCustomizer(Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
    Assert.notNull(connectionConfigCustomizer, "'connectionConfigCustomizer' is required");
    return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withConnectionConfigCustomizer(connectionConfigCustomizer));
  }

  /**
   * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} with a
   * replacement {@link TlsSocketStrategy} factory.
   *
   * @param tlsSocketStrategyFactory the new factory used to create a
   * {@link TlsSocketStrategy} for a given {@link SslBundle}
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
   */
  public HttpComponentsClientHttpRequestFactoryBuilder withTlsSocketStrategyFactory(TlsSocketStrategyFactory tlsSocketStrategyFactory) {
    Assert.notNull(tlsSocketStrategyFactory, "'tlsSocketStrategyFactory' is required");
    return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withTlsSocketStrategyFactory(tlsSocketStrategyFactory));
  }

  /**
   * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
   * additional customization to the underlying
   * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
   * requests.
   *
   * @param defaultRequestConfigCustomizer the customizer to apply
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
   */
  public HttpComponentsClientHttpRequestFactoryBuilder withDefaultRequestConfigCustomizer(Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
    Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' is required");
    return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer));
  }

  /**
   * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies the
   * given customizer. This can be useful for applying pre-packaged customizations.
   *
   * @param customizer the customizer to apply
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder}
   */
  public HttpComponentsClientHttpRequestFactoryBuilder with(UnaryOperator<HttpComponentsClientHttpRequestFactoryBuilder> customizer) {
    return customizer.apply(this);
  }

  @Override
  protected HttpComponentsClientHttpRequestFactory createClientHttpRequestFactory(HttpClientSettings settings) {
    HttpClient httpClient = this.httpClientBuilder.build(settings);
    return new HttpComponentsClientHttpRequestFactory(httpClient);
  }

  static class Classes {

    static final String HTTP_CLIENTS = "org.apache.hc.client5.http.impl.classic.HttpClients";

    static boolean present(@Nullable ClassLoader classLoader) {
      return ClassUtils.isPresent(HTTP_CLIENTS, classLoader);
    }

  }

}
