/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.validation.beanvalidation.MethodValidationExcludeFilter;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link EnableConfigurationProperties @EnableConfigurationProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {

  private static final String METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME = Conventions.getQualifiedAttributeName(
          EnableConfigurationPropertiesRegistrar.class, "methodValidationExcludeFilter");

  @Override
  public void registerBeanDefinitions(AnnotationMetadata metadata, BootstrapContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();
    registerInfrastructureBeans(registry);
    registerMethodValidationExcludeFilter(registry);
    ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(context);
    getTypes(metadata).forEach(beanRegistrar::register);
  }

  private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
    return metadata.getAnnotations().stream(EnableConfigurationProperties.class)
            .flatMap((annotation) -> Arrays.stream(annotation.getClassValueArray()))
            .filter((Predicate<Class>) (type) -> void.class != type)
            .collect(Collectors.toSet());
  }

  static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
    ConfigurationPropertiesBindingPostProcessor.register(registry);
    BoundConfigurationProperties.register(registry);
  }

  static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
    if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
      RootBeanDefinition definition = new RootBeanDefinition(MethodValidationExcludeFilter.class);
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      definition.setInstanceSupplier(() -> MethodValidationExcludeFilter.byAnnotation(ConfigurationProperties.class));

      registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
    }
  }

}
