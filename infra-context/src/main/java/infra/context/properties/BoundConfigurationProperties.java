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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ApplicationContext;
import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.lang.Assert;

/**
 * Bean to record and provide bound
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BoundConfigurationProperties {

  private final LinkedHashMap<ConfigurationPropertyName, ConfigurationProperty> properties = new LinkedHashMap<>();

  /**
   * The bean name that this class is registered with.
   */
  private static final String BEAN_NAME = BoundConfigurationProperties.class.getName();

  void add(ConfigurationProperty configurationProperty) {
    this.properties.put(configurationProperty.getName(), configurationProperty);
  }

  /**
   * Get the configuration property bound to the given name.
   *
   * @param name the property name
   * @return the bound property or {@code null}
   */
  @Nullable
  public ConfigurationProperty get(ConfigurationPropertyName name) {
    return this.properties.get(name);
  }

  /**
   * Get all bound properties.
   *
   * @return a map of all bound properties
   */
  public Map<ConfigurationPropertyName, ConfigurationProperty> getAll() {
    return Collections.unmodifiableMap(this.properties);
  }

  /**
   * Return the {@link BoundConfigurationProperties} from the given
   * {@link ApplicationContext} if it is available.
   *
   * @param context the context to search
   * @return a {@link BoundConfigurationProperties} or {@code null}
   */
  @Nullable
  public static BoundConfigurationProperties get(ApplicationContext context) {
    if (!context.containsBeanDefinition(BEAN_NAME)) {
      return null;
    }
    return context.getBean(BEAN_NAME, BoundConfigurationProperties.class);
  }

  static void register(BeanDefinitionRegistry registry) {
    Assert.notNull(registry, "Registry is required");
    if (!registry.containsBeanDefinition(BEAN_NAME)) {
      BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(BoundConfigurationProperties.class)
              .setEnableDependencyInjection(false)
              .setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
              .getBeanDefinition();
      registry.registerBeanDefinition(BEAN_NAME, definition);
    }
  }

}
