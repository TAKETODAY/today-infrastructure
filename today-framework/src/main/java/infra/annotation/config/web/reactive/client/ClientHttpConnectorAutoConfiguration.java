/*
 * Copyright 2017 - 2024 the original author or authors.
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

import infra.context.annotation.Import;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.AutoConfigureAfter;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.annotation.Order;
import infra.http.client.reactive.ClientHttpConnector;
import infra.stereotype.Component;
import infra.web.client.reactive.WebClient;
import infra.annotation.config.ssl.SslAutoConfiguration;
import reactor.core.publisher.Mono;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ClientHttpConnector}.
 * <p>
 * It can produce a {@link infra.http.client.reactive.ClientHttpConnector}
 * bean and possibly a companion {@code ResourceFactory} bean, depending on the chosen
 * HTTP client library.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@ConditionalOnClass({ WebClient.class, Mono.class })
@AutoConfigureAfter(SslAutoConfiguration.class)
@Import({ ClientHttpConnectorFactoryConfiguration.ReactorNetty.class,
        ClientHttpConnectorFactoryConfiguration.HttpClient5.class,
        ClientHttpConnectorFactoryConfiguration.JdkClient.class })
public class ClientHttpConnectorAutoConfiguration {

  @Lazy
  @Component
  @ConditionalOnMissingBean(ClientHttpConnector.class)
  static ClientHttpConnector webClientHttpConnector(ClientHttpConnectorFactory<?> clientHttpConnectorFactory) {
    return clientHttpConnectorFactory.createClientHttpConnector();
  }

  @Lazy
  @Order(0)
  @Component
  @ConditionalOnBean(ClientHttpConnector.class)
  static WebClientCustomizer webClientHttpConnectorCustomizer(ClientHttpConnector clientHttpConnector) {
    return builder -> builder.clientConnector(clientHttpConnector);
  }

}
