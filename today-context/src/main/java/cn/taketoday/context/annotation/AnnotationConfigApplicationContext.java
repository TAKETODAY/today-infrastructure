/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.GenericXmlApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.lang.Nullable;

/**
 * Standalone application context, accepting <em>component classes</em> as input &mdash;
 * in particular {@link Configuration @Configuration}-annotated classes, but also plain
 * {@link cn.taketoday.stereotype.Component @Component} types and JSR-330 compliant
 * classes using {@code jakarta.inject} annotations.
 *
 * <p>Allows for registering classes one by one using {@link #register(Class...)}
 * as well as for classpath scanning using {@link #scan(String...)}.
 *
 * <p>In case of multiple {@code @Configuration} classes, {@link cn.taketoday.stereotype.Component @Component} methods
 * defined in later classes will override those defined in earlier classes. This can
 * be leveraged to deliberately override certain bean definitions via an extra
 * {@code @Configuration} class.
 *
 * <p>See {@link Configuration @Configuration}'s javadoc for usage examples.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see GenericXmlApplicationContext
 * @since 4.0 2022/2/20 21:24
 */
public class AnnotationConfigApplicationContext extends StandardApplicationContext {
  /**
   * Create a new AnnotationConfigApplicationContext that needs to be populated
   * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
   */
  public AnnotationConfigApplicationContext() { }

  /**
   * Create a new AnnotationConfigApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public AnnotationConfigApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  public AnnotationConfigApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  public AnnotationConfigApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    super(beanFactory, parent);
  }

  /**
   * Create a new AnnotationConfigApplicationContext, deriving bean definitions
   * from the given component classes and automatically refreshing the context.
   *
   * @param components one or more component classes &mdash; for example,
   * {@link Configuration @Configuration} classes
   */
  public AnnotationConfigApplicationContext(Class<?>... components) {
    super(components);
  }

  /**
   * Create a new AnnotationConfigApplicationContext, scanning for components
   * in the given packages, registering bean definitions for those components,
   * and automatically refreshing the context.
   *
   * @param basePackages the packages to scan for component classes
   */
  public AnnotationConfigApplicationContext(String... basePackages) {
    super(basePackages);
  }
}
