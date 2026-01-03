/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.config.annotation;

import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.http.client.ReactorResourceFactory;
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
