/*
 * Copyright 2002-present the original author or authors.
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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.support.BeanNameGenerator;
import infra.core.type.MethodMetadata;
import infra.stereotype.Component;

/**
 * Extended variant of {@link BeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only covering
 * bean name generation for component and configuration classes themselves
 * but also for {@link Component @Component} methods.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AnnotationConfigApplicationContext#setBeanNameGenerator
 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
 * @since 5.0
 */
public interface ConfigurationBeanNameGenerator extends BeanNameGenerator {

  /**
   * Derive a default bean name for the given {@link Component @Component} method,
   * providing the {@link Component#name() name} attribute specified.
   *
   * @param beanMethod the method metadata for the {@link Component @Component} method
   * @param beanName the {@link Component#name() name} attribute or {@code null} if non is specified
   * @return the default bean name to use
   */
  String deriveBeanName(MethodMetadata beanMethod, @Nullable String beanName);

}
