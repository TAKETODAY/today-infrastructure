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

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.Resource;
import infra.web.RequestContextUtils;

/**
 * Subclass of {@link GenericApplicationContext}, suitable for reactive web environments.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 4.0
 */
public class GenericReactiveWebApplicationContext extends GenericApplicationContext
        implements ConfigurableReactiveWebApplicationContext {

  /**
   * Create a new {@link GenericReactiveWebApplicationContext}.
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericReactiveWebApplicationContext() { }

  /**
   * Create a new {@link GenericReactiveWebApplicationContext} with the given
   * StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericReactiveWebApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
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

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    RequestContextUtils.registerScopes(beanFactory);
  }
}
