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

package infra.beans.factory;

import infra.beans.BeansException;
import infra.util.ClassUtils;

/**
 * Thrown when a bean doesn't match the expected type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/10/26 21:36
 * @since 4.0
 */
public class BeanNotOfRequiredTypeException extends BeansException {

  /** The name of the instance that was of the wrong type. */
  private final String beanName;

  /** The required type. */
  private final Class<?> requiredType;

  /** The offending type. */
  private final Class<?> actualType;

  /**
   * Create a new BeanNotOfRequiredTypeException.
   *
   * @param beanName the name of the bean requested
   * @param requiredType the required type
   * @param actualType the actual type returned, which did not match
   * the expected type
   */
  public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
    super("Bean named '%s' is expected to be of type '%s' but was actually of type '%s'"
            .formatted(beanName, ClassUtils.getQualifiedName(requiredType), ClassUtils.getQualifiedName(actualType)));
    this.beanName = beanName;
    this.actualType = actualType;
    this.requiredType = requiredType;
  }

  /**
   * Return the name of the instance that was of the wrong type.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the expected type for the bean.
   */
  public Class<?> getRequiredType() {
    return this.requiredType;
  }

  /**
   * Return the actual type of the instance found.
   */
  public Class<?> getActualType() {
    return this.actualType;
  }

}
