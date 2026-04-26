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

package infra.app.webmvc.test.config;

import java.util.List;

import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.ConfigurationProperties;
import infra.test.web.mock.DispatcherCustomizer;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MockMvcBuilder;
import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.config.WebMvcProperties;
import infra.web.mock.MockDispatcherHandler;

/**
 * Configuration for core {@link MockMvc}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class MockMvcConfiguration {

  @Bean
  @ConditionalOnMissingBean(MockMvcBuilder.class)
  static DefaultMockMvcBuilder mockMvcBuilder(ApplicationContext context, List<MockMvcBuilderCustomizer> customizers, WebMvcProperties webMvcProperties) {
    DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context);
    builder.addDispatcherCustomizer(new MockMvcDispatcherCustomizer(webMvcProperties));
    for (MockMvcBuilderCustomizer customizer : customizers) {
      customizer.customize(builder);
    }
    return builder;
  }

  @Bean
  @ConfigurationProperties("infra.test.mockmvc")
  static InfraMockMvcBuilderCustomizer infraMockMvcBuilderCustomizer(ApplicationContext context) {
    return new InfraMockMvcBuilderCustomizer(context);
  }

  @Bean
  @ConditionalOnMissingBean
  MockMvc mockMvc(MockMvcBuilder builder) {
    return builder.build();
  }

  private static class MockMvcDispatcherCustomizer implements DispatcherCustomizer {

    private final WebMvcProperties properties;

    MockMvcDispatcherCustomizer(WebMvcProperties properties) {
      this.properties = properties;
    }

    @Override
    public void customize(MockDispatcherHandler handler) {
      handler.setThrowExceptionIfNoHandlerFound(properties.throwExceptionIfNoHandlerFound);
      handler.setEnableLoggingRequestDetails(properties.logRequestDetails);
    }
  }

}
