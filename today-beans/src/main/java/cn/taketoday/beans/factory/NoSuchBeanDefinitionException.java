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

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when a {@code BeanFactory} is asked for a bean instance for which it
 * cannot find a definition. This may point to a non-existing bean, a non-unique bean,
 * or a manually registered singleton instance without an associated bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory#getBean(String)
 * @see BeanFactory#getBean(Class)
 * @see NoUniqueBeanDefinitionException
 * @since 2018-07-3 20:24:18
 */
public class NoSuchBeanDefinitionException extends BeansException {

  @Nullable
  private final String beanName;

  @Nullable
  private final ResolvableType resolvableType;

  /**
   * Create a new {@code NoSuchBeanDefinitionException}.
   *
   * @param name the name of the missing bean
   */
  public NoSuchBeanDefinitionException(@Nullable String name) {
    super("No bean named '%s' available".formatted(name));
    this.beanName = name;
    this.resolvableType = null;
  }

  public NoSuchBeanDefinitionException(String message, Throwable cause) {
    super(message, cause);
    this.beanName = null;
    this.resolvableType = null;
  }

  public NoSuchBeanDefinitionException(@Nullable String name, Class<?> targetClass) {
    super("No qualifying bean of type '%s' and named '%s' available".formatted(targetClass, name));
    this.beanName = name;
    this.resolvableType = ResolvableType.forClass(targetClass);
  }

  /**
   * Create a new {@code NoSuchBeanDefinitionException}.
   *
   * @param name the name of the missing bean
   * @param message detailed message describing the problem
   * @since 4.0
   */
  public NoSuchBeanDefinitionException(@Nullable String name, String message) {
    super("No bean named '%s' available: %s".formatted(name, message));
    this.beanName = name;
    this.resolvableType = null;
  }

  /**
   * Create a new {@code NoSuchBeanDefinitionException}.
   *
   * @param type required type of the missing bean
   * @since 4.0
   */
  public NoSuchBeanDefinitionException(Class<?> type) {
    this(ResolvableType.forClass(type));
  }

  /**
   * Create a new {@code NoSuchBeanDefinitionException}.
   *
   * @param type required type of the missing bean
   * @param message detailed message describing the problem
   * @since 4.0
   */
  public NoSuchBeanDefinitionException(Class<?> type, String message) {
    this(ResolvableType.forClass(type), message);
  }

  /**
   * Create a new {@code NoSuchBeanDefinitionException}.
   *
   * @param type full type declaration of the missing bean
   * @since 4.0
   */
  public NoSuchBeanDefinitionException(@NonNull ResolvableType type) {
    super("No qualifying bean of type '%s' available".formatted(type));
    this.beanName = null;
    this.resolvableType = type;
  }

  /**
   * Create a new {@code NoSuchBeanDefinitionException}.
   *
   * @param type full type declaration of the missing bean
   * @param message detailed message describing the problem
   * @since 4.0
   */
  public NoSuchBeanDefinitionException(@NonNull ResolvableType type, String message) {
    super("No qualifying bean of type '%s' available: %s".formatted(type, message));
    this.beanName = null;
    this.resolvableType = type;
  }

  /**
   * Return the name of the missing bean, if it was a lookup <em>by name</em> that failed.
   *
   * @since 4.0
   */
  @Nullable
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the required type of the missing bean, if it was a lookup <em>by type</em>
   * that failed.
   *
   * @since 4.0
   */
  @Nullable
  public Class<?> getBeanType() {
    return this.resolvableType != null ? this.resolvableType.resolve() : null;
  }

  /**
   * Return the required {@link ResolvableType} of the missing bean, if it was a lookup
   * <em>by type</em> that failed.
   *
   * @since 4.0
   */
  @Nullable
  public ResolvableType getResolvableType() {
    return this.resolvableType;
  }

  /**
   * Return the number of beans found when only one matching bean was expected.
   * For a regular NoSuchBeanDefinitionException, this will always be 0.
   *
   * @see NoUniqueBeanDefinitionException
   */
  public int getNumberOfBeansFound() {
    return 0;
  }

}
