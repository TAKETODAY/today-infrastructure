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

package cn.taketoday.annotation.config.web.reactive.client;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.runner.ReactiveWebApplicationContextRunner;
import cn.taketoday.http.client.reactive.HttpComponentsClientHttpConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ClientHttpConnectorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
class ClientHttpConnectorFactoryConfigurationTests {

  @Test
  void shouldApplyHttpClientMapper() {
    JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
    JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
    SslBundle sslBundle = spy(SslBundle.of(stores, SslBundleKey.of("password")));
    new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClientHttpConnectorFactoryConfiguration.ReactorNetty.class))
            .withUserConfiguration(CustomHttpClientMapper.class)
            .run((context) -> {
              context.getBean(ReactorClientHttpConnectorFactory.class).createClientHttpConnector(sslBundle);
              assertThat(CustomHttpClientMapper.called).isTrue();
              then(sslBundle).should().getManagers();
            });
  }

  @Test
  void shouldNotConfigureReactiveHttpClient5WhenHttpCore5ReactiveJarIsMissing() {
    new ReactiveWebApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("org.apache.hc.core5.reactive"))
            .withConfiguration(AutoConfigurations.of(ClientHttpConnectorFactoryConfiguration.HttpClient5.class))
            .run((context) -> assertThat(context).doesNotHaveBean(HttpComponentsClientHttpConnector.class));
  }

  static class CustomHttpClientMapper {

    static boolean called = false;

    @Bean
    ReactorNettyHttpClientMapper clientMapper() {
      return (client) -> {
        called = true;
        return client.baseUrl("/test");
      };
    }

  }

}
