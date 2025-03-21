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

package infra.aop.framework;

import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Delegate interface for a configured AOP proxy, allowing for the creation
 * of actual proxy objects.
 *
 * <p>Out-of-the-box implementations are available for JDK dynamic proxies
 * and for CGLIB proxies, as applied by {@link DefaultAopProxyFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:24
 * @see DefaultAopProxyFactory
 * @since 3.0
 */
public interface AopProxy {

  /**
   * Create a new proxy object.
   * <p>Uses the AopProxy's default class loader (if necessary for proxy creation):
   * usually, the thread context class loader.
   *
   * @return the new proxy object (never {@code null})
   * @see Thread#getContextClassLoader()
   */
  default Object getProxy() {
    return getProxy(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new proxy object.
   * <p>Uses the given class loader (if necessary for proxy creation).
   * {@code null} will simply be passed down and thus lead to the low-level
   * proxy facility's default, which is usually different from the default chosen
   * by the AopProxy implementation's {@link #getProxy()} method.
   *
   * @param classLoader the class loader to create the proxy with
   * (or {@code null} for the low-level proxy facility's default)
   * @return the new proxy object (never {@code null})
   */
  Object getProxy(@Nullable ClassLoader classLoader);

  /**
   * Determine the proxy class.
   *
   * @param classLoader the class loader to create the proxy class with
   * (or {@code null} for the low-level proxy facility's default)
   * @return the proxy class
   * @since 4.0
   */
  Class<?> getProxyClass(@Nullable ClassLoader classLoader);

}
