/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.web.server.reactive.context;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.Resource;
import infra.stereotype.Component;

/**
 * {@link ConfigurableReactiveWebApplicationContext} that accepts annotated classes as
 * input - in particular {@link Configuration @Configuration}-annotated classes, but also
 * plain {@link Component @Component} classes and JSR-330 compliant classes using
 * {@code javax.inject} annotations. Allows for registering classes one by one (specifying
 * class names as config location) as well as for classpath scanning (specifying base
 * packages as config location).
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationConfigApplicationContext
 * @since 4.0
 */
public class AnnotationConfigReactiveWebApplicationContext extends AnnotationConfigApplicationContext
        implements ConfigurableReactiveWebApplicationContext {

  /**
   * Create a new AnnotationConfigReactiveWebApplicationContext that needs to be
   * populated through {@link #register} calls and then manually {@linkplain #refresh
   * refreshed}.
   */
  public AnnotationConfigReactiveWebApplicationContext() { }

  /**
   * Create a new AnnotationConfigApplicationContext with the given
   * StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public AnnotationConfigReactiveWebApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Create a new AnnotationConfigApplicationContext, deriving bean definitions from the
   * given annotated classes and automatically refreshing the context.
   *
   * @param annotatedClasses one or more annotated classes, e.g.
   * {@link Configuration @Configuration} classes
   */
  public AnnotationConfigReactiveWebApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  /**
   * Create a new AnnotationConfigApplicationContext, scanning for bean definitions in
   * the given packages and automatically refreshing the context.
   *
   * @param basePackages the packages to check for annotated classes
   */
  public AnnotationConfigReactiveWebApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardReactiveWebEnvironment();
  }

  @Override
  protected Resource getResourceByPath(String path) {
    // We must be careful not to expose classpath resources
    return new FilteredReactiveWebContextResource(path);
  }

}
