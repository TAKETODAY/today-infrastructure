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

package infra.aop.scope;

import java.io.Serializable;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Assert;

/**
 * Default implementation of the {@link ScopedObject} interface.
 *
 * <p>Simply delegates the calls to the underlying
 * {@link ConfigurableBeanFactory bean factory}
 * ({@link ConfigurableBeanFactory#getBean(String)}/
 * {@link ConfigurableBeanFactory#destroyScopedBean(String)}).
 *
 * @author Juergen Hoeller
 * @see BeanFactory#getBean
 * @see ConfigurableBeanFactory#destroyScopedBean
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultScopedObject implements ScopedObject, Serializable {

  private final ConfigurableBeanFactory beanFactory;

  private final String targetBeanName;

  /**
   * Creates a new instance of the {@link DefaultScopedObject} class.
   *
   * @param beanFactory the {@link ConfigurableBeanFactory} that holds the scoped target object
   * @param targetBeanName the name of the target bean
   */
  public DefaultScopedObject(ConfigurableBeanFactory beanFactory, String targetBeanName) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    Assert.hasText(targetBeanName, "'targetBeanName' must not be empty");
    this.beanFactory = beanFactory;
    this.targetBeanName = targetBeanName;
  }

  @Override
  @SuppressWarnings("NullAway")
  public Object getTargetObject() {
    return beanFactory.getBean(targetBeanName);
  }

  @Override
  public void removeFromScope() {
    this.beanFactory.destroyScopedBean(this.targetBeanName);
  }

}
