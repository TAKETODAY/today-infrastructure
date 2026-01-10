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

import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import infra.http.client.JdkClientHttpRequestFactory;
import infra.lang.Assert;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#jdk()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Sangmin Park
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class JdkClientHttpRequestFactoryBuilder extends AbstractClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> {

  private final JdkHttpClientBuilder httpClientBuilder;

  JdkClientHttpRequestFactoryBuilder() {
    this(null, new JdkHttpClientBuilder());
  }

  private JdkClientHttpRequestFactoryBuilder(@Nullable List<Consumer<JdkClientHttpRequestFactory>> customizers,
          JdkHttpClientBuilder httpClientBuilder) {
    super(customizers);
    this.httpClientBuilder = httpClientBuilder;
  }

  @Override
  public JdkClientHttpRequestFactoryBuilder withCustomizer(Consumer<JdkClientHttpRequestFactory> customizer) {
    return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
  }

  @Override
  public JdkClientHttpRequestFactoryBuilder withCustomizers(Collection<Consumer<JdkClientHttpRequestFactory>> customizers) {
    return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
  }

  /**
   * Return a new {@link JdkClientHttpRequestFactoryBuilder} that uses the given
   * executor with the underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param executor the executor to use
   * @return a new {@link JdkClientHttpRequestFactoryBuilder} instance
   */
  public JdkClientHttpRequestFactoryBuilder withExecutor(Executor executor) {
    return new JdkClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientBuilder.withExecutor(executor));
  }

  /**
   * Return a new {@link JdkClientHttpRequestFactoryBuilder} that applies additional
   * customization to the underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param httpClientCustomizer the customizer to apply
   * @return a new {@link JdkClientHttpRequestFactoryBuilder} instance
   */
  public JdkClientHttpRequestFactoryBuilder withHttpClientCustomizer(Consumer<HttpClient.Builder> httpClientCustomizer) {
    Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' is required");
    return new JdkClientHttpRequestFactoryBuilder(getCustomizers(),
            this.httpClientBuilder.withCustomizer(httpClientCustomizer));
  }

  /**
   * Return a new {@link JdkClientHttpRequestFactoryBuilder} that applies the given
   * customizer. This can be useful for applying pre-packaged customizations.
   *
   * @param customizer the customizer to apply
   * @return a new {@link JdkClientHttpRequestFactoryBuilder}
   */
  public JdkClientHttpRequestFactoryBuilder with(UnaryOperator<JdkClientHttpRequestFactoryBuilder> customizer) {
    return customizer.apply(this);
  }

  @Override
  protected JdkClientHttpRequestFactory createClientHttpRequestFactory(HttpClientSettings settings) {
    HttpClient httpClient = this.httpClientBuilder.build(settings.withReadTimeout(null));
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

    if (settings.readTimeout() != null) {
      requestFactory.setReadTimeout(settings.readTimeout());
    }
    return requestFactory;
  }

  static class Classes {

    static final String HTTP_CLIENT = "java.net.http.HttpClient";

  }

}
