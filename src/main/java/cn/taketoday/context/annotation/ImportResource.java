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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicates one or more resources containing bean definitions to import.
 *
 * <p>Like {@link Import @Import}, this annotation provides functionality similar to
 * the {@code <import/>} element in Spring XML. It is typically used when designing
 * {@link Configuration @Configuration} classes to be bootstrapped by an
 * {@link AnnotationConfigApplicationContext}, but where some XML functionality such
 * as namespaces is still necessary.
 *
 * <p>By default, arguments to the {@link #value} attribute will be processed using a
 * {@link cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}
 * will be used to parse Spring {@code <beans/>} XML files. Optionally, the {@link #reader}
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
   * resources specified via the {@link #value} attribute.
   * <p>By default, the reader will be adapted to the resource path specified:
   * resources will be processed with an
   * {@link cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}.
   *
   * @see #value
   */
  Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
