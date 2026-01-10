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

import infra.beans.FatalBeanException;

/**
 * @author TODAY 2018-07-08 19:54:46
 */
public class BeanDefinitionStoreException extends FatalBeanException {

  @Nullable
  private final String resourceDescription;

  @Nullable
  private final String beanName;

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param msg the detail message (used as exception message as-is)
   * @since 4.0
   */
  public BeanDefinitionStoreException(String msg) {
    super(msg);
    this.resourceDescription = null;
    this.beanName = null;
  }

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param msg the detail message (used as exception message as-is)
   * @param cause the root cause (may be {@code null})
   * @since 4.0
   */
  public BeanDefinitionStoreException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
    this.resourceDescription = null;
    this.beanName = null;
  }

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param msg the detail message (used as exception message as-is)
   * @since 4.0
   */
  public BeanDefinitionStoreException(@Nullable String resourceDescription, String msg) {
    super(msg);
    this.resourceDescription = resourceDescription;
    this.beanName = null;
  }

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param msg the detail message (used as exception message as-is)
   * @param cause the root cause (may be {@code null})
   * @since 4.0
   */
  public BeanDefinitionStoreException(@Nullable String resourceDescription, String msg, @Nullable Throwable cause) {
    super(msg, cause);
    this.resourceDescription = resourceDescription;
    this.beanName = null;
  }

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param beanName the name of the bean
   * @param msg the detail message (appended to an introductory message that indicates
   * the resource and the name of the bean)
   * @since 4.0
   */
  public BeanDefinitionStoreException(@Nullable String resourceDescription, String beanName, String msg) {
    this(resourceDescription, beanName, msg, null);
  }

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param beanName the name of the bean
   * @param msg the detail message (appended to an introductory message that indicates
   * the resource and the name of the bean)
   * @param cause the root cause (may be {@code null})
   * @since 4.0
   */
  public BeanDefinitionStoreException(@Nullable String resourceDescription, @Nullable String beanName, @Nullable String msg, @Nullable Throwable cause) {
    super("Invalid bean definition with name '%s' defined in %s: %s".formatted(beanName, resourceDescription, msg),
            cause);
    this.resourceDescription = resourceDescription;
    this.beanName = beanName;
  }

  /**
   * Create a new BeanDefinitionStoreException.
   *
   * @param beanName the name of the bean
   * @param msg the detail message (appended to an introductory message that indicates
   * the resource and the name of the bean)
   * @param cause the root cause (may be {@code null})
   * @since 4.0
   */
  public BeanDefinitionStoreException(@Nullable Throwable cause, @Nullable String beanName, String msg) {
    super(msg, cause);
    this.resourceDescription = null;
    this.beanName = beanName;
  }

  /**
   * Return the description of the resource that the bean definition came from, if available.
   *
   * @since 4.0
   */
  @Nullable
  public String getResourceDescription() {
    return this.resourceDescription;
  }

  /**
   * Return the name of the bean, if available.
   *
   * @since 4.0
   */
  @Nullable
  public String getBeanName() {
    return this.beanName;
  }

}
