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

package infra.aop.aspectj;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;

import infra.aop.framework.AopConfigException;
import infra.core.ConstructorNotFoundException;
import infra.core.Ordered;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;

/**
 * Implementation of {@link AspectInstanceFactory} that creates a new instance
 * of the specified aspect class for every {@link #getAspectInstance()} call.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleAspectInstanceFactory implements AspectInstanceFactory {

  private final Class<?> aspectClass;

  /**
   * Create a new SimpleAspectInstanceFactory for the given aspect class.
   *
   * @param aspectClass the aspect class
   */
  public SimpleAspectInstanceFactory(Class<?> aspectClass) {
    Assert.notNull(aspectClass, "Aspect class is required");
    this.aspectClass = aspectClass;
  }

  /**
   * Return the specified aspect class (never {@code null}).
   */
  public final Class<?> getAspectClass() {
    return this.aspectClass;
  }

  @Override
  public final Object getAspectInstance() {
    try {
      return ReflectionUtils.accessibleConstructor(this.aspectClass).newInstance();
    }
    catch (ConstructorNotFoundException ex) {
      throw new AopConfigException(
              "No default constructor on aspect class: " + this.aspectClass.getName(), ex);
    }
    catch (InstantiationException ex) {
      throw new AopConfigException(
              "Unable to instantiate aspect class: " + this.aspectClass.getName(), ex);
    }
    catch (IllegalAccessException | InaccessibleObjectException ex) {
      throw new AopConfigException(
              "Could not access aspect constructor: " + this.aspectClass.getName(), ex);
    }
    catch (InvocationTargetException ex) {
      throw new AopConfigException(
              "Failed to invoke aspect constructor: " + this.aspectClass.getName(), ex.getTargetException());
    }
  }

  @Override
  @Nullable
  public ClassLoader getAspectClassLoader() {
    return this.aspectClass.getClassLoader();
  }

  /**
   * Determine the order for this factory's aspect instance,
   * either an instance-specific order expressed through implementing
   * the {@link Ordered} interface,
   * or a fallback order.
   *
   * @see Ordered
   * @see #getOrderForAspectClass
   */
  @Override
  public int getOrder() {
    return getOrderForAspectClass(this.aspectClass);
  }

  /**
   * Determine a fallback order for the case that the aspect instance
   * does not express an instance-specific order through implementing
   * the {@link Ordered} interface.
   * <p>The default implementation simply returns {@code Ordered.LOWEST_PRECEDENCE}.
   *
   * @param aspectClass the aspect class
   */
  protected int getOrderForAspectClass(Class<?> aspectClass) {
    return LOWEST_PRECEDENCE;
  }

}
