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

package cn.taketoday.framework;

import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.Environment;

/**
 * A simple bootstrap context that is available during startup and {@link Environment}
 * post-processing up to the point that the {@link ApplicationContext} is prepared.
 * <p>
 * Provides lazy access to singletons that may be expensive to create, or need to be
 * shared before the {@link ApplicationContext} is available.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 10:43
 */
public interface BootstrapContext {

  /**
   * Return an instance from the context if the type has been registered. The instance
   * will be created it if it hasn't been accessed previously.
   *
   * @param <T> the instance type
   * @param type the instance type
   * @return the instance managed by the context
   * @throws IllegalStateException if the type has not been registered
   */
  <T> T get(Class<T> type) throws IllegalStateException;

  /**
   * Return an instance from the context if the type has been registered. The instance
   * will be created it if it hasn't been accessed previously.
   *
   * @param <T> the instance type
   * @param type the instance type
   * @param other the instance to use if the type has not been registered
   * @return the instance
   */
  <T> T getOrElse(Class<T> type, T other);

  /**
   * Return an instance from the context if the type has been registered. The instance
   * will be created it if it hasn't been accessed previously.
   *
   * @param <T> the instance type
   * @param type the instance type
   * @param other a supplier for the instance to use if the type has not been registered
   * @return the instance
   */
  <T> T getOrElseSupply(Class<T> type, Supplier<T> other);

  /**
   * Return an instance from the context if the type has been registered. The instance
   * will be created it if it hasn't been accessed previously.
   *
   * @param <T> the instance type
   * @param <X> the exception to throw if the type is not registered
   * @param type the instance type
   * @param exceptionSupplier the supplier which will return the exception to be thrown
   * @return the instance managed by the context
   * @throws X if the type has not been registered
   * @throws IllegalStateException if the type has not been registered
   */
  <T, X extends Throwable> T getOrElseThrow(Class<T> type, Supplier<? extends X> exceptionSupplier) throws X;

  /**
   * Return if a registration exists for the given type.
   *
   * @param <T> the instance type
   * @param type the instance type
   * @return {@code true} if the type has already been registered
   */
  <T> boolean isRegistered(Class<T> type);

}
