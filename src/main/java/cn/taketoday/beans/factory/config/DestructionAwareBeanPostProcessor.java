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
package cn.taketoday.beans.factory.config;

import cn.taketoday.beans.factory.DisposableBean;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-destruction
 * callback.
 *
 * <p>
 * The typical usage will be to invoke custom destruction callbacks on specific
 * bean types, matching corresponding initialization callbacks.
 *
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2019-12-10 00:05
 * @since 2.1.7
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

  /**
   * Apply this BeanPostProcessor to the given bean instance before its
   * destruction, e.g. invoking custom destruction callbacks.
   * <p>
   * Like DisposableBean's {@code destroy} and a custom destroy method, this
   * callback will only apply to beans which the container fully manages the
   * lifecycle for. This is usually the case for singletons and scoped beans.
   *
   * @param bean the bean instance to be destroyed
   * @param beanName the Bean name of the bean
   * @see DisposableBean#destroy()
   */
  void postProcessBeforeDestruction(Object bean, String beanName);

  /**
   * Determine whether the given bean instance requires destruction by this
   * post-processor.
   * <p>
   * The default implementation returns {@code true}. If a pre-5 implementation of
   * {@code DestructionAwareBeanPostProcessor} does not provide a concrete
   * implementation of this method, IOC silently assumes {@code true} as well.
   *
   * @param bean the bean instance to check
   * @return {@code true} if {@link #postProcessBeforeDestruction} is supposed to
   * be called for this bean instance eventually, or {@code false} if not
   * needed
   */
  default boolean requiresDestruction(Object bean) {
    return true;
  }

}
