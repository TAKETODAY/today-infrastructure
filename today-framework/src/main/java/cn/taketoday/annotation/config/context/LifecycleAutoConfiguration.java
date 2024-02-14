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

package cn.taketoday.annotation.config.context;

import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.SearchStrategy;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.context.support.DefaultLifecycleProcessor;
import cn.taketoday.stereotype.Component;

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

  @Component(name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
  @ConditionalOnMissingBean(
          search = SearchStrategy.CURRENT,
          name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
  static DefaultLifecycleProcessor defaultLifecycleProcessor(LifecycleProperties properties) {
    DefaultLifecycleProcessor lifecycleProcessor = new DefaultLifecycleProcessor();
    lifecycleProcessor.setTimeoutPerShutdownPhase(properties.getTimeoutPerShutdownPhase().toMillis());
    return lifecycleProcessor;
  }

}
