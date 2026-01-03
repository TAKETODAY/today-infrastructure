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

package infra.http.client.config.reactive;

import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import infra.http.client.reactive.JdkClientHttpConnector;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.JdkHttpClientBuilder;
import infra.lang.Assert;

/**
 * Builder for {@link ClientHttpConnectorBuilder#jdk()}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class JdkClientHttpConnectorBuilder extends AbstractClientHttpConnectorBuilder<JdkClientHttpConnector> {

  private final JdkHttpClientBuilder httpClientBuilder;

  JdkClientHttpConnectorBuilder() {
    this(null, new JdkHttpClientBuilder());
  }

  private JdkClientHttpConnectorBuilder(@Nullable List<Consumer<JdkClientHttpConnector>> customizers,
          JdkHttpClientBuilder httpClientBuilder) {
    super(customizers);
    this.httpClientBuilder = httpClientBuilder;
  }

  @Override
  public JdkClientHttpConnectorBuilder withCustomizer(Consumer<JdkClientHttpConnector> customizer) {
    return new JdkClientHttpConnectorBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
  }

  @Override
  public JdkClientHttpConnectorBuilder withCustomizers(Collection<Consumer<JdkClientHttpConnector>> customizers) {
    return new JdkClientHttpConnectorBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
  }

  /**
   * Return a new {@link JdkClientHttpConnectorBuilder} that uses the given executor
   * with the underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param executor the executor to use
   * @return a new {@link JdkClientHttpConnectorBuilder} instance
   */
  public JdkClientHttpConnectorBuilder withExecutor(Executor executor) {
    return new JdkClientHttpConnectorBuilder(getCustomizers(), this.httpClientBuilder.withExecutor(executor));
  }

  /**
   * Return a new {@link JdkClientHttpConnectorBuilder} that applies additional
   * customization to the underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param httpClientCustomizer the customizer to apply
   * @return a new {@link JdkClientHttpConnectorBuilder} instance
   */
  public JdkClientHttpConnectorBuilder withHttpClientCustomizer(Consumer<HttpClient.Builder> httpClientCustomizer) {
    Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' is required");
    return new JdkClientHttpConnectorBuilder(getCustomizers(),
            this.httpClientBuilder.withCustomizer(httpClientCustomizer));
  }

  /**
   * Return a new {@link JdkClientHttpConnectorBuilder} that applies the given
   * customizer. This can be useful for applying pre-packaged customizations.
   *
   * @param customizer the customizer to apply
   * @return a new {@link JdkClientHttpConnectorBuilder}
   */
  public JdkClientHttpConnectorBuilder with(UnaryOperator<JdkClientHttpConnectorBuilder> customizer) {
    return customizer.apply(this);
  }

  @Override
  protected JdkClientHttpConnector createClientHttpConnector(HttpClientSettings settings) {
    HttpClient httpClient = this.httpClientBuilder.build(settings.withReadTimeout(null));
    JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
    if (settings.readTimeout() != null) {
      connector.setReadTimeout(settings.readTimeout());
    }
    return connector;
  }

}
