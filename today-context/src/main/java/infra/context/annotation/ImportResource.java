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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.support.BeanDefinitionReader;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.core.annotation.AliasFor;

/**
 * Indicates one or more resources containing bean definitions to import.
 *
 * <p>Like {@link Import @Import}, this annotation provides functionality similar to
 * the {@code <import/>} element in Framework XML. It is typically used when designing
 * {@link Configuration @Configuration} classes to be bootstrapped by an
 * {@link AnnotationConfigApplicationContext}, but where some XML functionality such
 * as namespaces is still necessary.
 *
 * <p>By default, arguments to the {@link #value} attribute will be processed using a
 * {@link XmlBeanDefinitionReader XmlBeanDefinitionReader}
 * will be used to parse Framework {@code <beans/>} XML files. Optionally, the {@link #reader}
 * attribute may be declared, allowing the user to choose a custom {@link BeanDefinitionReader}
 * implementation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Configuration
 * @see Import
 * @since 4.0 2022/3/8 17:39
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

  /**
   * Alias for {@link #locations}.
   *
   * @see #locations
   * @see #reader
   */
  @AliasFor("locations")
  String[] value() default {};

  /**
   * Resource locations from which to import.
   * <p>Supports resource-loading prefixes such as {@code classpath:},
   * {@code file:}, etc.
   * <p>Consult the Javadoc for {@link #reader} for details on how resources
   * will be processed.
   *
   * @see #value
   * @see #reader
   */
  @AliasFor("value")
  String[] locations() default {};

  /**
   * {@link BeanDefinitionReader} implementation to use when processing
   * resources specified via the {@link #locations() locations} or
   * {@link #value() value} attribute.
   * <p>The configured {@code BeanDefinitionReader} type must declare a
   * constructor that accepts a single
   * {@link infra.beans.factory.support.BeanDefinitionRegistry
   * BeanDefinitionRegistry} argument.
   * all other resources will be processed
   * with an {@link infra.beans.factory.xml.XmlBeanDefinitionReader
   * XmlBeanDefinitionReader}.
   * F
   *
   * @see #locations
   * @see #value
   */
  Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
