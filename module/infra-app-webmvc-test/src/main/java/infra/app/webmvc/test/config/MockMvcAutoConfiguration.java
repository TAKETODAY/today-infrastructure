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

import infra.app.config.ConditionalOnWebApplication;
import infra.app.config.ConditionalOnWebApplication.Type;
import infra.context.annotation.Import;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.test.web.mock.MockMvc;
import infra.web.DispatcherHandler;
import infra.web.config.WebMvcAutoConfiguration;
import infra.web.config.WebMvcProperties;

/**
 * Auto-configuration for {@link MockMvc}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see AutoConfigureWebMvc
 * @since 5.0
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.MVC)
@EnableConfigurationProperties(WebMvcProperties.class)
@Import({ MockMvcConfiguration.class, MockMvcTesterConfiguration.class })
public final class MockMvcAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  static DispatcherHandler dispatcherHandler(MockMvc mockMvc) {
    return mockMvc.getDispatcher();
  }

}
