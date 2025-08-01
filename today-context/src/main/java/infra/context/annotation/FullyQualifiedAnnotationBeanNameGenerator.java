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

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.DefaultBeanNameGenerator;
import infra.lang.Assert;

/**
 * An extension of {@code AnnotationBeanNameGenerator} that uses the fully qualified
 * class name as the default bean name if an explicit bean name is not supplied via
 * a supported type-level annotation such as {@code @Component} (see
 * {@link AnnotationBeanNameGenerator} for details on supported annotations).
 *
 * <p>Favor this bean naming strategy over {@code AnnotationBeanNameGenerator} if
 * you run into naming conflicts due to multiple autodetected components having the
 * same non-qualified class name (i.e., classes with identical names but residing in
 * different packages). If you need such conflict avoidance for {@link Bean @Bean}
 * methods as well, consider {@link FullyQualifiedConfigurationBeanNameGenerator}.
 *
 * <p>Note that an instance of this class is used by default for configuration-level
 * import purposes; whereas, the default for component scanning purposes is a plain
 * {@code AnnotationBeanNameGenerator}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultBeanNameGenerator
 * @see AnnotationBeanNameGenerator
 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
 * @since 4.0
 */
public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

  /**
   * A convenient constant for a default {@code FullyQualifiedAnnotationBeanNameGenerator}
   * instance, as used for configuration-level import purposes.
   */
  public static final FullyQualifiedAnnotationBeanNameGenerator INSTANCE =
          new FullyQualifiedAnnotationBeanNameGenerator();

  @Override
  protected String buildDefaultBeanName(BeanDefinition definition) {
    String beanClassName = definition.getBeanClassName();
    Assert.state(beanClassName != null, "No bean class name set");
    return beanClassName;
  }

}
