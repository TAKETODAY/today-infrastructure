/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.HierarchicalBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Delegate used by {@link EnableConfigurationPropertiesRegistrar} and
 * {@link ConfigurationPropertiesScanRegistrar} to register a bean definition for a
 * {@link ConfigurationProperties @ConfigurationProperties} class.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ConfigurationPropertiesBeanRegistrar {

  private final BeanDefinitionRegistry registry;

  private final BeanFactory beanFactory;

  ConfigurationPropertiesBeanRegistrar(BootstrapContext context) {
    this.registry = context.getRegistry();
    this.beanFactory = context.getBeanFactory();
  }

  void register(Class<?> type) {
    var annotation = MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY)
            .get(ConfigurationProperties.class);
    register(type, annotation);
  }

  void register(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
    String name = getName(type, annotation);
    if (!containsBeanDefinition(name)) {
      registerBeanDefinition(name, type, annotation);
    }
  }

  private String getName(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
    String prefix = annotation.isPresent() ? annotation.getString("prefix") : "";
    return StringUtils.hasText(prefix) ? prefix + "-" + type.getName() : type.getName();
  }

  private boolean containsBeanDefinition(String name) {
    return containsBeanDefinition(this.beanFactory, name);
  }

  private boolean containsBeanDefinition(@Nullable BeanFactory beanFactory, String name) {
    if (beanFactory != null && beanFactory.containsBeanDefinition(name)) {
      return true;
    }
    if (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
      return containsBeanDefinition(hierarchicalBeanFactory.getParentBeanFactory(), name);
    }
    return false;
  }

  private void registerBeanDefinition(String beanName,
          Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
    if (!annotation.isPresent()) {
      throw new IllegalStateException(
              "No " + ConfigurationProperties.class.getSimpleName()
                      + " annotation found on  '" + type.getName() + "'.");
    }
    this.registry.registerBeanDefinition(beanName, createBeanDefinition(beanName, type));
  }

  private BeanDefinition createBeanDefinition(String beanName, Class<?> type) {
    BindMethod bindMethod = ConfigurationPropertiesBean.deduceBindMethod(type);
    RootBeanDefinition definition = new RootBeanDefinition(type);
    BindMethodAttribute.set(definition, bindMethod);
    if (bindMethod == BindMethod.VALUE_OBJECT) {
      definition.setInstanceSupplier(() -> ConstructorBound.from(this.beanFactory, beanName, type));
    }
    return definition;
  }

}
