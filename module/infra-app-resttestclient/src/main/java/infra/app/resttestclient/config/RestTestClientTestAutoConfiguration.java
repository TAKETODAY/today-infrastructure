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

package infra.app.resttestclient.config;

import java.util.List;

import infra.app.test.http.server.LocalTestWebServer;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.ObjectProvider;
import infra.context.ApplicationContext;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.converter.config.ClientHttpMessageConvertersCustomizer;
import infra.stereotype.Component;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.client.RestTestClient;
import infra.web.client.RestClient;

/**
 * Test auto-configuration for {@link RestTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see AutoConfigureRestTestClient
 */
@AutoConfiguration
@ConditionalOnClass({ RestClient.class, RestTestClient.class, ClientHttpMessageConvertersCustomizer.class })
final class RestTestClientTestAutoConfiguration {

  @Component
  static InfraRestTestClientBuilderCustomizer infraRestTestClientBuilderCustomizer(
          ObjectProvider<ClientHttpMessageConvertersCustomizer> httpMessageConverterCustomizers) {
    return new InfraRestTestClientBuilderCustomizer(httpMessageConverterCustomizers.orderedStream().toList());
  }

  @Component
  @ConditionalOnMissingBean
  RestTestClient restTestClient(ApplicationContext applicationContext,
          List<RestTestClientBuilderCustomizer> customizers) {
    RestTestClient.Builder<?> builder = getBuilder(applicationContext);
    customizers.forEach((customizer) -> customizer.customize(builder));
    return builder.build();
  }

  private RestTestClient.Builder<?> getBuilder(ApplicationContext applicationContext) {
    LocalTestWebServer localTestWebServer = LocalTestWebServer.get(applicationContext);
    if (localTestWebServer != null) {
      return RestTestClient.bindToServer().uriBuilderFactory(localTestWebServer.uriBuilderFactory());
    }
    if (hasBean(applicationContext, MockMvc.class)) {
      return RestTestClient.bindTo(applicationContext.getBean(MockMvc.class));
    }
    return RestTestClient.bindToApplicationContext(applicationContext);
  }

  private boolean hasBean(ApplicationContext applicationContext, Class<?> type) {
    try {
      applicationContext.getBean(type);
      return true;
    }
    catch (NoSuchBeanDefinitionException ex) {
      return false;
    }
  }

}
