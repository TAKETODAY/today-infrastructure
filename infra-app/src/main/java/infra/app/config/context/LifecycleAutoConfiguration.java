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

package infra.app.config.context;

import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.SearchStrategy;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.support.AbstractApplicationContext;
import infra.context.support.DefaultLifecycleProcessor;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} relating to the application
 * context's lifecycle.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@EnableConfigurationProperties(LifecycleProperties.class)
public class LifecycleAutoConfiguration {

  private LifecycleAutoConfiguration() {
  }

  @Component(name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
  @ConditionalOnMissingBean(search = SearchStrategy.CURRENT,
          name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
  public static DefaultLifecycleProcessor defaultLifecycleProcessor(LifecycleProperties properties) {
    DefaultLifecycleProcessor lifecycleProcessor = new DefaultLifecycleProcessor();
    lifecycleProcessor.setTimeoutPerShutdownPhase(properties.getTimeoutPerShutdownPhase().toMillis());
    return lifecycleProcessor;
  }

}
