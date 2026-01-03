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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import infra.http.client.ReactorResourceFactory;
import infra.http.client.reactive.ReactorClientHttpConnector;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.ReactorHttpClientBuilder;
import infra.lang.Assert;
import infra.util.ClassUtils;
import reactor.netty.http.client.HttpClient;

/**
 * Builder for {@link ClientHttpConnectorBuilder#reactor()}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class ReactorClientHttpConnectorBuilder
        extends AbstractClientHttpConnectorBuilder<ReactorClientHttpConnector> {

  private final ReactorHttpClientBuilder httpClientBuilder;

  ReactorClientHttpConnectorBuilder() {
    this(null, new ReactorHttpClientBuilder());
  }

  private ReactorClientHttpConnectorBuilder(@Nullable List<Consumer<ReactorClientHttpConnector>> customizers,
          ReactorHttpClientBuilder httpClientBuilder) {
    super(customizers);
    this.httpClientBuilder = httpClientBuilder;
  }

  @Override
  public ReactorClientHttpConnectorBuilder withCustomizer(Consumer<ReactorClientHttpConnector> customizer) {
    return new ReactorClientHttpConnectorBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
  }

  @Override
  public ReactorClientHttpConnectorBuilder withCustomizers(
          Collection<Consumer<ReactorClientHttpConnector>> customizers) {
    return new ReactorClientHttpConnectorBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
  }

  /**
   * Return a new {@link ReactorClientHttpConnectorBuilder} that uses the given
   * {@link ReactorResourceFactory} to create the underlying {@link HttpClient}.
   *
   * @param reactorResourceFactory the {@link ReactorResourceFactory} to use
   * @return a new {@link ReactorClientHttpConnectorBuilder} instance
   */
  public ReactorClientHttpConnectorBuilder withReactorResourceFactory(ReactorResourceFactory reactorResourceFactory) {
    Assert.notNull(reactorResourceFactory, "'reactorResourceFactory' is required");
    return new ReactorClientHttpConnectorBuilder(getCustomizers(),
            this.httpClientBuilder.withReactorResourceFactory(reactorResourceFactory));
  }

  /**
   * Return a new {@link ReactorClientHttpConnectorBuilder} that uses the given factory
   * to create the underlying {@link HttpClient}.
   *
   * @param factory the factory to use
   * @return a new {@link ReactorClientHttpConnectorBuilder} instance
   */
  public ReactorClientHttpConnectorBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
    Assert.notNull(factory, "'factory' is required");
    return new ReactorClientHttpConnectorBuilder(getCustomizers(),
            this.httpClientBuilder.withHttpClientFactory(factory));
  }

  /**
   * Return a new {@link ReactorClientHttpConnectorBuilder} that applies additional
   * customization to the underlying {@link HttpClient}.
   *
   * @param httpClientCustomizer the customizer to apply
   * @return a new {@link ReactorClientHttpConnectorBuilder} instance
   */
  public ReactorClientHttpConnectorBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> httpClientCustomizer) {
    Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' is required");
    return new ReactorClientHttpConnectorBuilder(getCustomizers(),
            this.httpClientBuilder.withHttpClientCustomizer(httpClientCustomizer));
  }

  /**
   * Return a new {@link ReactorClientHttpConnectorBuilder} that applies the given
   * customizer. This can be useful for applying pre-packaged customizations.
   *
   * @param customizer the customizer to apply
   * @return a new {@link ReactorClientHttpConnectorBuilder}
   */
  public ReactorClientHttpConnectorBuilder with(UnaryOperator<ReactorClientHttpConnectorBuilder> customizer) {
    return customizer.apply(this);
  }

  @Override
  protected ReactorClientHttpConnector createClientHttpConnector(HttpClientSettings settings) {
    HttpClient httpClient = this.httpClientBuilder.build(settings);
    return new ReactorClientHttpConnector(httpClient);
  }

  static class Classes {

    static final String HTTP_CLIENT = "reactor.netty.http.client.HttpClient";

    static boolean present(@Nullable ClassLoader classLoader) {
      return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
    }

  }

}
