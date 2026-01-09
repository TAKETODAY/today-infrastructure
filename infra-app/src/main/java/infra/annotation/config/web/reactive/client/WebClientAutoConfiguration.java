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

package infra.annotation.config.web.reactive.client;

import java.util.List;

import infra.annotation.config.http.CodecsAutoConfiguration;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.annotation.Order;
import infra.core.ssl.SslBundles;
import infra.http.codec.CodecCustomizer;
import infra.stereotype.Component;
import infra.stereotype.Prototype;
import infra.web.client.reactive.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebClient}.
 * <p>
 * This will produce a
 * {@link infra.web.client.reactive.WebClient.Builder
 * WebClient.Builder} bean with the {@code prototype} scope, meaning each injection point
 * will receive a newly cloned instance of the builder.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration(after = { CodecsAutoConfiguration.class, ClientHttpConnectorAutoConfiguration.class })
@ConditionalOnClass({ WebClient.class, Mono.class })
public class WebClientAutoConfiguration {

  private WebClientAutoConfiguration() {
  }

  @Prototype
  @ConditionalOnMissingBean
  public static WebClient.Builder webClientBuilder(List<WebClientCustomizer> customizers) {
    WebClient.Builder builder = WebClient.builder();
    for (WebClientCustomizer customizer : customizers) {
      customizer.customize(builder);
    }
    return builder;
  }

  @Component
  @ConditionalOnMissingBean(WebClientSsl.class)
  @ConditionalOnBean(SslBundles.class)
  public static AutoConfiguredWebClientSsl webClientSsl(ClientHttpConnectorFactory<?> clientHttpConnectorFactory, SslBundles sslBundles) {
    return new AutoConfiguredWebClientSsl(clientHttpConnectorFactory, sslBundles);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(CodecCustomizer.class)
  protected static class WebClientCodecsConfiguration {

    @Component
    @ConditionalOnMissingBean
    @Order(0)
    public static WebClientCodecCustomizer exchangeStrategiesCustomizer(List<CodecCustomizer> codecCustomizers) {
      return new WebClientCodecCustomizer(codecCustomizers);
    }

  }

}
