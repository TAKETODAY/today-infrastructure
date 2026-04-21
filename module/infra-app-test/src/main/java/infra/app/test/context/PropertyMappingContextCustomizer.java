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

package infra.app.test.context;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.context.ConfigurableApplicationContext;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.env.Environment;
import infra.stereotype.Component;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;
import infra.util.ClassUtils;

/**
 * {@link ContextCustomizer} to map annotation attributes to {@link Environment}
 * properties.
 *
 * @author Phillip Webb
 */
class PropertyMappingContextCustomizer implements ContextCustomizer {

  private final AnnotationsPropertySource propertySource;

  PropertyMappingContextCustomizer(AnnotationsPropertySource propertySource) {
    this.propertySource = propertySource;
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedContextConfiguration) {
    if (!this.propertySource.isEmpty()) {
      context.getEnvironment().getPropertySources().addFirst(this.propertySource);
    }
    context.getBeanFactory()
            .registerSingleton(PropertyMappingCheckBeanPostProcessor.class.getName(),
                    new PropertyMappingCheckBeanPostProcessor());
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (obj != null) && (getClass() == obj.getClass())
            && this.propertySource.equals(((PropertyMappingContextCustomizer) obj).propertySource);
  }

  @Override
  public int hashCode() {
    return this.propertySource.hashCode();
  }

  /**
   * {@link BeanPostProcessor} to check that {@link PropertyMapping @PropertyMapping} is
   * only used on test classes.
   */
  static class PropertyMappingCheckBeanPostProcessor implements InitializationBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      Class<?> beanClass = bean.getClass();
      MergedAnnotations annotations = MergedAnnotations.from(beanClass, SearchStrategy.SUPERCLASS);
      Set<Class<?>> components = annotations.stream(Component.class)
              .map(this::getRoot)
              .collect(Collectors.toSet());
      Set<Class<?>> propertyMappings = annotations.stream(PropertyMapping.class)
              .map(this::getRoot)
              .collect(Collectors.toSet());
      if (!components.isEmpty() && !propertyMappings.isEmpty()) {
        throw new IllegalStateException("The @PropertyMapping " + getAnnotationsDescription(propertyMappings)
                + " cannot be used in combination with the @Component "
                + getAnnotationsDescription(components));
      }
      return bean;
    }

    private Class<?> getRoot(MergedAnnotation<?> annotation) {
      return annotation.getRoot().getType();
    }

    private String getAnnotationsDescription(Set<Class<?>> annotations) {
      StringBuilder result = new StringBuilder();
      for (Class<?> annotation : annotations) {
        if (!result.isEmpty()) {
          result.append(", ");
        }
        result.append('@').append(ClassUtils.getShortName(annotation));
      }
      result.insert(0, (annotations.size() != 1) ? "annotations " : "annotation ");
      return result.toString();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
    }

  }

}
