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

package infra.context.properties;

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
import infra.lang.Nullable;

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
