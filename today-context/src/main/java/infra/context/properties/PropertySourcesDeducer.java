/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.properties;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.context.ApplicationContext;
import infra.context.support.PropertySourcesPlaceholderConfigurer;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.PropertySources;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;

/**
 * Utility to deduce the {@link PropertySources} to use for configuration binding.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PropertySourcesDeducer {

  private static final Logger logger = LoggerFactory.getLogger(PropertySourcesDeducer.class);

  private final ApplicationContext context;

  PropertySourcesDeducer(ApplicationContext applicationContext) {
    this.context = applicationContext;
  }

  PropertySources getPropertySources() {
    PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
    if (configurer != null) {
      return configurer.getAppliedPropertySources();
    }
    PropertySources sources = extractEnvironmentPropertySources();
    Assert.state(sources != null,
            "Unable to obtain PropertySources from PropertySourcesPlaceholderConfigurer or Environment");
    return sources;
  }

  @Nullable
  private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
    // Take care not to cause early instantiation of all FactoryBeans
    Map<String, PropertySourcesPlaceholderConfigurer> beans = context.getBeansOfType(
            PropertySourcesPlaceholderConfigurer.class, false, false);
    if (beans.size() == 1) {
      return CollectionUtils.firstElement(beans.values());
    }
    if (beans.size() > 1 && logger.isWarnEnabled()) {
      logger.warn("Multiple PropertySourcesPlaceholderConfigurer beans registered {}, falling back to Environment",
              beans.keySet());
    }
    return null;
  }

  @Nullable
  private PropertySources extractEnvironmentPropertySources() {
    Environment environment = this.context.getEnvironment();
    if (environment instanceof ConfigurableEnvironment) {
      return ((ConfigurableEnvironment) environment).getPropertySources();
    }
    return null;
  }

}
