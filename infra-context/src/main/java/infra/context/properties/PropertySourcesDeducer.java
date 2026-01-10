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
