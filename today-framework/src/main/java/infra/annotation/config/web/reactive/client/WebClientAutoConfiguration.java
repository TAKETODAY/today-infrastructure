/*
 * Copyright 2017 - 2025 the original author or authors.
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
