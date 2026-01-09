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

import infra.aop.scope.ScopedProxyUtils;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.BeanDefinitionReaderUtils;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.BootstrapContext;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.AnnotationScopeMetadataResolver;
import infra.context.annotation.ScopeMetadata;
import infra.context.annotation.ScopeMetadataResolver;
import infra.context.annotation.ScopedProxyMode;
import infra.context.properties.bind.BindMethod;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.util.StringUtils;

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
