/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aop.aspectj;

import java.io.Serializable;

import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@link AspectInstanceFactory} that is backed by a
 * specified singleton object, returning the same instance for every
 * {@link #getAspectInstance()} call.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleAspectInstanceFactory
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

  private final Object aspectInstance;

  /**
   * Create a new SingletonAspectInstanceFactory for the given aspect instance.
   *
   * @param aspectInstance the singleton aspect instance
   */
  public SingletonAspectInstanceFactory(Object aspectInstance) {
    Assert.notNull(aspectInstance, "Aspect instance must not be null");
    this.aspectInstance = aspectInstance;
  }

  @Override
  public final Object getAspectInstance() {
    return this.aspectInstance;
  }

  @Override
  @Nullable
  public ClassLoader getAspectClassLoader() {
    return this.aspectInstance.getClass().getClassLoader();
  }

  /**
   * Determine the order for this factory's aspect instance,
   * either an instance-specific order expressed through implementing
   * the {@link cn.taketoday.core.Ordered} interface,
   * or a fallback order.
   *
   * @see cn.taketoday.core.Ordered
   * @see #getOrderForAspectClass
   */
  @Override
  public int getOrder() {
    if (this.aspectInstance instanceof Ordered) {
      return ((Ordered) this.aspectInstance).getOrder();
    }
    return getOrderForAspectClass(this.aspectInstance.getClass());
  }

  /**
   * Determine a fallback order for the case that the aspect instance
   * does not express an instance-specific order through implementing
   * the {@link cn.taketoday.core.Ordered} interface.
   * <p>The default implementation simply returns {@code Ordered.LOWEST_PRECEDENCE}.
   *
   * @param aspectClass the aspect class
   */
  protected int getOrderForAspectClass(Class<?> aspectClass) {
    return Ordered.LOWEST_PRECEDENCE;
  }

}
