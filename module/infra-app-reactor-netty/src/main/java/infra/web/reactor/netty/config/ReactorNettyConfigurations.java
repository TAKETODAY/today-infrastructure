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

package infra.web.reactor.netty.config;

import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.http.support.ReactorResourceFactory;
import infra.stereotype.Component;

/**
 * Configurations for Reactor Netty. Those should be {@code @Import} in a regular
 * auto-configuration class.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class ReactorNettyConfigurations {

  private ReactorNettyConfigurations() {
  }

  @Lazy
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ReactorNettyProperties.class)
  public static class ReactorResourceFactoryConfiguration {

    @Component
    @ConditionalOnMissingBean
    public static ReactorResourceFactory reactorResourceFactory(ReactorNettyProperties configurationProperties) {
      ReactorResourceFactory reactorResourceFactory = new ReactorResourceFactory();
      if (configurationProperties.shutdownQuietPeriod != null) {
        reactorResourceFactory.setShutdownQuietPeriod(configurationProperties.shutdownQuietPeriod);
      }
      return reactorResourceFactory;
    }

  }

}
