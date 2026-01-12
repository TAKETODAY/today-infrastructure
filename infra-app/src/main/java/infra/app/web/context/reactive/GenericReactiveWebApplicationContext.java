/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.web.context.reactive;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.Resource;
import infra.web.context.reactive.StandardReactiveWebEnvironment;

/**
 * Subclass of {@link GenericApplicationContext}, suitable for reactive web environments.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
  public GenericReactiveWebApplicationContext() {
  }

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

}
