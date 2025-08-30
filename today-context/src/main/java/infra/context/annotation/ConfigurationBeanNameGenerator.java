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

import infra.beans.factory.support.BeanNameGenerator;
import infra.core.type.MethodMetadata;
import infra.stereotype.Component;

/**
 * Extended variant of {@link BeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only covering
 * bean name generation for component and configuration classes themselves
 * but also for {@link Component @Component} methods without a {@link Component#name() name}
 * attribute specified on the annotation itself.
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
   * in the absence of a {@link Component#name() name} attribute specified.
   *
   * @param componentMethod the method metadata for the {@link Component @Component} method
   * @return the default bean name to use
   */
  String deriveBeanName(MethodMetadata componentMethod);

}
