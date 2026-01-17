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

package infra.web.client.config;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.annotation.Order;
import infra.core.io.ResourceLoader;
import infra.app.ssl.SslBundles;
import infra.http.client.HttpClientSettings;
import infra.http.client.config.reactive.ReactiveHttpClientAutoConfiguration;
import infra.http.client.reactive.ClientHttpConnectorBuilder;
import infra.http.codec.CodecCustomizer;
import infra.http.codec.CodecsAutoConfiguration;
import infra.http.reactive.client.ClientHttpConnector;
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.web.client.WebClientCustomizer;
import infra.web.reactive.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebClient}.
 * <p>
 * This will produce a
 * {@link infra.web.reactive.client.WebClient.Builder WebClient.Builder}
 * bean with the {@code prototype} scope, meaning each injection point
 * will receive a newly cloned instance of the builder.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Lazy
@ConditionalOnClass(WebClient.class)
@DisableDIAutoConfiguration(after = { ReactiveHttpClientAutoConfiguration.class, CodecsAutoConfiguration.class })
public final class WebClientAutoConfiguration {

  @Prototype
  @ConditionalOnMissingBean
  static WebClient.Builder webClientBuilder(List<WebClientCustomizer> customizerProvider) {
    WebClient.Builder builder = WebClient.builder();
    for (WebClientCustomizer customizer : customizerProvider) {
      customizer.customize(builder);
    }
    return builder;
  }

  @Component
  @Lazy
  @Order(0)
  @ConditionalOnBean(ClientHttpConnector.class)
  static WebClientCustomizer webClientHttpConnectorCustomizer(ClientHttpConnector connector) {
    return builder -> builder.clientConnector(connector);
  }

  @Component
  @ConditionalOnMissingBean(WebClientSsl.class)
  @ConditionalOnBean(SslBundles.class)
  static AutoConfiguredWebClientSsl webClientSsl(ResourceLoader resourceLoader,
          @Nullable ClientHttpConnectorBuilder<?> clientHttpConnectorBuilder,
          @Nullable HttpClientSettings httpClientSettings, SslBundles sslBundles) {

    if (clientHttpConnectorBuilder == null) {
      clientHttpConnectorBuilder = ClientHttpConnectorBuilder.detect(resourceLoader.getClassLoader());
    }

    if (httpClientSettings == null) {
      httpClientSettings = HttpClientSettings.defaults();
    }

    return new AutoConfiguredWebClientSsl(clientHttpConnectorBuilder, httpClientSettings, sslBundles);
  }

  @DisableAllDependencyInjection
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(CodecCustomizer.class)
  protected static class WebClientCodecsConfiguration {

    @Component
    @ConditionalOnMissingBean
    @Order(0)
    static WebClientCodecCustomizer exchangeStrategiesCustomizer(ObjectProvider<CodecCustomizer> codecCustomizers) {
      return new WebClientCodecCustomizer(codecCustomizers.orderedStream().toList());
    }

  }

}
