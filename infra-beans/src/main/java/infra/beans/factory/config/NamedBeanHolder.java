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

package infra.beans.factory.config;

import infra.beans.factory.NamedBean;
import infra.lang.Assert;

/**
 * A simple holder for a given bean name and bean instance.
 *
 * @param <T> the bean type
 * @author Juergen Hoeller
 * @author TODAY 2021/10/26 21:49
 * @see AutowireCapableBeanFactory#resolveNamedBean(Class)
 * @since 4.0
 */
public class NamedBeanHolder<T> implements NamedBean {

  private final String beanName;

  private final T beanInstance;

  /**
   * Create a new holder for the given bean name plus instance.
   *
   * @param beanName the name of the bean
   * @param beanInstance the corresponding bean instance
   */
  public NamedBeanHolder(String beanName, T beanInstance) {
    Assert.notNull(beanName, "Bean name is required");
    this.beanName = beanName;
    this.beanInstance = beanInstance;
  }

  /**
   * Return the name of the bean.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the corresponding bean instance.
   */
  public T getBeanInstance() {
    return this.beanInstance;
  }

}
