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
