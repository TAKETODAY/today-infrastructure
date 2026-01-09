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

package infra.beans.factory.support;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.config.BeanDefinition;

/**
 * Subclass of {@link BeanDefinitionStoreException} indicating an invalid override
 * attempt: typically registering a new definition for the same bean name while
 * {@link StandardBeanFactory#isAllowBeanDefinitionOverriding()} is {@code false}.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/1 15:53
 * @see StandardBeanFactory#setAllowBeanDefinitionOverriding
 * @see StandardBeanFactory#registerBeanDefinition
 * @since 4.0
 */
public class BeanDefinitionOverrideException extends BeanDefinitionStoreException {

  private final BeanDefinition beanDefinition;

  private final BeanDefinition existingDefinition;

  /**
   * Create a new BeanDefinitionOverrideException for the given new and existing definition.
   *
   * @param beanName the name of the bean
   * @param beanDefinition the newly registered bean definition
   * @param existingDefinition the existing bean definition for the same name
   */
  public BeanDefinitionOverrideException(String beanName, BeanDefinition beanDefinition, BeanDefinition existingDefinition) {
    super(beanDefinition.getResourceDescription(), beanName,
            "Cannot register bean definition [%s] for bean '%s' since there is already [%s] bound."
                    .formatted(beanDefinition, beanName, existingDefinition));
    this.beanDefinition = beanDefinition;
    this.existingDefinition = existingDefinition;
  }

  /**
   * Create a new BeanDefinitionOverrideException for the given new and existing definition.
   *
   * @param beanName the name of the bean
   * @param beanDefinition the newly registered bean definition
   * @param existingDefinition the existing bean definition for the same name
   * @param msg the detail message to include
   * @since 5.0
   */
  public BeanDefinitionOverrideException(String beanName,
          BeanDefinition beanDefinition, BeanDefinition existingDefinition, String msg) {

    super(beanDefinition.getResourceDescription(), beanName, msg);
    this.beanDefinition = beanDefinition;
    this.existingDefinition = existingDefinition;
  }

  /**
   * Return the newly registered bean definition.
   */
  public BeanDefinition getBeanDefinition() {
    return this.beanDefinition;
  }

  /**
   * Return the existing bean definition for the same name.
   */
  public BeanDefinition getExistingDefinition() {
    return this.existingDefinition;
  }

}
