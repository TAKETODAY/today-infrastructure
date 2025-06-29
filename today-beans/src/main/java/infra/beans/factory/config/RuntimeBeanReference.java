/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.config;

import infra.beans.factory.BeanFactory;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Immutable placeholder class used for a property value object when it's
 * a reference to another bean in the factory, to be resolved at runtime.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BeanDefinition#getPropertyValues()
 * @see BeanFactory#getBean(String)
 * @see BeanFactory#getBean(Class)
 * @since 4.0 2021/11/27 21:02
 */
public class RuntimeBeanReference implements BeanReference {

  private final String beanName;

  @Nullable
  private final Class<?> beanType;

  private final boolean toParent;

  @Nullable
  private Object source;

  /**
   * Create a new RuntimeBeanReference to the given bean name.
   *
   * @param beanName name of the target bean
   */
  public RuntimeBeanReference(String beanName) {
    this(beanName, false);
  }

  /**
   * Create a new RuntimeBeanReference to the given bean name,
   * with the option to mark it as reference to a bean in the parent factory.
   *
   * @param beanName name of the target bean
   * @param toParent whether this is an explicit reference to a bean in the
   * parent factory
   */
  public RuntimeBeanReference(String beanName, boolean toParent) {
    Assert.hasText(beanName, "'beanName' must not be empty");
    this.beanName = beanName;
    this.beanType = null;
    this.toParent = toParent;
  }

  /**
   * Create a new RuntimeBeanReference to a bean of the given type.
   *
   * @param beanType type of the target bean
   */
  public RuntimeBeanReference(Class<?> beanType) {
    this(beanType, false);
  }

  /**
   * Create a new RuntimeBeanReference to a bean of the given type,
   * with the option to mark it as reference to a bean in the parent factory.
   *
   * @param beanType type of the target bean
   * @param toParent whether this is an explicit reference to a bean in the
   * parent factory
   */
  public RuntimeBeanReference(Class<?> beanType, boolean toParent) {
    Assert.notNull(beanType, "'beanType' is required");
    this.beanName = beanType.getName();
    this.beanType = beanType;
    this.toParent = toParent;
  }

  /**
   * Create a new RuntimeBeanReference to a bean of the given type.
   *
   * @param beanName name of the target bean
   * @param beanType type of the target bean
   * @since 5.0
   */
  public RuntimeBeanReference(String beanName, Class<?> beanType) {
    this(beanName, beanType, false);
  }

  /**
   * Create a new RuntimeBeanReference to a bean of the given type,
   * with the option to mark it as reference to a bean in the parent factory.
   *
   * @param beanName name of the target bean
   * @param beanType type of the target bean
   * @param toParent whether this is an explicit reference to a bean in the
   * parent factory
   * @since 5.0
   */
  public RuntimeBeanReference(String beanName, Class<?> beanType, boolean toParent) {
    Assert.hasText(beanName, "'beanName' must not be empty");
    Assert.notNull(beanType, "'beanType' is required");
    this.beanName = beanName;
    this.beanType = beanType;
    this.toParent = toParent;
  }

  /**
   * Return the requested bean name, or the fully-qualified type name
   * in case of by-type resolution.
   *
   * @see #getBeanType()
   */
  @Override
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the requested bean type if resolution by type is demanded.
   */
  @Nullable
  public Class<?> getBeanType() {
    return this.beanType;
  }

  /**
   * Return whether this is an explicit reference to a bean in the parent factory.
   */
  public boolean isToParent() {
    return this.toParent;
  }

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Nullable
  @Override
  public Object getSource() {
    return this.source;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other || (other instanceof RuntimeBeanReference that
            && this.beanName.equals(that.beanName) && this.beanType == that.beanType
            && this.toParent == that.toParent);
  }

  @Override
  public int hashCode() {
    int result = this.beanName.hashCode();
    result = 29 * result + (this.toParent ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return '<' + getBeanName() + '>';
  }

}
