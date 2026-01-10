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

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;

/**
 * Exception thrown when the BeanFactory cannot load the specified class
 * of a given bean.
 *
 * @author TODAY 2021/10/23 19:19
 * @since 4.0
 */
public class BeanClassLoadFailedException extends BeansException {

  @Nullable
  private final String resourceDescription;

  private final String beanName;

  @Nullable
  private final String beanClassName;

  /**
   * Create a new CannotLoadBeanClassException.
   *
   * @param resourceDescription description of the resource
   * that the bean definition came from
   * @param beanName the name of the bean requested
   * @param beanClassName the name of the bean class
   * @param cause the root cause
   */
  public BeanClassLoadFailedException(@Nullable String resourceDescription, String beanName,
          @Nullable String beanClassName, ClassNotFoundException cause) {
    super("Cannot find class [%s] for bean with name '%s'%s".formatted(beanClassName, beanName,
            resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
    this.resourceDescription = resourceDescription;
    this.beanName = beanName;
    this.beanClassName = beanClassName;
  }

  /**
   * Create a new CannotLoadBeanClassException.
   *
   * @param resourceDescription description of the resource
   * that the bean definition came from
   * @param beanName the name of the bean requested
   * @param beanClassName the name of the bean class
   * @param cause the root cause
   */
  public BeanClassLoadFailedException(@Nullable String resourceDescription, String beanName,
          @Nullable String beanClassName, LinkageError cause) {

    super("Error loading class [%s] for bean with name '%s'%s: problem with class file or dependent class"
            .formatted(beanClassName, beanName, resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
    this.resourceDescription = resourceDescription;
    this.beanName = beanName;
    this.beanClassName = beanClassName;
  }

  /**
   * Return the description of the resource that the bean
   * definition came from.
   */
  @Nullable
  public String getResourceDescription() {
    return this.resourceDescription;
  }

  /**
   * Return the name of the bean requested.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the name of the class we were trying to load.
   */
  @Nullable
  public String getBeanClassName() {
    return this.beanClassName;
  }

}
