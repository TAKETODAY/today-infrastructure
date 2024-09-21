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

package cn.taketoday.context.properties;

import cn.taketoday.aop.scope.ScopedProxyUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.ScopeMetadata;
import cn.taketoday.context.annotation.ScopeMetadataResolver;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
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

  private static final ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

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
    return beanFactory.containsBeanDefinition(name);
  }

  private void registerBeanDefinition(String beanName, Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
    if (!annotation.isPresent()) {
      throw new IllegalStateException("No %s annotation found on '%s'.".formatted(ConfigurationProperties.class.getSimpleName(), type.getName()));
    }
    BeanDefinitionReaderUtils.registerBeanDefinition(createBeanDefinition(beanName, type), this.registry);
  }

  private BeanDefinitionHolder createBeanDefinition(String beanName, Class<?> type) {
    AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(type);
    AnnotationConfigUtils.processCommonDefinitionAnnotations(definition);
    BindMethod bindMethod = ConfigurationPropertiesBean.deduceBindMethod(type);
    BindMethodAttribute.set(definition, bindMethod);
    if (bindMethod == BindMethod.VALUE_OBJECT) {
      definition.setInstanceSupplier(() -> ConstructorBound.from(this.beanFactory, beanName, type));
    }
    ScopeMetadata metadata = scopeMetadataResolver.resolveScopeMetadata(definition);
    definition.setScope(metadata.getScopeName());
    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition, beanName);
    return applyScopedProxyMode(metadata, definitionHolder, this.registry);
  }

  static BeanDefinitionHolder applyScopedProxyMode(ScopeMetadata metadata, BeanDefinitionHolder definition,
          BeanDefinitionRegistry registry) {
    ScopedProxyMode mode = metadata.getScopedProxyMode();
    if (mode != ScopedProxyMode.NO) {
      return ScopedProxyUtils.createScopedProxy(definition, registry, mode == ScopedProxyMode.TARGET_CLASS);
    }
    return definition;
  }

}
