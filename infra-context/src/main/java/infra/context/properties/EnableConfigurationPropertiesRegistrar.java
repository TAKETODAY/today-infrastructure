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

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.BootstrapContext;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.Conventions;
import infra.core.type.AnnotationMetadata;
import infra.validation.beanvalidation.MethodValidationExcludeFilter;

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

    var beanRegistrar = new ConfigurationPropertiesBeanRegistrar(context);
    for (Class<?> type : getTypes(metadata)) {
      beanRegistrar.register(type);
    }
  }

  private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
    return metadata.getAnnotations()
            .stream(EnableConfigurationProperties.class)
            .flatMap(annotation -> Arrays.stream(annotation.getClassValueArray()))
            .filter(Predicate.isEqual(void.class).negate())
            .collect(Collectors.toSet());
  }

  static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
    ConfigurationPropertiesBindingPostProcessor.register(registry);
    BoundConfigurationProperties.register(registry);
  }

  static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
    if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
      BeanDefinition definition = BeanDefinitionBuilder
              .rootBeanDefinition(MethodValidationExcludeFilter.class, "byAnnotation")
              .addConstructorArgValue(ConfigurationProperties.class)
              .setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
              .setEnableDependencyInjection(false)
              .getBeanDefinition();
      registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
    }
  }

}
