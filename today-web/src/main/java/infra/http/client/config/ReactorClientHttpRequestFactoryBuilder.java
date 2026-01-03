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

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import infra.http.client.ReactorClientHttpRequestFactory;
import infra.http.client.ReactorResourceFactory;
import infra.lang.Assert;
import infra.util.ClassUtils;
import reactor.netty.http.client.HttpClient;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#reactor()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class ReactorClientHttpRequestFactoryBuilder extends AbstractClientHttpRequestFactoryBuilder<ReactorClientHttpRequestFactory> {

  private final ReactorHttpClientBuilder httpClientBuilder;

  ReactorClientHttpRequestFactoryBuilder() {
    this(null, new ReactorHttpClientBuilder());
  }

  private ReactorClientHttpRequestFactoryBuilder(@Nullable List<Consumer<ReactorClientHttpRequestFactory>> customizers, ReactorHttpClientBuilder httpClientBuilder) {
    super(customizers);
    this.httpClientBuilder = httpClientBuilder;
  }

  @Override
  public ReactorClientHttpRequestFactoryBuilder withCustomizer(Consumer<ReactorClientHttpRequestFactory> customizer) {
    return new ReactorClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
  }

  @Override
  public ReactorClientHttpRequestFactoryBuilder withCustomizers(Collection<Consumer<ReactorClientHttpRequestFactory>> customizers) {
    return new ReactorClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
  }

  /**
   * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that uses the given
   * {@link ReactorResourceFactory} to create the underlying {@link HttpClient}.
   *
   * @param reactorResourceFactory the {@link ReactorResourceFactory} to use
   * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
   */
  public ReactorClientHttpRequestFactoryBuilder withReactorResourceFactory(ReactorResourceFactory reactorResourceFactory) {
    Assert.notNull(reactorResourceFactory, "'reactorResourceFactory' is required");
    return new ReactorClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withReactorResourceFactory(reactorResourceFactory));
  }

  /**
   * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that uses the given
   * factory to create the underlying {@link HttpClient}.
   *
   * @param factory the factory to use
   * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
   */
  public ReactorClientHttpRequestFactoryBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
    Assert.notNull(factory, "'factory' is required");
    return new ReactorClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withHttpClientFactory(factory));
  }

  /**
   * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that applies additional
   * customization to the underlying {@link HttpClient}.
   *
   * @param operator the customizer to apply
   * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
   */
  public ReactorClientHttpRequestFactoryBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> operator) {
    Assert.notNull(operator, "'httpClientCustomizer' is required");
    return new ReactorClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withHttpClientCustomizer(operator));
  }

  /**
   * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that applies the given
   * customizer. This can be useful for applying pre-packaged customizations.
   *
   * @param customizer the customizer to apply
   * @return a new {@link ReactorClientHttpRequestFactoryBuilder}
   */
  public ReactorClientHttpRequestFactoryBuilder with(UnaryOperator<ReactorClientHttpRequestFactoryBuilder> customizer) {
    return customizer.apply(this);
  }

  @Override
  protected ReactorClientHttpRequestFactory createClientHttpRequestFactory(HttpClientSettings settings) {
    HttpClient httpClient = httpClientBuilder.build(settings.withTimeouts(null, null));
    ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);

    if (settings.connectTimeout() != null) {
      requestFactory.setConnectTimeout(Math.toIntExact(settings.connectTimeout().toMillis()));
    }

    if (settings.readTimeout() != null) {
      requestFactory.setReadTimeout(settings.readTimeout().toMillis());
    }

    return requestFactory;
  }

  static class Classes {

    static final String HTTP_CLIENT = "reactor.netty.http.client.HttpClient";

    static boolean present(@Nullable ClassLoader classLoader) {
      return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
    }

  }

}
