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

package cn.taketoday.beans.factory;

import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.lang.Nullable;

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
  public BeanDefinitionStoreException(@Nullable String resourceDescription, @Nullable String beanName, String msg, @Nullable Throwable cause) {
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
