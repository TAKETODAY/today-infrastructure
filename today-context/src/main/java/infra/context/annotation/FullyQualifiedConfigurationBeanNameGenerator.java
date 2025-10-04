/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import infra.core.type.MethodMetadata;

/**
 * Extended variant of {@link FullyQualifiedAnnotationBeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only enforcing
 * fully-qualified names for component and configuration classes themselves
 * but also fully-qualified default bean names ("className.methodName") for
 * {@link Bean @Bean} methods. This only affects methods without an explicit
 * {@link Bean#name() name} attribute specified.
 *
 * <p>This provides an alternative to the default bean name generation for
 * {@code @Bean} methods (which uses the plain method name), primarily for use
 * in large applications with potential bean name overlaps. Favor this bean
 * naming strategy over {@code FullyQualifiedAnnotationBeanNameGenerator} if
 * you expect such naming conflicts for {@code @Bean} methods, as long as the
 * application does not depend on {@code @Bean} method names as bean names.
 * Where the name does matter, make sure to declare {@code @Bean("myBeanName")}
 * in such a scenario, even if it repeats the method name as the bean name.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AnnotationBeanNameGenerator
 * @see FullyQualifiedAnnotationBeanNameGenerator
 * @see AnnotationConfigApplicationContext#setBeanNameGenerator
 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
 * @since 5.0
 */
public class FullyQualifiedConfigurationBeanNameGenerator extends FullyQualifiedAnnotationBeanNameGenerator
        implements ConfigurationBeanNameGenerator {

  /**
   * A convenient constant for a default {@code FullyQualifiedConfigurationBeanNameGenerator}
   * instance, as used for configuration-level import purposes.
   */
  public static final FullyQualifiedConfigurationBeanNameGenerator INSTANCE =
          new FullyQualifiedConfigurationBeanNameGenerator();

  @Override
  public String deriveBeanName(MethodMetadata beanMethod, @Nullable String beanName) {
    return beanName != null ? beanName : beanMethod.getDeclaringClassName() + "." + beanMethod.getMethodName();
  }

}
