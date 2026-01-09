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

package infra.aop.target;

import infra.beans.factory.BeanFactory;
import infra.lang.Assert;

/**
 * Refreshable TargetSource that fetches fresh target beans from a BeanFactory.
 *
 * <p>Can be subclassed to override {@code requiresRefresh()} to suppress
 * unnecessary refreshes. By default, a refresh will be performed every time
 * the "refreshCheckDelay" has elapsed.
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author TODAY 2021/2/1 21:23
 * @see BeanFactory
 * @see #requiresRefresh()
 * @see #setRefreshCheckDelay
 * @since 3.0
 */
public class BeanFactoryRefreshableTargetSource extends AbstractRefreshableTargetSource {

  private final String beanName;

  private final BeanFactory beanFactory;

  /**
   * Create a new BeanFactoryRefreshableTargetSource for the given
   * bean factory and bean name.
   * <p>Note that the passed-in BeanFactory should have an appropriate
   * bean definition set up for the given bean name.
   *
   * @param beanFactory the BeanFactory to fetch beans from
   * @param beanName the name of the target bean
   */
  public BeanFactoryRefreshableTargetSource(BeanFactory beanFactory, String beanName) {
    Assert.notNull(beanName, "Bean name is required");
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanName = beanName;
    this.beanFactory = beanFactory;
  }

  /**
   * Retrieve a fresh target object.
   */
  @Override
  protected final Object freshTarget() {
    return this.obtainFreshBean(this.beanFactory, this.beanName);
  }

  /**
   * A template method that subclasses may override to provide a
   * fresh target object for the given bean factory and bean name.
   * <p>This default implementation fetches a new target bean
   * instance from the bean factory.
   *
   * @see BeanFactory#getBean
   */
  @SuppressWarnings("NullAway")
  protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
    return beanFactory.getBean(beanName);
  }

}
