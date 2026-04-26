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

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.config.ServerHttpMessageConvertersCustomizer;
import infra.stereotype.Component;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.assertj.MockMvcTester;

/**
 * Configuration for {@link MockMvcTester}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.assertj.core.api.Assert")
class MockMvcTesterConfiguration {

  @Component
  @ConditionalOnMissingBean
  static MockMvcTester mockMvcTester(MockMvc mockMvc,
          ObjectProvider<ServerHttpMessageConvertersCustomizer> customizersProvider) {
    MockMvcTester mockMvcTester = MockMvcTester.create(mockMvc);
    List<ServerHttpMessageConvertersCustomizer> customizers = customizersProvider.orderedList();
    if (!customizers.isEmpty()) {
      ServerBuilder serverBuilder = HttpMessageConverters.forServer();
      for (var customizer : customizersProvider) {
        customizer.customize(serverBuilder);
      }
      mockMvcTester = mockMvcTester.withHttpMessageConverters(serverBuilder.build());
    }
    return mockMvcTester;
  }

}
